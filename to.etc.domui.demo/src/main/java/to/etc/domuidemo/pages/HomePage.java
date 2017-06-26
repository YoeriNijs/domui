package to.etc.domuidemo.pages;

import to.etc.domui.component.misc.*;
import to.etc.domui.dom.html.*;
import to.etc.domuidemo.pages.basic.*;
import to.etc.domuidemo.pages.binding.tbl.*;
import to.etc.domuidemo.pages.graphs.*;
import to.etc.domuidemo.pages.overview.agenda.*;
import to.etc.domuidemo.pages.overview.buttons.*;
import to.etc.domuidemo.pages.overview.delayed.*;
import to.etc.domuidemo.pages.overview.dnd.*;
import to.etc.domuidemo.pages.overview.graph.*;
import to.etc.domuidemo.pages.overview.htmleditor.*;
import to.etc.domuidemo.pages.overview.input.*;
import to.etc.domuidemo.pages.overview.layout.*;
import to.etc.domuidemo.pages.overview.lookup.*;
import to.etc.domuidemo.pages.overview.menu.*;
import to.etc.domuidemo.pages.overview.misc.*;
import to.etc.domuidemo.pages.overview.tree.*;
import to.etc.formbuilder.pages.*;

public class HomePage extends MenuPage {
	public HomePage() {
		super("Component Overview - DomUI");
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-content");
		String text = "Welcome to the DomUI demo application! This application has simple examples of many of the components. It also has some code "
			+ "from the tutorial. Use it to get "
			+ "an idea on what is possible with DomUI, and how easy it is! Click the links to go to a page, and when done use the \"breadcrumbs\" in the "
			+ "bar on top of the screen to return back to where you came from."
 + "<br><br>Please keep in mind: the examples here have been made as <b>simple as possible</b>. " //
			+ "Which means that the code is quite verbose sometimes. That is not how it usually is, of course, " //
			+ "it is done that way to make 'how it works' as clear as possible."
			;
		NodeContainer ip = new Explanation(text);
		add(ip);

		Div d = new Div();
		ip.add(d);
		//		d.setCssClass("d-expl");
		d.setText("At any time, you can press the Java icon ");
		d.add(new Img("img/java.png"));
		d.add(" to get a window showing the Java source code for the screen in question. In this window you can click the underlined class names to go to their sources too.");

		addCaption("Layout components");
		addLink(DemoCaptionedHeader.class, "The CaptionedHeader");
		addLink(DemoCaption.class, "The Caption component");
		addLink(DemoCaptionedPanel.class, "The CaptionedPanel component");
		addLink(DemoAppTitle.class, "The AppPageTitle component");
		addLink(DemoTabPanel.class, "The TabPanel component");
		addLink(DemoScrollableTabPanel.class, "The ScrollableTabPanel panel, when there's many tabs to show.");
		addLink(DemoSplitterPanel.class, "The SplitterPanel, containing two panels with a movable separator between them");
		addLink(DemoMessageLine.class, "A message line");

		addCaption("Simple components");
		addLink(DemoDefaultButton.class, "The DefaultButton");
		addLink(DemoLinkButton.class, "The LinkButton");
		addLink(DemoALink.class, "The ALink and ATag components: several kinds of links");

		addCaption("Input components");
		addLink(DemoCheckbox.class, "The checkbox component");
		addLink(DemoRadioButton.class, "The RadioButton components");
		addLink(DemoText.class, "The Text<T> component");
		addLink(DemoTextStr.class, "The TextStr component (shortcut for Text<String>)");
		addLink(DemoDateInput.class, "The DateInput component for date and datetime input");
		addLink(DemoComboFixed.class, "The ComboFixed component");
		addLink(DemoFileUpload.class, "File upload component");
		addLink(DemoBulkUpload.class, "The bulk file upload component");
		addLink(DemoTextArea.class, "The TextArea component");
		addLink(DemoHtmlEditor.class, "The small and fast HTMLEditor component");
		addLink(DemoCKEditor.class, "The big HTML editor - CKEditor component");

		addCaption("Display-only components");
		addLink(DemoDisplayValue.class, "The DisplayValue component");
		addLink(DemoDisplayHtml.class, "The DisplayHtml component");
		addLink(DemoDisplayCheckbox.class, "The DisplayCheckbox component");

		addCaption("Graphical components");
		addLink(DemoColorPicker.class, "The color picker in flat (opened) mode");
		addLink(DemoColorPicker2.class, "The color picker in button mode");
		addLink(GraphPage.class, "DOES NOT YET WORK- Pie chart using a dynamic image/JChart");

		addCaption("Tables");
		addLink(TableMenuPage.class, "Data tables, row renderers and ITableModels.");

		addCaption("Trees");
		addLink(DemoTree.class, "The tree component - file system tree, lazily loaded, and file type icons");

		addCaption("Drag and drop");
		addLink(DemoDragDrop.class, "Drag and drop - Petstore (DIV dropmode)");
		addLink(DemoTableInDrag.class, "Drag and drop - ordered row drop mode");

		addCaption("LookupForm and form builder");
		addLink(DemoLookupForm.class, "Using a lookupform to generalize search pages");
		addLink(DemoLookupForm2.class, "LookupForm with LookupInput for a many-to-one relation, and search-as-you-type");

		addLink(FormDesigner.class, "Form designer - work in progress");

		addCaption("Special components");
		addLink(DemoWeekAgenda.class, "The WeekAgenda");
		addLink(DemoAsyncContainer.class, "The AsyncContainer");
		addLink(DemoPollingDiv.class, "The PollingDiv component");
		addLink(DemoPopupMenu.class, "Popup menu");

		addCaption("Binding");
		addLink(DemoObservableListPage.class, "Database relation IObservableList binding");

	}
}
