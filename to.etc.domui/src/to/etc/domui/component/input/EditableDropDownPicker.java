package to.etc.domui.component.input;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.event.*;
import to.etc.domui.component.input.DropDownPicker.HAlign;
import to.etc.domui.converter.*;
import to.etc.domui.dom.html.*;
import to.etc.webapp.nls.*;

/**
 * Encapsulates AutocompleteText and drop down picker into single component. Input behaves as autocomplete field that does search on select inside select within drop down picker.
 * Search is done at client side for faster user experience. 
 * It also allows entering of new data (not contained inside predefined list), that is why it works on-top of String based input.  
 * 
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Nov 6, 2012
 */
public class EditableDropDownPicker<T> extends AutocompleteText {
	@Nullable
	private DropDownPicker<T> m_picker;

	@Nonnull
	private List<T> m_data = Collections.EMPTY_LIST;

	@Nullable
	private String m_dropDownIcon;

	@Nonnull
	private HAlign m_halign = DropDownPicker.HAlign.LEFT;

	@Nullable
	private IObjectToStringConverter<T> m_toStringConverter;

	@Nonnull
	private final Class<T> m_type;

	/**
	 * Empty constructor.
	 * Before use, make sure to setup component using:
	 * <UL>
	 * <LI> {@link EditableDropDownPicker#setData(List)} </LI>
	 * <LI> {@link EditableDropDownPicker#setDropDownIcon(String)} </LI>
	 * <LI> {@link EditableDropDownPicker#setToStringConverter(IObjectToStringConverter)} in case of 'type' is not assignable from String.class</LI>
	 * </UL> 
	 *
	 * @param type
	 */
	public EditableDropDownPicker(@Nonnull Class<T> type) {
		super();
		m_type = type;
	}

	/**
	 * Factory constructor.
	 * @param type
	 * @param data
	 * @param dropDownIcon
	 * @param toStringConverter In case of T = String, toStringConverter can be left null, otherwise it needs to be specified.
	 */
	public EditableDropDownPicker(@Nonnull Class<T> type, @Nonnull List<T> data, @Nonnull String dropDownIcon, @Nullable IObjectToStringConverter<T> toStringConverter) {
		this(type);
		m_data = data;
		m_dropDownIcon = dropDownIcon;
		m_toStringConverter = toStringConverter;
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();

		m_picker = new DropDownPicker<T>(m_data);
		if(m_dropDownIcon != null) {
			m_picker.setSrc(m_dropDownIcon);
		}

		m_picker.setMandatory(isMandatory());
		m_picker.setValue(null);
		m_picker.setHalign(m_halign);
		m_picker.setAlignmentBase(this);
		m_picker.setOnBeforeShow(new INotifyEvent<DropDownPicker<T>, ComboLookup<T>>() {
			@Override
			public void onNotify(@Nonnull DropDownPicker<T> sender, @Nullable ComboLookup<T> combo) throws Exception {
				String text = getValueSafe();
				clearMessage();
				adjustSelection(combo, text);
			}
		});

		m_picker.setOnValueChanged(new IValueChanged<DropDownPicker<T>>() {

			@Override
			public void onValueChanged(@Nonnull DropDownPicker<T> component) throws Exception {
				T value = component.getValueSafe();
				setValue(convertObjectToString(NlsContext.getCurrencyLocale(), value));
				IValueChanged<EditableDropDownPicker<T>> onValueChanged = (IValueChanged<EditableDropDownPicker<T>>) EditableDropDownPicker.this.getOnValueChanged();
				if(onValueChanged != null) {
					onValueChanged.onValueChanged(EditableDropDownPicker.this);
				}
			}
		});

		setSelect(m_picker.getSelectControl());
		appendAfterMe(m_picker);
		m_picker.build();
		initializeJS();
	}

	protected String convertObjectToString(Locale currencyLocale, T val) {
		if(m_toStringConverter != null) {
			return m_toStringConverter.convertObjectToString(NlsContext.getCurrencyLocale(), val);
		} else {
			return ConverterRegistry.getConverter(m_type, null).convertObjectToString(NlsContext.getCurrencyLocale(), val);
		}
	}

	private void adjustSelection(ComboLookup<T> combo, String text) throws Exception {
		boolean found = false;
		for(int i = 0; i < combo.getData().size(); i++) {
			T val = combo.getData().get(i);
			String optionText = convertObjectToString(NlsContext.getCurrencyLocale(), val);
			if(text != null && text.equals(optionText)) {
				combo.setValue(val);
				m_picker.setButtonValue(text);
				found = true;
				break;
			}
		}
		if(combo.getData().size() < combo.getSize()) {
			combo.setSize(found ? combo.getData().size() : combo.getData().size() + 1);
		}
		if(!found) {
			m_picker.setButtonValue(null);
		}
	}

	/**
	 * Gets picker select options.
	 * @param data
	 */
	public @Nonnull
	List<T> getData() {
		return m_data;
	}

	/**
	 * Sets data that is used for picker select options.
	 * @param data
	 */
	public void setData(@Nonnull List<T> data) {
		if(m_data != data) {
			m_data = data;
			if(null != m_picker) {
				//if picker is already created then switch it's data, otherwise data would be used when picker is creating
				m_picker.setData(data);
			}
		}
	}

	public @Nullable
	String getDropDownIcon() {
		return m_dropDownIcon;
	}

	public void setDropDownIcon(@Nullable String dropDownIcon) {
		m_dropDownIcon = dropDownIcon;
		if(m_picker != null) {
			m_picker.setSrc(dropDownIcon);
		}
	}

	public @Nonnull
	HAlign getHalign() {
		return m_halign;
	}

	public void setHalign(@Nonnull HAlign halign) {
		m_halign = halign;
		if(m_picker != null) {
			m_picker.setHalign(halign);
		}
	}

	public @Nullable
	IObjectToStringConverter<T> getToStringConverter() {
		return m_toStringConverter;
	}

	public void setToStringConverter(@Nullable IObjectToStringConverter<T> toStringConverter) {
		m_toStringConverter = toStringConverter;
	}
}
