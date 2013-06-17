/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.hibernate.cfg.Environment;
import org.jboss.cache.PropertyConfigurator;

/**
 * Support for a standalone JBossCache (TreeCache) instance.  The JBossCache is configured
 * via a local config resource.
 *
 * @author Gavin King
 */
public class TreeCacheProvider implements CacheProvider {

	/**
	 * @deprecated use {@link org.hibernate.cfg.Environment#CACHE_PROVIDER_CONFIG}
	 */
	public static final String CONFIG_RESOURCE = "hibernate.cache.tree_cache.config";
	public static final String DEFAULT_CONFIG = "treecache-optimistic.xml";

	private static final Logger log = LoggerFactory.getLogger( TreeCacheProvider.class );

	private org.jboss.cache.TreeCache cache;
	private TransactionManager transactionManager;

	/**
	 * Construct and configure the Cache representation of a named cache region.
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 * @return The Cache representation of the named cache region.
	 * @throws CacheException Indicates an error building the cache region.
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new TreeCache(cache, regionName, transactionManager);
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	/**
	 * Prepare the underlying JBossCache TreeCache instance.
	 *
	 * @param properties All current config settings.
	 *
	 * @throws CacheException Indicates a problem preparing cache for use.
	 */
	public void start(Properties properties) {
		String resource = properties.getProperty( Environment.CACHE_PROVIDER_CONFIG );

		if ( resource == null ) {
			resource = properties.getProperty( CONFIG_RESOURCE );
		}
		if ( resource == null ) {
			resource = DEFAULT_CONFIG;
		}
		log.debug( "Configuring TreeCache from resource [" + resource + "]" );
		try {
			cache = new org.jboss.cache.TreeCache();
			PropertyConfigurator config = new PropertyConfigurator();
			config.configure( cache, resource );
			TransactionManagerLookup transactionManagerLookup = TransactionManagerLookupFactory.getTransactionManagerLookup(properties);
			if (transactionManagerLookup!=null) {
				cache.setTransactionManagerLookup( new TransactionManagerLookupAdaptor(transactionManagerLookup, properties) );
				transactionManager = transactionManagerLookup.getTransactionManager(properties);
			}
			cache.start();
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void stop() {
		if (cache!=null) {
			cache.stop();
			cache.destroy();
			cache=null;
		}
	}
	
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	static final class TransactionManagerLookupAdaptor implements org.jboss.cache.TransactionManagerLookup {
		private final TransactionManagerLookup tml;
		private final Properties props;
		TransactionManagerLookupAdaptor(TransactionManagerLookup tml, Properties props) {
			this.tml=tml;
			this.props=props;
		}
		public TransactionManager getTransactionManager() throws Exception {
			return tml.getTransactionManager(props);
		}
	}

	public org.jboss.cache.TreeCache getUnderlyingCache() {
		return cache;
	}
}
