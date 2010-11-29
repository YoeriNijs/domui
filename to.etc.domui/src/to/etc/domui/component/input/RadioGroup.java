package to.etc.domui.component.input;

import java.util.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;

/**
 * RadioGroup can be used to create a Group of RadioButtons. The RadioButtons are created by The RadioGroup and cannot be
 * added programmatically by clients. When you create a RadioGroup for an Enum, all values can be automatically transformed into RadioButtons.
 * 
 * ...
 * 
 *
 * @author <a href="mailto:willem.voogd@itris.nl">Willem Voogd</a>
 * Created on Nov 25, 2010
 */
public class RadioGroup<T> extends Div implements IControl<T> {

	private Class<T> m_clz;
	private String m_name;
	private boolean m_disabled;
	private boolean m_mandatory;
	private boolean m_readOnly;
	private T m_value;

	protected Map<RadioButton,T> m_valueMap;

	private long m_idGen;

	IValueChanged<?> m_valueChanger;
	ClassMetaModel m_cmm;

	public RadioGroup(Class<T> clz, String name, boolean expandEnum) throws InstantiationException, IllegalAccessException, Exception {
		m_clz = clz;
		m_name = name;
		m_valueMap = new HashMap<RadioButton, T>();
		m_idGen = 0;
		m_valueChanger = null;

		m_cmm = MetaManager.findClassMeta(clz);

		if (expandEnum)
			expandEnum();
	}

	public RadioGroup(Class<T> clz, String name) throws InstantiationException, IllegalAccessException, Exception {
		this(clz,name,false);
	}

	private RadioButton newDressedRadioButton() {
		RadioButton rb = new RadioButton();
		dress(rb);
		return rb;
	}

	/**
	 * When the class the radiogroup is specifying is an enum, this will create one radiobutton for each enum element.
	 * @throws Exception
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void expandEnum() throws InstantiationException, IllegalAccessException, Exception {

		if (m_clz.isEnum()) {
			for (T e: m_clz.getEnumConstants()) {
				addLabelAndRadio(e);
			}
		}

	}

	/**
	 * Sets the groupglobal properties for a radiobutton in this group
	 * 
	 * @param rb
	 */
	private void dress(RadioButton rb) {
		rb.setName(m_name);
		rb.setReadOnly(m_readOnly);
		rb.setDisabled(m_disabled);
		rb.setClicked(new IClicked<NodeBase>() {
			@Override
			public void clicked(NodeBase clickednode) throws Exception {
				setValue(m_valueMap.get(clickednode));
				checkSelectedRadio();
			}

		});
	}

	/**
	 * Adds a NodeBase to the group. RadioButtons can *not* be added, these can only be created by the group itself,
	 * via <code>addLabelAndRadio</code>.
	 * @see to.etc.domui.dom.html.NodeContainer#add(int, to.etc.domui.dom.html.NodeBase)
	 */
	@Override
	public void add(int index, NodeBase nd) {
		if (nd instanceof RadioButton) {
			throw new IllegalStateException("RadioButtons cannot be added, they will be created by the RadioGroup");
		}
		super.add(index, nd);
	}

	/**
	 * Adds a NodeBase to the group. RadioButtons can *not* be added, these can only be created by the group itself,
	 * via <code>addLabelAndRadio</code>.
	 * @see to.etc.domui.dom.html.NodeContainer#add(to.etc.domui.dom.html.NodeBase)
	 */
	@Override
	public void add(NodeBase nd) {
		if (nd instanceof RadioButton) {
			throw new IllegalStateException("RadioButtons cannot be added, they will be created by the RadioGroup");
		}
		super.add(nd);
	}

	/**
	 * Adds a radiobutton to the group for the specified object, with specified label.
	 * 
	 * @param label, the label for the radiobutton.
	 * @param object, the value the radiobutton will represent.
	 * @throws Exception
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void addLabelAndRadio(String label, T object) throws InstantiationException, IllegalAccessException, Exception {
		Div labelledradio = new Div();

		if (label == null)
			label = object.toString();

		RadioButton rb = newDressedRadioButton();
		m_valueMap.put(rb, object);
		labelledradio.add(rb);
		labelledradio.add(label);

		add(labelledradio);
	}

	/**
	 * Adds a radiobutton to the group for the specified object, for the label,
	 * toString() will be called on the object.
	 * 
	 * //TODO: Use metamodel.
	 * 
	 * @param object, the value the radiobutton will represent.
	 * @throws Exception
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void addLabelAndRadio(T object) throws InstantiationException, IllegalAccessException, Exception {
		addLabelAndRadio(null, object);
	}

	public void addAllAsRadio(Collection<T> objects) throws InstantiationException, IllegalAccessException, Exception {
		for (T object: objects) {
			addLabelAndRadio(object);
		}
	}

	/**
	 * @see to.etc.domui.dom.html.IDisplayControl#getValue()
	 */
	@Override
	public T getValue() {
		return m_value;
	}

	/**
	 * @see to.etc.domui.dom.html.IDisplayControl#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(T v)  {
		if (m_readOnly)
			return;
		if (v == null && m_value == null)
			return;
		if (v != null && v.equals(m_value))
			return;

		m_value = v;
		if(getOnValueChanged() != null) {
			try {
				((IValueChanged<NodeBase>) getOnValueChanged()).onValueChanged(this);
			} catch (Exception e) {}
		}
		checkSelectedRadio();
	}

	/**
	 * @see to.etc.domui.dom.html.IHasChangeListener#getOnValueChanged()
	 */
	@Override
	public IValueChanged< ? > getOnValueChanged() {
		// TODO Auto-generated method stub
		return m_valueChanger;
	}

	/**
	 * @see to.etc.domui.dom.html.IHasChangeListener#setOnValueChanged(to.etc.domui.dom.html.IValueChanged)
	 */
	@Override
	public void setOnValueChanged(IValueChanged< ? > onValueChanged) {
		m_valueChanger = onValueChanged;
	}

	/**
	 * @see to.etc.domui.dom.html.IActionControl#setDisabled(boolean)
	 */
	@Override
	public void setDisabled(boolean d) {
		m_disabled = d;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#getValueSafe()
	 */
	@Override
	public T getValueSafe() {
		return m_value;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return m_readOnly;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean ro) {
		m_readOnly = ro;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#isDisabled()
	 */
	@Override
	public boolean isDisabled() {
		return m_disabled;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#isMandatory()
	 */
	@Override
	public boolean isMandatory() {
		return m_mandatory;
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#setMandatory(boolean)
	 */
	@Override
	public void setMandatory(boolean ro) {
		m_mandatory = ro;
	}

	/**
	 * @see to.etc.domui.dom.html.NodeBase#createContent()
	 */
	@Override
	public void createContent() throws Exception {

		//We let the Div do all the hard work. For now, just make sure the right button is checked:
		for (RadioButton rb : m_valueMap.keySet()) {
			rb.setReadOnly(m_readOnly);
			rb.setDisabled(m_disabled);
		}
		checkSelectedRadio();
		super.createContent();
	}

	/**
	 * Makes sure the RadioButtons ar ein sync with the value. Changes the checked state of the radiobuttons if needed.
	 */
	protected void checkSelectedRadio() {
		//We let the Div do all the hard work. For now, just make sure the right button is checked:
		for (RadioButton rb : m_valueMap.keySet()) {
			if (m_valueMap.get(rb).equals(m_value)) {
				rb.setChecked(true);
			}
		}
	}

}
