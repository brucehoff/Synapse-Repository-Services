package org.sagebionetworks.table.cluster;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Note: For the first pass at this feature we are only using one database. This
 * will be extended in the future.
 * 
 * @author jmhill
 *
 */
public class ConnectionFactoryImpl implements ConnectionFactory {

	Logger log = LogManager.getLogger(ConnectionFactoryImpl.class);

	@Autowired
	InstanceDiscovery instanceDiscovery;
	/**
	 * Note: This field will be remove when we actually have more than one
	 * connection. It is a simple way to get the functionality up and running
	 * without adding full support for a load balancing.
	 */
	private BasicDataSource singleConnectionPool;

	@Autowired
	private StackConfiguration stackConfig;

	/**
	 * Note: The DAO is autowired so it can be profiled. See: PLFM-5984. We might
	 * need an alternate solution to support multiple database connections in the
	 * future.
	 */
	@Autowired
	private TableIndexDAO tableIndexDao;

	@Override
	public TableIndexDAO getConnection(IdAndVersion tableId) {
		// Create a new DAO for this call.
		return tableIndexDao;
	}

	/**
	 * This is called when the Spring bean is initialized.
	 */
	public void initialize() {
		// There is nothing to do if the table feature is not enabled.
		// The features is enabled so we must find all database instances that we can
		// use
		List<InstanceInfo> instances = instanceDiscovery.discoverAllInstances();
		if (instances == null || instances.isEmpty())
			throw new IllegalArgumentException("Did not find at least one database instances.");

		// This will be improved in the future. For now we just use the first database
		// we find
		InstanceInfo instance = instances.get(0);
		// Use the one instance to create a single connection pool
		singleConnectionPool = InstanceUtils.createNewDatabaseConnectionPool(stackConfig, instance);
		// ensure the index has the correct tables
		tableIndexDao.setDataSource(singleConnectionPool);
		tableIndexDao.createEntityReplicationTablesIfDoesNotExist();
	}

	/**
	 * Spring will calls this method when this bean is destroyed. This is our chance
	 * to shutdown the database connection pools.
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		if (singleConnectionPool != null) {
			log.debug("Closing connection pool to: " + singleConnectionPool.getUrl());
			singleConnectionPool.close();
		}
	}

	@Override
	public List<TableIndexDAO> getAllConnections() {
		List<TableIndexDAO> results = new LinkedList<>();
		results.add(tableIndexDao);
		return results;
	}

	@Override
	public TableIndexDAO getFirstConnection() {
		return tableIndexDao;
	}

	@Override
	public DataSource getFirstDataSource() {
		return singleConnectionPool;
	}

}
