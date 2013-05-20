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
package to.etc.domui.databinding;

import javax.annotation.*;

import to.etc.domui.component.meta.*;

/**
 * An observed property on some instance.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 23, 2013
 */
public class PropertyObservableValue<C, T> extends ListenerList<T, ValueChangeEvent<T>, IValueChangeListener<T>> implements IObservableValue<T> {
	@Nonnull
	final private C m_instance;

	@Nonnull
	final private PropertyMetaModel<T> m_property;

	public PropertyObservableValue(@Nonnull C instance, @Nonnull PropertyMetaModel<T> property) {
		m_instance = instance;
		m_property = property;
	}

	@Override
	@Nonnull
	public Class<T> getValueType() {
		return m_property.getActualType();
	}

	@Override
	@Nullable
	public T getValue() throws Exception {
		return m_property.getValue(m_instance);
	}


	@Override
	public void setValue(@Nullable T value) throws Exception {
		/*
		 * 20130425 jal
		 * The old implementation did a getvalue before the setvalue, and fired an event when the
		 * old and new values changed. This should apparently not be done: the property we're observing
		 * itself will fire an event when modified.
		 */
		m_property.setValue(m_instance, value);

		//		T old = null;
		//		try {
		//			old = m_property.getValue(m_instance);
		//		} catch(Exception x) {}
		//		m_property.setValue(m_instance, value);
		//		notifyIfChanged(old, value);
	}

	void notifyIfChanged(@Nullable T old, @Nullable T value) {
		if(MetaManager.areObjectsEqual(old, value))
			return;
		ValueDiff<T> vd = new ValueDiff<T>(old, value);
		fireEvent(new ValueChangeEvent<T>(this, vd));
	}
}
