package to.etc.domui.component.meta.init;

import to.etc.domui.component.meta.SearchPropertyMetaModel;
import to.etc.domui.component.meta.impl.DefaultClassMetaModel;
import to.etc.domui.component.meta.impl.DefaultPropertyMetaModel;
import to.etc.util.PropertyInfo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This collects whatever extra data is needed during the parse of this object.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Mar 6, 2012
 */
class DefaultJavaClassInfo {
	@Nonnull
	final private DefaultClassMetaModel m_cmm;

	@Nonnull
	final private Map<PropertyInfo, DefaultPropertyMetaModel< ? >> m_map = new HashMap<PropertyInfo, DefaultPropertyMetaModel< ? >>();

	@Nonnull
	final private List<SearchPropertyMetaModel> m_searchList = new ArrayList<SearchPropertyMetaModel>();

	@Nonnull
	final private List<SearchPropertyMetaModel> m_keySearchList = new ArrayList<SearchPropertyMetaModel>();

	@Nonnull
	final private List<Runnable> m_runList;

	public DefaultJavaClassInfo(@Nonnull DefaultClassMetaModel defaultClassMetaModel, @Nonnull List<Runnable> runlist) {
		m_cmm = defaultClassMetaModel;
		m_runList = runlist;
	}

	@Nonnull
	public DefaultClassMetaModel getModel() {
		return m_cmm;
	}

	public Map<PropertyInfo, DefaultPropertyMetaModel< ? >> getMap() {
		return m_map;
	}

	public List<SearchPropertyMetaModel> getSearchList() {
		return m_searchList;
	}

	public List<SearchPropertyMetaModel> getKeySearchList() {
		return m_keySearchList;
	}

	public Class< ? > getTypeClass() {
		return m_cmm.getActualClass();
	}

	public void later(Runnable r) {
		m_runList.add(r);
	}
}
