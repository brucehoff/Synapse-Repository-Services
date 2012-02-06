package org.sagebionetworks.tool.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigurationTest {

	static private MigrationConfigurationImpl configuration = new MigrationConfigurationImpl();
	
	@BeforeClass
	public static void before() throws IOException{
		// Load the test config file
		String file = ConfigurationTest.class.getClassLoader().getResource("configuation-test.properties").getFile().replaceAll("%20", " ");
		configuration.loadConfigurationFile(file);
	}

	
	@Test
	public void testConfiguration() throws IOException{

		// Test the source
		SynapseConnectionInfo info = configuration.getSourceConnectionInfo();
		assertNotNull(info);
		assertEquals("sourceAuth", info.getAuthenticationEndPoint());
		assertEquals("sourceRepo", info.getRepositoryEndPoint());
		assertEquals("sourceUser", info.getAdminUsername());
		assertEquals("sourcePassword", info.getAdminPassword());
		
		// Test the destination
		info = configuration.getDestinationConnectionInfo();
		assertNotNull(info);
		assertEquals("destAuth", info.getAuthenticationEndPoint());
		assertEquals("destRepo", info.getRepositoryEndPoint());
		assertEquals("destUser", info.getAdminUsername());
		assertEquals("destPassword", info.getAdminPassword());
	}
	
	@Test
	public void testMaxThreads(){
		assertEquals(10, configuration.getMaximumNumberThreads());
	}
	
	@Test
	public void testMaxBatchSize(){
		assertEquals(100, configuration.getMaximumBatchSize());
	}
	
	@Test
	public void testWorkerTimeout(){
		assertEquals(30*1000, configuration.getWorkerTimeoutMs());
	}
}
