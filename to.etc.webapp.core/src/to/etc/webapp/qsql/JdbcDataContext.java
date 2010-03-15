package to.etc.webapp.qsql;

import java.sql.*;
import java.util.*;

import to.etc.webapp.query.*;

/**
 * WATCH OUT- THIS DOES NOT OBEY OBJECT IDENTITY RULES!! Records loaded through
 * this code are NOT mapped by identity, so multiple queries for the same object
 * WILL return different copies!!
 *
 * <p>This is a poor-man's datacontext that can be used to do JDBC queries using
 * the QCriteria interface.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Mar 10, 2010
 */
public class JdbcDataContext implements QDataContext {
	/** The originating factory. */
	private QDataContextFactory m_factory;

	/** The underlying connection. */
	private Connection m_dbc;

	private boolean m_ignoreClose;

	public JdbcDataContext(QDataContextFactory factory, Connection dbc) {
		m_factory = factory;
		internalSetConnection(dbc);
		//		dbc.setAutoCommit(false);
	}

	protected void internalSetConnection(Connection dbc) {
		m_dbc = dbc;
	}

	protected Connection internalGetConnection() throws Exception {
		return m_dbc;
	}

	protected void unclosed() throws Exception {
		if(internalGetConnection() == null)
			throw new IllegalStateException("This JDBC Data Context has been closed.");
	}

	private void unsupported() {
		throw new IllegalStateException("Unsupported call for JdbcDataContext");
	}

	/**
	 * DOES NOTHING.
	 * @see to.etc.webapp.query.QDataContext#attach(java.lang.Object)
	 */
	@Override
	public void attach(Object o) throws Exception {
	}

	@Override
	public void close() {
		if(m_ignoreClose)
			return;
		if(m_dbc != null) {
			try {
				m_dbc.rollback();
			} catch(Exception x) {}
			try {
				m_dbc.close();
			} catch(Exception x) {}
			m_dbc = null;
		}
	}

	@Override
	public void commit() throws Exception {
		unclosed();
		internalGetConnection().commit();
	}

	/**
	 * Unsupported for JDBC code.
	 * @see to.etc.webapp.query.QDataContext#delete(java.lang.Object)
	 */
	@Override
	public void delete(Object o) throws Exception {
		unsupported();
	}

	/**
	 * Locate the specified record by PK.
	 * @see to.etc.webapp.query.QDataContext#find(java.lang.Class, java.lang.Object)
	 */
	@Override
	public <T> T find(Class<T> clz, Object pk) throws Exception {
		unclosed();
		return JdbcQuery.find(this, clz, pk);
	}

	/**
	 * Get an instance; this will return an instance by first trying to load it; if that fails
	 * it will create one but only fill the PK. Use is questionable though.
	 * @see to.etc.webapp.query.QDataContext#getInstance(java.lang.Class, java.lang.Object)
	 */
	@Override
	public <T> T getInstance(Class<T> clz, Object pk) throws Exception {
		unclosed();
		return JdbcQuery.getInstance(this, clz, pk);
	}

	/**
	 * Return the underlying connection verbatim.
	 * @see to.etc.webapp.query.QDataContext#getConnection()
	 */
	@Override
	public Connection getConnection() throws Exception {
		unclosed();
		return internalGetConnection();
	}

	@Override
	public QDataContextFactory getFactory() {
		return m_factory;
	}

	@Override
	public boolean inTransaction() throws Exception {
		return false;
	}

	@Override
	public <T> List<T> query(QCriteria<T> q) throws Exception {
		unclosed();
		return JdbcQuery.query(this, q);
	}

	@Override
	public List<Object[]> query(QSelection< ? > sel) throws Exception {
		unclosed();
		return JdbcQuery.query(this, sel);
	}

	@Override
	public <T> T queryOne(QCriteria<T> q) throws Exception {
		unclosed();
		return JdbcQuery.queryOne(this, q);
	}

	@Override
	public Object queryOne(QSelection< ? > q) throws Exception {
		unclosed();
		return JdbcQuery.queryOne(this, q);
	}

	/**
	 * Not suppore
	 * @see to.etc.webapp.query.QDataContext#refresh(java.lang.Object)
	 */
	@Override
	public void refresh(Object o) throws Exception {
		unsupported();
	}

	@Override
	public void rollback() throws Exception {
		unclosed();
		internalGetConnection().rollback();
	}

	/**
	 * Not supported
	 * @see to.etc.webapp.query.QDataContext#save(java.lang.Object)
	 */
	@Override
	public void save(Object o) throws Exception {
		unsupported();
	}

	@Override
	public void setIgnoreClose(boolean on) {
		m_ignoreClose = on;
	}

	@Override
	public void startTransaction() throws Exception {
		unclosed();
	}
}
