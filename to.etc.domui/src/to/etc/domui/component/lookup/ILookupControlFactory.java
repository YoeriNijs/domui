package to.etc.domui.component.lookup;

import to.etc.domui.component.meta.*;

/**
 * Creates the stuff needed to generate a single property lookup control, plus
 * the stuff to handle the control's input and converting it to part of a
 * QCriteria restriction.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 23, 2008
 */
public interface ILookupControlFactory {
	public int accepts(SearchPropertyMetaModel pmm);

	public ILookupControlInstance createControl(SearchPropertyMetaModel spm);
}