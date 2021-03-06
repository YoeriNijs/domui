package to.etc.domuidemo.pages.overview.tbl;

import to.etc.domui.component.meta.*;
import to.etc.domui.component.misc.*;
import to.etc.domui.component.tbl.*;
import to.etc.domui.dom.html.*;
import to.etc.domuidemo.db.*;
import to.etc.webapp.query.*;

public class DemoDataTable2 extends UrlPage {
	@Override
	public void createContent() throws Exception {

		QCriteria<Album> q = QCriteria.create(Album.class);
		SimpleSearchModel<Album> ssm = new SimpleSearchModel<Album>(this, q);

		BasicRowRenderer<Album> brr = new BasicRowRenderer<Album>(Album.class, "artist.name", "title");
		DataTable<Album> dt = new DataTable<Album>(ssm, brr);
		add(dt);
		dt.setPageSize(25);
		add(new DataPager(dt));

		brr.setRowClicked(new ICellClicked<Album>() {
			@Override
			public void cellClicked(Album rowval) throws Exception {
				MsgBox.message(DemoDataTable2.this, MsgBox.Type.INFO, "You selected: " + MetaManager.identify(rowval));
			}
		});
	}
}
