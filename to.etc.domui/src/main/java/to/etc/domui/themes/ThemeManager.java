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
package to.etc.domui.themes;

import java.io.*;
import java.util.*;

import javax.annotation.*;

import to.etc.domui.server.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.js.*;
import to.etc.domui.util.resources.*;
import to.etc.util.*;

/**
 * This is used by DomApplication to manage themes. It exists to reduce the code in DomApplication; it
 * cannot be overridden.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 27, 2011
 */
final public class ThemeManager {
	final private DomApplication m_application;

	/** The thing that themes the application. Set only once @ init time. */
	private IThemeFactory m_defaultThemeFactory;

	/** The "current theme". This will become part of all themed resource URLs and is interpreted by the theme factory to resolve resources. */
	private String m_defaultTheme = "domui";

	static private class ThemeRef {
		final private ITheme m_theme;

		private long m_lastuse;

		final private IIsModified m_rdl;

		public ThemeRef(ITheme theme, IIsModified rdl) {
			m_theme = theme;
			m_rdl = rdl;
		}

		public ITheme getTheme() {
			return m_theme;
		}

		public long getLastuse() {
			return m_lastuse;
		}

		public void setLastuse(long lastuse) {
			m_lastuse = lastuse;
		}

		public IIsModified getDependencies() {
			return m_rdl;
		}
	}

	/** Map of themes by theme name, as implemented by the current engine. */
	private final Map<String, ThemeRef> m_themeMap = new HashMap<String, ThemeRef>();

	public ThemeManager(DomApplication application) {
		m_application = application;
	}

	/**
	 * Sets the current theme string. This string is used as a "parameter" for the theme factory
	 * which will use it to decide on the "real" theme to use.
	 * @param defaultTheme	The theme name, valid for the current theme engine. Cannot be null nor the empty string.
	 */
	public synchronized void setDefaultTheme(@Nonnull String defaultTheme) {
		if(null == defaultTheme)
			throw new IllegalArgumentException("This cannot be null");
		m_defaultTheme = defaultTheme;
	}

	/**
	 * Gets the application-default theme string.  This will become part of all themed resource URLs
	 * and is interpreted by the theme factory to resolve resources.
	 * @return
	 */
	@Nonnull
	public synchronized String getDefaultTheme() {
		return m_defaultTheme;
	}

	/**
	 * Get the current theme factory.
	 * @return
	 */
	@Nonnull
	private synchronized IThemeFactory getDefaultThemeFactory() {
		if(m_defaultThemeFactory == null)
			throw new IllegalStateException("Theme factory cannot be null");
		return m_defaultThemeFactory;
	}

	/**
	 * Set the factory for handling the theme.
	 * @param themer
	 */
	public synchronized void setDefaultThemeFactory(@Nonnull IThemeFactory themer) {
		if(themer == null)
			throw new IllegalStateException("Theme factory cannot be null");
		m_defaultThemeFactory = themer;
		m_themeMap.clear();
	}

	/**
	 * Get an ITheme instance for the default theme manager and theme.
	 */
	@Nonnull
	public ITheme getDefaultThemeInstance() {
		return getTheme(getDefaultTheme(), null);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Getting a theme instance.							*/
	/*--------------------------------------------------------------*/
	static private final long OLD_THEME_TIME = 5 * 60 * 1000;

	private int m_themeReapCount;

	private long m_themeNextReapTS;

	/**
	 * Get the theme store representing the specified theme name. This is the name as obtained
	 * from the resource name which is the part between $THEME/ and the actual filename. This
	 * code is fast once the theme is loaded after the 1st call.
	 *
	 * @param rdl
	 * @return
	 * @throws Exception
	 */
	@Nonnull
	public ITheme getTheme(String themeName, @Nullable IResourceDependencyList rdl) {
		synchronized(this) {
			if(m_themeReapCount++ > 1000) {
				m_themeReapCount = 0;
				checkReapThemes();
			}

			ThemeRef tr = m_themeMap.get(themeName);
			if(tr != null) {
				//-- Developer mode: is the theme still valid?
				if(tr.getDependencies() == null || !tr.getDependencies().isModified()) {
					if(rdl != null && tr.getDependencies() != null)
						rdl.add(tr.getDependencies());
					tr.setLastuse(System.currentTimeMillis());
					return tr.getTheme();
				}
			}

			//-- No such cached theme yet, or the theme has changed. (Re)load it.
			ITheme theme;
			try {
				theme = getDefaultThemeFactory().getTheme(m_application, themeName);
			} catch(Exception x) {
				throw WrappedException.wrap(x);
			}
			if(null == theme)
				throw new IllegalStateException("Theme factory returned null!?");
			ResourceDependencies deps = null;
			if(m_application.inDevelopmentMode()) {
				ThemeModifiableResource tmr = new ThemeModifiableResource(theme.getDependencies(), 3000);
				deps = new ResourceDependencies(new IIsModified[]{tmr});
			}
			tr = new ThemeRef(theme, deps);
			if(rdl != null && deps != null)
				rdl.add(deps);
			m_themeMap.put(themeName, tr);
			return theme;
		}
	}

	/**
	 * Check to see if there are "old" themes (not used for > 5 minutes)
	 * that we can reap. We will always retain the most recently used theme.
	 */
	private synchronized void checkReapThemes() {
		long ts = System.currentTimeMillis();
		if(ts < m_themeNextReapTS)
			return;

		//-- Get a list of all themes and sort in ascending time order.
		List<ThemeRef> list = new ArrayList<ThemeRef>(m_themeMap.values());
		Collections.sort(list, new Comparator<ThemeRef>() {
			@Override
			public int compare(ThemeRef a, ThemeRef b) {
				long d = a.getLastuse() - b.getLastuse();
				return d == 0 ? 0 : d > 0 ? 1 : -1;
			}
		});

		long abstime = ts - OLD_THEME_TIME;
		for(int i = list.size()-1; --i >= 0;) {
			ThemeRef tr = list.get(i);
			if(tr.getLastuse() < abstime)
				list.remove(i);
		}
		m_themeNextReapTS = ts + OLD_THEME_TIME;
	}


	public String getThemeReplacedString(@Nonnull IResourceDependencyList rdl, String rurl) throws Exception {
		return getThemeReplacedString(rdl, rurl, null);
	}

	/**
	 * EXPENSIVE CALL - ONLY USE TO CREATE CACHED RESOURCES
	 *
	 * This loads a theme resource as an utf-8 encoded template, then does expansion using the
	 * current theme's variable map. This map is either a "style.properties" file
	 * inside the theme's folder, or can be configured dynamically using a IThemeMapFactory.
	 *
	 * The result is returned as a string.
	 *
	 * @param rdl
	 * @return
	 */
	public String getThemeReplacedString(@Nonnull IResourceDependencyList rdl, @Nonnull String rurl, @Nullable BrowserVersion bv) throws Exception {
		long ts = System.nanoTime();
		IResourceRef ires = m_application.getResource(rurl, rdl); // Get the template source file
		if(!ires.exists()) {
			System.out.println(">>>> RESOURCE ERROR: " + rurl + ", ref=" + ires);
			throw new ThingyNotFoundException("Unexpected: cannot get input stream for IResourceRef rurl=" + rurl + ", ref=" + ires);
		}

		String[] spl = ThemeResourceFactory.splitThemeURL(rurl);
		ITheme theme = getTheme(spl[0], null); // Dependencies already added by get-resource call.
		IScriptScope ss = theme.getPropertyScope();
		ss = ss.newScope();

		if(bv != null) {
			ss.put("browser", bv);
		}
		m_application.augmentThemeMap(ss); // Provide a hook to let user code add stuff to the theme map

		//-- 2. Get a reader.
		InputStream is = ires.getInputStream();
		if(is == null) {
			System.out.println(">>>> RESOURCE ERROR: " + rurl + ", ref=" + ires);
			throw new ThingyNotFoundException("Unexpected: cannot get input stream for IResourceRef rurl=" + rurl + ", ref=" + ires);
		}
		try {
			Reader r = new InputStreamReader(is, "utf-8");
			StringBuilder sb = new StringBuilder(65536);

			RhinoTemplateCompiler rtc = new RhinoTemplateCompiler();
			rtc.execute(sb, r, rurl, ss);
			ts = System.nanoTime() - ts;
			if(bv != null)
				System.out.println("theme-replace: " + rurl + " for " + bv.getBrowserName() + ":" + bv.getMajorVersion() + " took " + StringTool.strNanoTime(ts));
			else
				System.out.println("theme-replace: " + rurl + " for all browsers took " + StringTool.strNanoTime(ts));
			return sb.toString();
		} finally {
			try {
				is.close();
			} catch(Exception x) {}
		}
	}

	/**
	 * Return the current theme map (a readonly map), cached from the last
	 * time. It will refresh automatically when the resource dependencies
	 * for the theme are updated.
	 *
	 * @param rdl
	 * @return
	 * @throws Exception
	 */
	public IScriptScope getThemeMap(String themeName, IResourceDependencyList rdlin) throws Exception {
		ITheme ts = getTheme(themeName, rdlin);
		return ts.getPropertyScope();
	}

	/**
	 * This checks to see if the RURL passed is a theme-relative URL. These URLs start
	 * with THEME/. If not the RURL is returned as-is; otherwise the URL is translated
	 * to a path containing the current theme string:
	 * <pre>
	 * 	$THEME/[currentThemeString]/[name]
	 * </pre>
	 * where [name] is the rest of the path string after THEME/ has been removed from it.
	 * @param themeStyle			The substyle/variant of the theme that the page wants to use.
	 * @param path
	 * @return
	 */
	@Nonnull
	public String getThemedResourceRURL(@Nonnull IThemeVariant themeStyle, @Nonnull String path) {
		if(path.startsWith("THEME/")) {
			path = path.substring(6); 							// Strip THEME/
		} else if(path.startsWith("ICON/")) {
			throw new IllegalStateException("Bad ROOT: ICON/. Use THEME/ instead.");
		} else
			return path;										// Not theme-relative, so return as-is.
		if(path == null)
			throw new NullPointerException();

		//-- This *is* a theme URL. Do we need to replace the icon?
		ITheme theme = getTheme(getDefaultTheme()+"/"+themeStyle.getVariantName(), null);
		String newicon = theme.translateResourceName(path);
		return ThemeResourceFactory.PREFIX + getDefaultTheme() + "/" + themeStyle.getVariantName() + "/" + newicon;
	}
}
