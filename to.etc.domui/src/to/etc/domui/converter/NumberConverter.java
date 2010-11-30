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
package to.etc.domui.converter;

import java.util.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.trouble.*;

/**
 * Parameterizable converter for numbers.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Mar 19, 2010
 */
public class NumberConverter<T extends Number> implements IConverter<T> {
	private NumericPresentation m_presentation;

	private int m_scale;

	private Class<T> m_actualType;

	public NumberConverter(Class<T> actualType, NumericPresentation presentation, int scale) {
		m_actualType = actualType;
		m_presentation = presentation;
		m_scale = scale;
	}

	@Override
	public String convertObjectToString(Locale loc, T in) throws UIException {
		return NumericUtil.renderNumber(in, m_presentation, m_scale);
	}

	@Override
	public T convertStringToObject(Locale loc, String in) throws UIException {
		return NumericUtil.parseNumber(m_actualType, in);
	}
}
