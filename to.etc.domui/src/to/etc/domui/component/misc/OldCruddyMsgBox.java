package to.etc.domui.component.misc;

import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;

/**
 * A floating messagebox component (example).
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 27, 2008
 */
public class OldCruddyMsgBox extends Div {
	private String m_title;

	private NodeBase m_contents;

	public OldCruddyMsgBox() {}

	public OldCruddyMsgBox(String title, NodeBase contents) {
		m_contents = contents;
		m_title = title;
	}

	@Override
	public void createContent() throws Exception {
		Div ttlbar = new Div();
		add(ttlbar);
		Img img = new Img();
		img.setSrc("THEME/close.png");
		img.setFloat(FloatType.RIGHT);
		ttlbar.add(img);
		ttlbar.add(m_title);
		ttlbar.setCssClass("ui-mb-ttl");

		add(m_contents);
		setPosition(PositionType.ABSOLUTE);
		setZIndex(10);
		setTop("100px");
		setLeft(100);
		setCssClass("ui-mb");

		img.setClicked(new IClicked<Img>() {
			public void clicked(Img b) throws Exception {
				OldCruddyMsgBox.this.remove();
			}
		});
	}

	static public <T extends NodeBase> void message(T pg, String title, String message) {
		OldCruddyMsgBox box = new OldCruddyMsgBox(title, new TextNode(message));
		pg.getPage().getBody().add(box);
	}
}