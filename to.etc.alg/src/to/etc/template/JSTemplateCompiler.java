package to.etc.template;

import java.io.*;
import java.util.*;

import javax.script.*;

/**
 * This singleton creates a compiled template for a JSP like template. The
 * language is Javascript, using JDK 6 scripting engine. The template's data
 * is copied verbatim to output until a &lt;% or &lt;%= is found; from there
 * it assumes the code is Javascript. The engine first creates a Javascript
 * program from the code entered, then it compiles it into the JSTemplate.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 26, 2010
 */
public class JSTemplateCompiler {
	/** Work buffer */
	private StringBuilder	m_sb	= new StringBuilder(1024);

	/** Javascript output buffer */
	private StringBuilder	m_jsb	= new StringBuilder(1024);

	private Reader			m_r;

	private String			m_source;

	private int				m_line, m_col;

	private int				m_pushed;

	private int				m_ocol, m_oline;

	private List<JSLocationMapping>	m_mapList;

	private enum Pha {
		/** Literal text */
		LIT,

		/** Got &lt;, */
		LT,

		/** Got &lt;% */
		PCT,

		/** Got &lt;%=, in expr there */
		XPR,

		/** In code section (&lt;%) */
		CODE,

		/** In % end delimiter */
		EPCT,
	}

	private Pha	m_pha;

	private Pha	m_opha;

	/**
	 * Create a template from input.
	 *
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public JSTemplate compile(Reader input, String sourceName) throws Exception {
		try {
			m_source = sourceName;
			translate(input);
			return compile();
		} finally {
			m_jsb.setLength(0);
			m_sb.setLength(0);
			m_mapList = null;
			m_r = null;
		}
	}

	/**
	 * Compile the Javascript program in m_jsb, then create a template.
	 * @throws Exception
	 */
	private JSTemplate compile() throws Exception {
		//-- Get a Javascript compiler.
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine jsengine = sem.getEngineByName("js");
		if(!(jsengine instanceof Compilable))
			throw new IllegalStateException("Got Javascript engine " + jsengine + " which cannot compile Javascripts!?");
		Compilable	compiler = (Compilable) jsengine;

		//-- Get the Javascript thing, then compile
		String js = m_jsb.toString();
		try {
			CompiledScript cs = compiler.compile(js);
			return new JSTemplate(m_source, jsengine, cs, m_mapList);
		} catch(ScriptException sx) {
			int[] res = remapLocation(m_mapList, sx.getLineNumber(), sx.getColumnNumber());
			throw new JSTemplateError(sx.getMessage(), m_source, res[0], res[1]);
		}
	}

	/**
	 * Walk the remap list, and try to calculate a source location for a given output location.
	 * @param mapList
	 * @param lineNumber
	 * @param columnNumber
	 * @return
	 */
	static public int[] remapLocation(List<JSLocationMapping> mapList, int lineNumber, int columnNumber) {
		//-- Walk the mapping backwards. Find 1st thing that is at/before this location.
		for(int i = mapList.size(); --i >= 0;) {
			JSLocationMapping m = mapList.get(i);
			if(m.getTline() <= lineNumber) {
				if(m.getTcol() <= columnNumber) {
					//-- Gotcha.
					int dline = lineNumber - m.getTline();
					int dcol = columnNumber - m.getTcol();
					return new int[]{m.getSline() + dline, m.getScol() + dcol};
				}
			}
		}
		//-- Nothing found: return verbatim.
		return new int[]{lineNumber, columnNumber};
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Translate to Javascript.							*/
	/*--------------------------------------------------------------*/
	/**
	 * Translate input to a Javascript program.
	 * @param input
	 */
	protected void translate(Reader input) throws Exception {
		if(!(input instanceof BufferedReader))
			input = new BufferedReader(input);
		m_r = input;
		m_jsb.setLength(0);
		m_line = 0;
		m_col = 0;
		m_pha = Pha.LIT;
		m_pushed = -1;
		m_mapList = new ArrayList<JSLocationMapping>();

		//-- Per-char state machine
		//		int scol = 0;
		//		int sline = 0;
		for(;;) {
			int c = la();
			if(c == -1) {
				if(m_pha != Pha.LIT)
					error("Unexpected end-of-file.");

				return;
			}
			switch(m_pha){
				default:
					throw new IllegalStateException("Bad pha");

				case LIT:
					if(c == '<') {
						m_pha = Pha.LT;
					} else {
						m_sb.append((char) c);
					}
					break;

				case LT:
					if(c == '%') {
						m_pha = Pha.PCT;

						//-- Ok: we need to flush the javascript string collected here.
						flushLiteral();
						//						sline = m_line;
						//						scol = m_col;
					} else {
						//-- Not <%. Just add the < and pushback this char
						m_sb.append('<');
						push(c);
					}
					break;

				case PCT:
					if(c == '=') {
						//-- EXPR section.
						m_pha = Pha.XPR;

						addMapping(m_oline, m_ocol + 1, m_line, m_col);
					} else {
						//-- Nothing special- must be javascript code.
						m_pha = Pha.CODE;
						addMapping(m_oline, m_ocol, m_line, m_col);
						m_jsb.append((char) c);
					}
					break;

				case XPR:
				case CODE:
					if(c == '%') {
						m_pha = Pha.EPCT;
						m_opha = m_pha;
					} else {
						m_jsb.append((char) c);
					}
					break;

				case EPCT:
					if(c == '>') {
						m_pha = Pha.LIT;
						flushJavascript();
					} else {
						m_jsb.append('%');
						m_pha = m_opha;
					}
					break;
			}
			if(c == '\n') {
				m_line++;
				m_col = 0;
			} else {
				m_col++;
			}
		}
	}

	private void addMapping(int oline, int ocol, int line, int col) {
		JSLocationMapping m = new JSLocationMapping(oline, ocol, line, col);
		m_mapList.add(m);
	}

	private void flushJavascript() {
	}

	/**
	 * Create a Javascript command to print the string in m_sb to out.
	 */
	private void flushLiteral() {
		m_jsb.append("out.write(");
		strToJavascriptString(m_jsb, m_sb, true);
		m_jsb.append(");\n");
		m_ocol = 0;
		m_oline++;
		m_sb.setLength(0);
	}

	static public void strToJavascriptString(final StringBuilder w, final CharSequence cs, final boolean dblquote) {
		int len = cs.length();
		//		if(len == 0)					jal 20090225 WTF!?!! Empty strings MUST be ""!!!!!
		//			return;
		int ix = 0;
		char quotechar;
		quotechar = dblquote ? '\"' : '\'';
		w.append(quotechar);

		while(ix < len) {
			//-- Collect a run
			int runstart = ix;
			char c = 0;
			while(ix < len) {
				c = cs.charAt(ix);
				if(c < 32 || c == '\'' || c == '\\' || c == quotechar)
					break;
				ix++;
			}
			if(ix > runstart) {
				w.append(cs, runstart, ix);
				if(ix >= len)
					break;
			}
			ix++;
			switch(c){
				default:
					w.append("\\u"); // Unicode escape
					intToStr(w, c & 0xffff, 16, 4);
					break;
				case '\n':
					w.append("\\n");
					break;
				case '\b':
					w.append("\\b");
					break;
				case '\f':
					w.append("\\f");
					break;
				case '\r':
					w.append("\\r");
					break;
				case '\t':
					w.append("\\t");
					break;
				case '\'':
					w.append("\\'");
					break;
				case '\"':
					w.append("\\\"");
					break;
				case '\\':
					w.append("\\\\");
					break;
			}
		}
		w.append(quotechar);
	}

	private static void intToStr(StringBuilder w, int value, int radix, int len) {
		String v = Integer.toString(value, radix);
		int sl = v.length();
		while(sl < len) {
			w.append('0');
			sl++;
		}
		w.append(v);
	}

	private void push(int c) {
		if(m_pushed != -1)
			throw new IllegalStateException("Dup push");
		m_pushed = c;
	}

	private int la() throws IOException {
		if(m_pushed != -1) {
			int c = m_pushed;
			m_pushed = -1;
			return c;
		}
		return m_r.read();
	}

	protected void error(String string) {
		throw new JSTemplateError(string, m_source, m_line, m_col);
	}
}
