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
package to.etc.webapp.nls;

import java.text.*;
import java.util.*;

/**
 * Base class for bundle related things, exposing formatters and other code around the
 * single "findMessage" method.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 31, 2010
 */
abstract public class BundleBase implements NlsMessageProvider {
	@Override
	abstract public String findMessage(Locale loc, String code);

	/**
	 * Returns a translation of key in the specified locale (or the one
	 * closest to it). If no translation exists for the message in the
	 * specified bundle then we try the "default" bundle; if it still
	 * does not exist we return a string containing the key with ????.
	 * @param loc
	 * @param key
	 * @throws  ResourceNotFoundException the bundle cannot be located.
	 */
	public String getString(final Locale loc, final String key) {
		String msg = findMessage(loc, key);
		return msg != null ? msg : "???" + key + "???";
	}

	/**
	 * Returns the translation of the key passed in the <i>current</i> client
	 * locale.
	 *
	 * @param key
	 * @return
	 */
	public String getString(final String key) {
		return getString(NlsContext.getLocale(), key);
	}

	/**
	 * Gets the string, and applies default message formatting using the parameters
	 * passed in the current locale.
	 * @param key
	 * @param param
	 * @return
	 */
	public String formatMessage(final String key, final Object... param) {
		String s = findMessage(NlsContext.getLocale(), key);
		if(s == null)
			return "???" + key + "???";
		return MessageFormat.format(s, param);
	}
}