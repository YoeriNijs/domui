/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.state;

import java.lang.reflect.*;
import java.util.*;

import javax.annotation.*;

import org.slf4j.*;

import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.state.ConversationContext.ConversationState;
import to.etc.domui.util.*;
import to.etc.util.*;

/**
 * Manages conversations and the shelve stack. A WindowSession is in charge of all conversations
 * within a given window, and maintains all state there.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Oct 24, 2008
 */
final public class WindowSession {
	static final Logger LOG = LoggerFactory.getLogger(WindowSession.class);

	final AppSession m_appSession;

	final private String m_windowID;

	final private int m_id;

	static private int m_nextId;

	/**
	 * Map of all active conversations, indexed by conversation ID.
	 */
	final private Map<String, ConversationContext> m_conversationMap = new HashMap<String, ConversationContext>();

	/**
	 * The stack of shelved pages; pages that can be returned to easily.
	 */
	private final List<IShelvedEntry> m_shelvedPageStack = new ArrayList<IShelvedEntry>();

	private int m_nextCid;

	private boolean m_attached;

	private Class< ? extends ConversationContext> m_targetConversationClass;

	private Class< ? extends UrlPage> m_targetPageClass;

	private PageParameters m_targetPageParameters;

	private ConversationContext m_targetConversation;

	private MoveMode m_targetMode;

	private String m_targetURL;

	/** Timestamp of the last time this WindowSession was used by a request. This is used to determine if a WindowSession has expired */
	private long m_lastUsed;

	/** When an obituary has been received, this contains the ID of the destruction timer for this session. It contains -1 if no destruction timer is active on this window */
	private int m_obituaryTimer = -1;

	/** The page tag of the last page that had a request for this window session. Used to decide whether an Obituary is out-of-order. */
	private int m_lastRequestedPageTag;

	/** The map of all attribute objects added to this window session. */
	private Map<String, Object> m_map = Collections.EMPTY_MAP;

	public WindowSession(final AppSession session) {
		m_appSession = session;
		m_windowID = DomUtil.generateGUID();
		m_id = nextID();
	}

	static private synchronized int nextID() {
		return ++m_nextId;
	}

	final public DomApplication getApplication() {
		return m_appSession.getApplication();
	}

	final public String getWindowID() {
		return m_windowID;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Conversation stack management.						*/
	/*--------------------------------------------------------------*/
	/**
	 * Return a new, unique ID for a conversation in this user's session.
	 * @return
	 */
	synchronized int nextCID() {
		return ++m_nextCid;
	}

	/**
	 * Locate a conversation by ID. Returns null if the conversation is not found.
	 * @param cid
	 * @return
	 */
	ConversationContext findConversation(final String cid) throws Exception {
		ConversationContext cc = m_conversationMap.get(cid);
		if(null != cc)
			internalAttachConversations();
		return cc;
	}

	/**
	 * Return all conversations that contain the specified page class.
	 * @param clz
	 * @return
	 */
	List<ConversationContext> findConversationsFor(final Class< ? extends NodeBase> clz) throws Exception {
		List<ConversationContext> res = new ArrayList<ConversationContext>();
		for(ConversationContext cc : m_conversationMap.values()) {
			if(cc.findPage(clz) != null)
				res.add(cc);
		}
		if(res.size() > 0)
			internalAttachConversations();
		return res;
	}

	/**
	 * Add a new conversation to the conversation context.
	 * @param cc
	 */
	void registerConversation(final ConversationContext cc) {
		if(cc.getId() == null)
			cc.setId("c" + nextCID());
		m_conversationMap.put(cc.getId(), cc);
		cc.setManager(this);
	}

	/**
	 * Call the "attach" method for all conversations, indicating that a new request
	 * is going to be handled.
	 * @throws Exception
	 */
	public void internalAttachConversations() throws Exception {
		if(m_attached)
			return;
		for(ConversationContext cc : m_conversationMap.values())
			cc.internalAttach();
		m_attached = true;
	}

	/**
	 * Detach all conversations, called just before the request is done.
	 */
	public void internalDetachConversations() {
		//		System.out.println("detachConversations called with state="+m_attached);
		if(!m_attached)
			return;
		m_attached = false;
		for(ConversationContext cc : m_conversationMap.values()) {
			try {
				cc.internalDetach();
			} catch(Exception x) {
				x.printStackTrace();
			}
		}
	}

	/**
	 * Dump all conversations and their resident pages.
	 */
	public void dump() {
		System.out.println("  " + this + ": Conversation list");
		for(ConversationContext cc : m_conversationMap.values()) {
			cc.dump();
		}
		System.out.println("  Page shelve");
		for(int i = 0; i < m_shelvedPageStack.size(); i++) {
			IShelvedEntry se = m_shelvedPageStack.get(i);
			System.out.println("  " + i + ": " + se);
		}

		//		System.out.println("  ---- Conversation dump end -----");
	}

	/**
	 * Closes all conversations. This discards all screen data and resources.
	 */
	void destroyConversations() {
		m_attached = false;
		for(ConversationContext cc : m_conversationMap.values()) {
			try {
				cc.internalDestroy();
			} catch(Exception x) {
				x.printStackTrace();
			}
		}
		m_conversationMap.clear();
	}

	protected void destroyConversation(final ConversationContext cc) {
		if(null == m_conversationMap.remove(cc.getId()))
			return;

		//-- Discard all pages used by this from the shelve stack
		for(int i = m_shelvedPageStack.size(); --i >= 0;) {
			IShelvedEntry she = m_shelvedPageStack.get(i);
			if(she instanceof ShelvedDomUIPage) {
				ShelvedDomUIPage sdp = (ShelvedDomUIPage) she;
				if(sdp.getPage().getConversation() == cc) {
					m_shelvedPageStack.remove(i);
				}
			}
		}

		try {
			if(cc.getState() == ConversationState.ATTACHED)
				cc.internalDetach();
		} catch(Exception x) {
			LOG.error("Exception on onDetach() of destroyed conversation", x);
		}
		try {
			cc.internalDestroy();
		} catch(Exception x) {
			LOG.error("Exception in onDestroy() of destroyed conversation", x);
		}
	}

	public ConversationContext createConversation(final IRequestContext ctx, final Class< ? extends ConversationContext> clz) throws Exception {
		if(clz == null)
			return new SimpleConversationContext();
		ConversationContext cc = clz.newInstance(); // FIXME Should do something with injection and stuff.
		m_appSession.getApplication().internalCallConversationCreated(cc);
		return cc;
	}

	public void acceptNewConversation(final ConversationContext cc) throws Exception {
		//-- Drop all "old" conversations, then add the new one
		//		destroyConversations();					// ORDERED 1
		registerConversation(cc); // ORDERED 2
		cc.internalAttach(); // ORDERED 3
		m_attached = true; // jal 20090108 "Pages were kept ATTACHED, causing exception on re-entry"
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Shelving and unshelving page handler.				*/
	/*--------------------------------------------------------------*/
	/**
	 * Shelve the current page, then move to the new one.
	 * @param shelved
	 */
	private void shelvePage(@Nonnull final Page shelved) {
		if(shelved == null)
			throw new IllegalStateException("Missing current page??");
		m_shelvedPageStack.add(new ShelvedDomUIPage(this, shelved));
	}

	public List<IShelvedEntry> getShelvedPageStack() {
		return m_shelvedPageStack;
	}

	/**
	 * Goto handling in EXCEPTION handling mode: only Redirect is allowed here.
	 * @param ctx
	 * @param currentpg
	 * @param ajax
	 * @return
	 */
	public boolean handleExceptionGoto(@Nonnull final RequestContextImpl ctx, @Nonnull final Page currentpg, boolean ajax) throws Exception {
		if(getTargetMode() == null)
			return false;

		switch(getTargetMode()){
			default:
				throw new IllegalStateException("UIGoto." + getTargetMode() + " is invalid when calling UIGoto from an exception listener");

			case REDIRECT:
			case NEW:
			case SUB:
				break;
		}
		return handleGoto(ctx, currentpg, ajax);
	}

	/**
	 * This checks whether a new page is to be made resident, instead of the
	 * current page.
	 *
	 * @param ctx
	 * @param currentpg		The page that is <b>current</b> (the one that issued the MOVE command).
	 * @return
	 * @throws Exception
	 */
	public boolean handleGoto(@Nonnull final RequestContextImpl ctx, @Nonnull final Page currentpg, boolean ajax) throws Exception {
		//		System.out.println("GOTO: currentpg=" + currentpg + ", shelved=" + currentpg.isShelved());
		if(getTargetMode() == null)
			return false;
		if(getTargetMode() == MoveMode.BACK) {
			// Back requested-> move back, then.
			handleMoveBack(ctx, currentpg, ajax);
			return true;
		}
		if(getTargetMode() == MoveMode.REDIRECT) {
			String tu = m_targetURL;
			if(tu.startsWith("/"))
				tu = tu.substring(1);
			if(tu.indexOf(':') == -1) {
				tu = ctx.getRelativePath(tu); // Make absolute.
			}
			generateRedirect(ctx, tu, ajax);
			return true;
		}

		//-- We move somewhere else. Really?
		Class< ? extends UrlPage> clz = getTargetPageClass(); // New class set?
		if(clz == null)
			return false;
		// jal 30 augustus 2008 switched off to allow for full re-render of the current page (theme change).
		//		if(clz == pg.getClass())					// Already at that page?
		//			return false;							// Piss off then

		ConversationContext cc = getTargetConversation();
		PageParameters pp = getTargetPageParameters();
		Constructor< ? extends UrlPage> bestpc = null;

		if(getTargetMode() != MoveMode.REPLACE) {
			/*
			 * Look back in the page shelve and check if a compatible page is present there. If so
			 * we move back by destroying the pages "above" the target.
			 */
			//-- Locate the specified page/conversation in the page stack,
			int psix = findInPageStack(cc, clz, pp);
			if(psix != -1) {
				/*
				 * Page found. Is it the current page? If so we just ignore the request.
				 */
				if(psix == m_shelvedPageStack.size() - 1) {
					return false;
				}

				/*
				 * Entry accepted. Discard all stacked entries *above* the selected thing.
				 */
				clearShelve(psix + 1);
				internalAttachConversations();
				IShelvedEntry xse = m_shelvedPageStack.get(psix);
				if(!(xse instanceof ShelvedDomUIPage))
					throw new IllegalStateException("Shelve entry is not a domui page but " + xse);

				Page currentPage = ((ShelvedDomUIPage) xse).getPage();

				/*
				 * jal 20100224 The old page is destroyed and we're now running in the "new" page's context! Since
				 * unshelve calls user code - which can access that context using PageContext.getXXX calls- we must
				 * make sure it is correct even though the request was for another page and is almost dying.
				 */
				UIContext.internalSet(currentPage);
				currentPage.internalUnshelve();
				generateRedirect(ctx, currentPage, ajax);
				return true;
			}
		}

		//-- Handle the shelve mode,
		if(getTargetMode() == MoveMode.NEW || mustResetShelve(clz))
			clearShelve(0);
		else if(getTargetMode() == MoveMode.REPLACE) {
			/*
			 * The "current" page on top of the shelve stack is destroyed; the new page replaces it on top
			 * of the stack.
			 */
			int psix = m_shelvedPageStack.size() - 1; // We need to DESTROY the last page stack element,
			if(psix < 0) // If there is no topmost page
				psix = 0; // Just clear.
			clearShelve(psix);
		} else if(getTargetMode() == MoveMode.SUB) {
			//-- We're shelving the current page- call all shelve handlers.
			currentpg.internalShelve();
		} else
			throw new IllegalStateException("Internal: don't know how to handle shelve mode " + getTargetMode());

		/*
		 * Shite. We need to really move. We need to do context handling here. We
		 * have the following possibilities:
		 * <ul>
		 * 	<li>the old page goes out of scope (it's conversation gets cleared, which also drops the page),</li>
		 *	<li>the new page is connecting to the same conversation as the old page</li>
		 *	<li>the new page has a new conversation; the old conversation is put away (nesting)</li>
		 * </ul>
		 */

		/*
		 * If a new conversation is specified make sure the page class can accept it,
		 * Determine the conversation and the constructor to use for the page.
		 */
		if(cc == null) {
			//-- We need a new conversation of the given type...
			Class< ? extends ConversationContext> coclz = getTargetConversationClass();
			if(coclz == null) {
				bestpc = PageMaker.getBestPageConstructor(clz, pp != null); // Get best constructor;
				coclz = PageMaker.getConversationType(bestpc);
			} else {
				//-- Find a constructor for the specified conversation
				bestpc = PageMaker.getPageConstructor(clz, coclz, pp != null);
			}

			//-- Create the conversation to use,
			cc = createConversation(ctx, coclz);
			acceptNewConversation(cc);
		} else {
			//-- We JOIN..... Use the conversation specified. The new page class must accept it, of course,
			bestpc = PageMaker.getPageConstructor(clz, cc.getClass(), pp != null);
			//-- Join is acceptable; conversation is ok, and present in 'cc'
		}

		//-- Conversation has been validated now, and it is active. Create and link the new page now.
		if(pp == null)
			pp = new PageParameters();
		Page currentPage = PageMaker.createPageWithContent(ctx, bestpc, cc, pp);
		UIContext.internalSet(currentPage); // jal 20100224 Code can run in new page on shelve.
		shelvePage(currentPage);

		//-- Call all of the page's listeners.
		//		callNewPageListeners(m_currentPage); // jal 20091122 Bug# 605 Move this globally.
		generateRedirect(ctx, currentPage, ajax);
		return true;
	}

	/**
	 * Returns TRUE if the target page is a page which can only be on top of the shelve. For now
	 * it checks if the page == the index page.
	 * @param clz
	 * @return
	 */
	private boolean mustResetShelve(final Class< ? extends UrlPage> clz) {
		Class<?> ac = m_appSession.getApplication().getRootPage();
		if(ac == null)
			return false;

		if(clz.getName().equals(m_appSession.getApplication().getRootPage().getName()))
			return true;
		return false;
	}

	void generateRedirect(final RequestContextImpl ctx, final Page to, boolean ajax) throws Exception {
		//-- Send a "redirect" to the new page;
		StringBuilder sb = new StringBuilder();
		sb.append(ctx.getRelativePath(to.getBody().getClass().getName()));
		sb.append('.');
		sb.append(ctx.getApplication().getUrlExtension());
		sb.append('?');
		StringTool.encodeURLEncoded(sb, Constants.PARAM_CONVERSATION_ID);
		sb.append('=');
		sb.append(to.getConversation().getFullId());

		//-- Add any parameters
		PageParameters pp = to.getPageParameters();
		if(pp != null) {
			DomUtil.addUrlParameters(sb, pp, false);
		}
		generateRedirect(ctx, sb.toString(), ajax);
	}

	private void generateRedirect(final RequestContextImpl ctx, final String url, boolean ajax) throws Exception {
		if(ajax)
			ApplicationRequestHandler.generateAjaxRedirect(ctx, url);
		else
			ApplicationRequestHandler.generateHttpRedirect(ctx, url, "Redirecting");
	}

	/**
	 * Moves one shelve entry back. If there's no shelve entry current moves back to the application's index.
	 * @param currentpg
	 */
	private void handleMoveBack(@Nonnull final RequestContextImpl ctx, @Nonnull Page currentpg, boolean ajax) throws Exception {
		int ix = m_shelvedPageStack.size() - 2;
		if(ix < 0) {
			clearShelve(0); // Discard EVERYTHING

			//-- If we have a root page go there, else
			Class< ? extends UrlPage> clz = getApplication().getRootPage();
			if(clz != null) {
				internalSetNextPage(MoveMode.NEW, getApplication().getRootPage(), null, null, null);
				handleGoto(ctx, currentpg, ajax);
			} else {
				//-- Last resort: move to root of the webapp by redirecting to some URL
				generateRedirect(ctx, ctx.getRelativePath(""), ajax);
			}
			return;
		}

		//-- Unshelve and destroy the topmost thingy, then move back to the then-topmost.
		clearShelve(ix + 1); // Destroy everything above;
		IShelvedEntry se = m_shelvedPageStack.get(ix);	// Get the thing to move to,
		se.activate(ctx, ajax);									// Activate this page.
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Page to page navigation stuff.						*/
	/*--------------------------------------------------------------*/
	/**
	 * Clear all goto stuff at request start time.
	 */
	public void clearGoto() {
		m_targetConversationClass = null;
		m_targetPageClass = null;
		m_targetPageParameters = null;
		m_targetConversation = null;
		m_targetMode = null;
	}

	public void internalSetNextPage(final MoveMode m, final Class< ? extends UrlPage> clz, final ConversationContext cc, final Class< ? extends ConversationContext> ccclz, final PageParameters pp) {
		m_targetMode = m;
		m_targetPageClass = clz;
		m_targetConversationClass = ccclz;
		m_targetPageParameters = pp;
		m_targetConversation = cc;
	}

	public void internalSetRedirect(final String targeturl) {
		m_targetMode = MoveMode.REDIRECT;
		m_targetURL = targeturl;
	}

	public Class< ? extends UrlPage> getTargetPageClass() {
		return m_targetPageClass;
	}

	public PageParameters getTargetPageParameters() {
		return m_targetPageParameters;
	}

	public Class< ? extends ConversationContext> getTargetConversationClass() {
		return m_targetConversationClass;
	}

	public ConversationContext getTargetConversation() {
		return m_targetConversation;
	}

	public MoveMode getTargetMode() {
		return m_targetMode;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Page location and creation.							*/
	/*--------------------------------------------------------------*/

	/**
	 * Discards all shelved stuff up to the specified level (inclusive). Calling this
	 * with ix==0 means the entire page shelve and contexts are discarded.
	 */
	private void clearShelve(final int ix) {
		//		System.out.println("CLEARING SHELVE to "+ix);
		if(ix == 0) {
			m_shelvedPageStack.clear(); // Quickly destroy everything.
			destroyConversations();
			return;
		} else if(ix < 0)
			throw new IllegalStateException("?? index is invalid: " + ix);

		/*
		 * Discard top-level entries until we reach the specified level.
		 */
		while(m_shelvedPageStack.size() > ix) {
			IShelvedEntry se = m_shelvedPageStack.remove(m_shelvedPageStack.size() - 1);
			se.discard();
			//			System.out.println("Trying to discard " + se.getPage() + " in conversation " + se.getPage().getConversation());
		}
	}

	/**
	 * Discards this page from the page shelf. It discards it's conversation if that is no
	 * longer present on the shelf.
	 * @param pg
	 */
	void discardPage(final Page pg) {
		boolean destroyc = true;
		for(int i = m_shelvedPageStack.size(); --i >= 0;) {
			IShelvedEntry se = m_shelvedPageStack.get(i);
			if(se instanceof ShelvedDomUIPage) {
				ShelvedDomUIPage sdp = (ShelvedDomUIPage) se;
				if(sdp.getPage().internalGetConversation() == pg.internalGetConversation()) {
					destroyc = false;
					break;
				}
			}
		}

		if(destroyc) {
			destroyConversation(pg.internalGetConversation()); // Forcefully destroy this conversation,
		} else {
			//-- Remove this page from the conversation.
			pg.getConversation().destroyPage(pg);
		}
	}

	/**
	 * Get a valid Page, either from the shelve stack or some other location.
	 * @param rctx
	 * @param clz
	 * @param papa
	 * @return
	 * @throws Exception
	 */
	public Page makeOrGetPage(final IRequestContext rctx, final Class< ? extends UrlPage> clz, final PageParameters papa) throws Exception {
		//-- 1. If a conversation ID is present try to get the page from there,
		ConversationContext cc = null;
		String cid = rctx.getParameter(Constants.PARAM_CONVERSATION_ID);
		if(cid != null) {
			String[] cida = DomUtil.decodeCID(cid);
			cid = cida[1];
			cc = findConversation(cid);
		}

		//-- Locate the specified page/conversation in the page stack,
		if(cc != null) {
			int psix = findInPageStack(cc, clz, papa);
			if(psix != -1) {
				//-- Entry accepted. Discard all stacked entries *above* the selected thing.
				clearShelve(psix + 1);
				internalAttachConversations();

				//-- We know this is a DomUI page, no?
				ShelvedDomUIPage sdp = (ShelvedDomUIPage) m_shelvedPageStack.get(psix);
				Page pg = sdp.getPage();
				if(pg.isShelved())
					pg.internalUnshelve();
				return pg;
			}
		}

		/*
		 * None of the shelved thingies accept the current page -> discard shelve, then create a new history.
		 */
		clearShelve(0); // ORDERED 1; Discard all shelved entries

		/*
		 * We need to create a new page, always. Find the best constructor and it's conversation.
		 */
		Constructor< ? extends UrlPage> bestpc = PageMaker.getBestPageConstructor(clz, true);
		Class< ? extends ConversationContext> ccclz = PageMaker.getConversationType(bestpc); // Get the conversation class to use,

		//-- Create the conversation context
		ConversationContext coco = createConversation(rctx, ccclz);

		/*
		 * jal 20120522 We use the cid from the URL, because that is the full CID that the browser knows about. If a new CID was
		 * needed, then the URL generated by the server will have the new CID.
		 */
		coco.setId(cid == null ? "" + nextCID() : cid);

		//-- Since this is a new page we clear ALL existing conversations
		registerConversation(coco); // ORDERED 2
		ConversationContext.LOG.debug("Created conversation=" + coco + " for new page=" + clz);
		internalAttachConversations(); // ORDERED 3

		//-- Create the page && add to shelve,
		Page newpg = PageMaker.createPageWithContent(rctx, bestpc, coco, PageParameters.createFrom(rctx));
		shelvePage(newpg); // Append the current page to the shelve,

		//-- Call all of the page's listeners.
		//		callNewPageListeners(m_currentPage); jal 20091122 Bug# 605 Move this globally.
		return newpg;
	}

	// jal 20091122 Bug# 605 Move this globally.
	//	private void callNewPageListeners(final Page pg) throws Exception {
	//		PageContext.internalSet(pg); // Jal 20081103 Set state before calling add listeners.
	//		for(INewPageInstantiated npi : getApplication().getNewPageInstantiatedListeners())
	//			npi.newPageInstantiated(m_currentPage.getBody());
	//	}

	/**
	 * Check to see if we can use a page stack entry.
	 *
	 * @param cc
	 * @param clz
	 * @param papa
	 * @return
	 */
	private int findInPageStack(final ConversationContext cc, final Class< ? extends UrlPage> clz, final PageParameters papa) throws Exception {
		//		if(cc == null) FIXME jal 20090824 Revisit: this is questionable; why can it be null? Has code path from UIGoto-> handleGoto.
		//			throw new IllegalStateException("The conversation cannot be empty here.");
		for(int ix = m_shelvedPageStack.size(); --ix >= 0;) {
			IShelvedEntry se = m_shelvedPageStack.get(ix);
			if(se instanceof ShelvedDomUIPage) {
				ShelvedDomUIPage sdp = (ShelvedDomUIPage) se;

				if(sdp.getPage().getBody().getClass() != clz) // Of the appropriate type?
					continue; // No -> not acceptable
				if(cc != null && cc != sdp.getPage().getConversation()) // Is in the conversation supplied?
					continue; // No -> not acceptable

				//-- Page AND context are acceptable; check parameters;
				if(PageMaker.pageAcceptsParameters(sdp.getPage(), papa)) // Got a page; must make sure the parameters, if present, are equal.
					return ix;
			}
		}
		return -1; // Nothing acceptable
	}

	@Override
	public String toString() {
		return "Window[" + m_id + ":" + m_windowID + "]";
	}

	public long getLastUsed() {
		return m_lastUsed;
	}

	void internalTouched() {
		m_lastUsed = System.currentTimeMillis();
	}

	int getObituaryTimer() {
		return m_obituaryTimer;
	}

	void setObituaryTimer(final int obituaryTimer) {
		m_obituaryTimer = obituaryTimer;
	}

	public void internalSetLastPage(final Page page) {
		synchronized(m_appSession) {
			m_lastRequestedPageTag = page.getPageTag();
		}
	}

	int internalGetLastPageTag() {
		synchronized(m_appSession) {
			return m_lastRequestedPageTag;
		}
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Contained objects map (EXPERIMENTAL)				*/
	/*--------------------------------------------------------------*/
	/**
	 * EXPERIMENTAL DO NOT USE.
	 * @param name
	 * @param val
	 */
	public void setAttribute(final String name, final Object val) {
		if(m_map == Collections.EMPTY_MAP)
			m_map = new HashMap<String, Object>();
		if(val == null)
			m_map.remove(name);
		else {
			m_map.put(name, val);
		}
	}

	/**
	 * EXPERIMENTAL DO NOT USE.
	 * @param name
	 * @return
	 */
	public Object getAttribute(final String name) {
		return m_map.get(name);
	}

	/**
	 * Add or insert a page to the shelve stack. Used to shelve non DomUI stack entries.
	 * @param depth
	 * @param entry
	 */
	public void addShelveEntry(int depth, @Nonnull IShelvedEntry entry) {
		if(depth > 0)
			throw new IllegalArgumentException("Depth must be <= 0");
		int ix = m_shelvedPageStack.size() + depth;			// Depth moves index backwards because it is -ve
		if(ix < 0)
			throw new IllegalArgumentException("Depth of " + depth + " invalid: max is " + -m_shelvedPageStack.size());
		m_shelvedPageStack.add(ix, entry);
	}
}
