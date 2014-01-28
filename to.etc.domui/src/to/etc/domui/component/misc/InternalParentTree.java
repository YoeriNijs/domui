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
package to.etc.domui.component.misc;

import java.io.*;
import java.net.*;

import javax.annotation.*;

import to.etc.domui.component.buttons.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.util.*;
import to.etc.util.*;

/**
 * This popup floater shows all parent nodes from a given node up, and selects one. It is part
 * of the development mode double-tilde keypress.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 28, 2010
 */
public class InternalParentTree extends Div {
	private NodeBase m_touched;

	private Div m_structure;

	public InternalParentTree(NodeBase touched) {
		m_touched = touched;
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-ipt");
		Div ttl = new Div();
		add(ttl);
		ttl.setCssClass("ui-ipt-ttl");
		ttl.add("Development: Parent Structure");
		Img img = new Img("THEME/close.png");
		img.setAlign(ImgAlign.RIGHT);
		ttl.add(img);
		img.setClicked(new IClicked<Img>() {
			@Override
			public void clicked(Img clickednode) throws Exception {
				//-- Remove this.
				InternalParentTree.this.remove();
			}
		});
		Div list = new Div();
		m_structure = list;
		add(list);
		list.setCssClass("ui-ipt-list");
		renderStructure(list);
		appendCreateJS("$('#" + getActualID() + "').draggable({" + "ghosting: false, zIndex:" + 200 + ", handle: '#" + ttl.getActualID() + "'});");
	}

	protected void renderStructure(Div list) {
		//-- Run all parents.
		TBody b = list.addTable();

		for(NodeBase nb = m_touched; nb != null;) {
			final NodeBase clicked = nb;
			TR row = b.addRow();
			row.setCssClass("ui-ipt-item");

			//-- Type icon
			TD td = b.addCell();
			td.setCellWidth("1%");
			String icon = "";
			String nn = nb.getClass().getName();
			if(nn.startsWith("to.etc.domui.dom.")) {
				icon = "iptHtml.png";
			} else if(nb instanceof UrlPage) {
				icon = "iptPage.png";
			} else {
				icon = "iptComponent.png";
			}
			td.add(new Img("THEME/" + icon));

			//-- Show component source code button.
			td = b.addCell();
			td.setCssClass("ui-ipt-btn");
			td.setCellWidth("1%");
			td.setClicked(new IClicked<NodeBase>() {
				@Override
				public void clicked(NodeBase clickednode) throws Exception {
					openSource(clicked);
				}
			});
			td.setTitle("Open the component's source code");
			td.add(new Img("THEME/iptSourceCode.png"));

			//-- If applicable: component creation location
			if(null != nb.getAllocationTracepoint()) {
				td = b.addCell();
				td.setCssClass("ui-ipt-btn");
				td.setCellWidth("1%");
				td.setClicked(new IClicked<NodeBase>() {
					@Override
					public void clicked(NodeBase clickednode) throws Exception {
						showCreationTrace(clicked, clicked.getAllocationTracepoint());
					}
				});
				td.setTitle("Open the location where the component was created");
				td.add(new Img("THEME/iptLocation.png"));
			}

			//-- The name
			td = b.addCell();
			td.setCellWidth("97%");
			td.add(nn);

			if(!nb.hasParent())
				break;
			nb = nb.getParent();
		}
	}

	/**
	 * Show a stacktrace window with the ability to open the source for that element.
	 * @param clicked
	 * @param allocationTracepoint
	 */
	protected void showCreationTrace(NodeBase clicked, StackTraceElement[] allocationTracepoint) {
		m_structure.removeAllChildren();

		Div alt = new Div();
		m_structure.add(alt);
		LinkButton lb = new LinkButton("Back to structure", "THEME/btnBack.png", new IClicked<LinkButton>() {
			@Override
			public void clicked(LinkButton clickednode) throws Exception {
				m_structure.removeAllChildren();
				renderStructure(m_structure);
			}
		});
		alt.add(lb);

		Div stk = new Div();
		m_structure.add(stk);

		/*
		 * We need to find the 1st constructor in the stack trace, because that w
		 */
		boolean first = true;
		boolean gotctor = false;
		TBody b = stk.addTable();
		for(StackTraceElement ste : allocationTracepoint) {
			String nn = ste.getClassName();
			if(nn.startsWith("org.apache.tomcat."))
				return;

			//-- Skip code when it is inside internal code.
			if(first) {
				if(ste.getMethodName().equals("<init>")) {
					gotctor = true;
				}

				if(nn.equals(DomUtil.class.getName()) || nn.equals(NodeBase.class.getName()) || nn.equals(NodeContainer.class.getName()))
					continue;
				first = false;
				if(!gotctor)
					continue;
				if(ste.getMethodName().equals("<init>"))
					continue;
			}

			first = false;
			TR row = b.addRow();
			row.setCssClass("ui-ipt-item");

			//-- Type icon
			TD td = b.addCell();
			td.setCellWidth("1%");
			String icon = "";
			if(nn.startsWith("to.etc.domui.dom.")) {
				icon = "iptHtml.png";
			} else if(nn.startsWith("to.etc.domui.")) {
				icon = "iptComponent.png";
			} else {
				icon = "iptPage.png";
			}
			td.add(new Img("THEME/" + icon));

			//-- Show component source code button.
			td = b.addCell();
			td.setCssClass("ui-ipt-btn");
			td.setCellWidth("1%");
			final StackTraceElement cste = ste;
			td.setClicked(new IClicked<NodeBase>() {
				@Override
				public void clicked(NodeBase clickednode) throws Exception {
					openSource(cste);
				}
			});
			td.setTitle("Open the source code at this location");
			td.add(new Img("THEME/iptSourceCode.png"));

			//-- Source link.
			td = b.addCell();
			td.setCellWidth("97%");
			td.add(ste.getClassName() + "#" + ste.getMethodName() + " (" + ste.getLineNumber() + ")");
		}
	}

	@Nonnull
	private String openableClassName(@Nonnull String str) {
		return str.replace('.', '/').replaceAll("\\$.*", "");
	}

	protected void openSource(NodeBase clicked) {
		NodeBase body = getPage().getBody();
		remove();

		//-- Get name for the thingy,
		String name = openableClassName(clicked.getClass().getName()) + ".java";
		openSourceWithWarning(body, name);
	}

	private void openSourceWithWarning(@Nonnull NodeBase body, @Nonnull String name) {
		if(! openEclipseSource(name)) {
			MsgBox.message(body, MsgBox.Type.WARNING, "I was not able to send an OPEN FILE command to Eclipse.. You need to have the newest version of the Eclipse plugin running. Please see " + URL + " for details");
		}
	}

	protected void openSource(StackTraceElement ste) {
		NodeBase body = getPage().getBody();
		remove();

		//-- Get name for the thingy,
		String name = openableClassName(ste.getClassName()) + ".java";
		if(ste.getLineNumber() <= 0)
			name += "@" + ste.getMethodName();
		else
			name += "#" + ste.getLineNumber();
		openSourceWithWarning(body, name);
	}

	static private final String URL = "http://www.domui.org/wiki/bin/view/Documentation/EclipsePlugin";

	private boolean openEclipseSource(@Nonnull String name) {
		File root = DomApplication.get().getAppFile("");
		return openEclipseSource(root.toString(), name);
	}

	/**
	 * Try to reach Eclipse on localhost and make it open the source for the specified class.
	 * @param name
	 * @return
	 */
	static public boolean openEclipseSource(@Nonnull String webappRoot, @Nonnull String name) {
		for(int port = 5051; port < 5060; port++) {
			if(tryPortCommand(port, webappRoot, name)) {
				return true;
			}
		}
		System.out.println("DomUI: cannot connect to Eclipse on localhost ports 5051..5060. Is the new version of the DomUI plugin running in Eclipse? See " + URL);
		return false;
	}

	/**
	 * New-style command sending: send a SELECT [webapp] COMMAND url and wait for Eclipse to answer.
	 * @param port
	 * @param webappRoot
	 * @param name
	 * @return
	 */
	static private boolean tryPortCommand(int port, @Nonnull String webappRoot, @Nonnull String name) {
		Socket s = null;
		//		boolean connected = false;
		try {
			s = new Socket("127.0.0.1", port);
		} catch(Exception x) {
			System.out.println("DomUI: connect to Eclipse on socket "+port+" failed: "+x);
			return false;
		}

		//-- Send a command
		OutputStream outputStream = null;
		InputStream is = null;
		try {
			//			connected = true;
			outputStream = s.getOutputStream();

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT `");
			sb.append(webappRoot);
			sb.append("` OPENFILE `");
			sb.append(name);
			sb.append('`');
			outputStream.write(sb.toString().getBytes("UTF-8"));
			outputStream.write(0);
			outputStream.flush();

			//-- Read the response till EOF or error.
			is = s.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int szrd;
			while(0 < (szrd = is.read(buffer))) {
				//				System.out.println("data: " + szrd);
				baos.write(buffer, 0, szrd);
			}
			//			System.out.println("data end = " + szrd);
			baos.close();

			String response = new String(baos.toByteArray(), "utf-8");
			System.out.println("DomUI Eclipse: response=" + response);

			String[] frags = response.split("\\s+");
			if(frags.length < 1)
				return false;

			String cmd = frags[0];
			if("SELECT-FAILED".equals(cmd))
				return false;
			if("OK".equals(cmd))
				return true;

			//-- TBD
			return true;
		} catch(Exception x) {
			System.out.println("DomUI: eclipse connect failed with " + x);
			x.printStackTrace();
			return false;
		} finally {
			FileTool.closeAll(outputStream, is);
			try {
				if(s != null)
					s.close();
			} catch(Exception x) {}
		}
	}


}
