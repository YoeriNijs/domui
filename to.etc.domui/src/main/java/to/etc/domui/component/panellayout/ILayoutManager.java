package to.etc.domui.component.panellayout;

import javax.annotation.*;

import to.etc.domui.dom.html.*;

public interface ILayoutManager {
	public void place(@Nonnull LayoutPanelBase layoutPanelBase, @Nonnull NodeBase node, @Nonnull Object layoutOptions);

}
