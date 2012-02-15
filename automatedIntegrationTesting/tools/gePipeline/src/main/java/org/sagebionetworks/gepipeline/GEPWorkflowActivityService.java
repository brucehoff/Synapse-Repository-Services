package org.sagebionetworks.gepipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.AsynchronyExecutorService;

/**
 * This is the process which hosts all the SWF activities exposed in this
 * package.
 * 
 * Note that some of the activities in this package depend upon R and
 * Bioconductor being properly installed on the machine and available in the
 * PATH.
 */
public class GEPWorkflowActivityService {

	private static AmazonSimpleWorkflow swfService;
	private static GEPWorkflowActivityService gepWorkflowActivityServiceInstance;
	private static AsynchronyExecutorService activityExecutor;

	/**
	 * @return the activity service instance
	 */
	public synchronized static GEPWorkflowActivityService getGEPWorkflowActivityServiceInstance() {
		if (gepWorkflowActivityServiceInstance == null) {
			gepWorkflowActivityServiceInstance = new GEPWorkflowActivityService();
		}

		return gepWorkflowActivityServiceInstance;
	}
	
	private static final String CAPABILITY_PROPERTY_NAME = "org.sagebionetworks.gepipeline.capability";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// TODO confirm that R and Bioconductor are available before proceeding.
		// Return a helpful error message to the user if the environment is not
		// sufficient for this service to run.

		// Create the client for Simple Workflow Service
		swfService = ConfigHelper.createSWFClient();

		// Enable routing to specific hosts, based on capabilities
		// Note, we do NOT want to put this property into a properties file, which may be used by multiple
		// hosts sharing a common file system.  Rather we pass in as a command-line property
		// the property contains multiple capabilities, separated by comma, colon, or semi-colon
		String capabilitiesString = System.getProperty(CAPABILITY_PROPERTY_NAME);
		List<String> capabilities = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(capabilitiesString, ",:;");
		while (st.hasMoreTokens()) {
			capabilities.add(st.nextToken());
		}

		// Start Activity Executor Service
		getGEPWorkflowActivityServiceInstance()
				.startGEPWorkflowActivityService(capabilities);

		// Add a Shutdown hook to close ActivityExecutorService
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				try {
					getGEPWorkflowActivityServiceInstance()
							.stopGEPWorkflowActivityService();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}));

		System.out.println("Please press any key to terminate service.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	private void startGEPWorkflowActivityService(List<String> capabilities) throws Exception {
		System.out.println("Starting Agent Service...");

		activityExecutor = new AsynchronyExecutorService(swfService);

		// Discover and register all activities exposed by this package
		activityExecutor
				.addActivitiesFromPackage(GEPWorkflowActivityService.class
						.getPackage().getName());

		activityExecutor.setCapabilities(capabilities);

		// Start ActivityExecutor Service
		activityExecutor.start();

		System.out.println("Agent Service Started...");
	}

	private void stopGEPWorkflowActivityService() throws InterruptedException {
		System.out.println("Stopping Agent Service...");
		activityExecutor.shutdownNow();
		swfService.shutdown();
		activityExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		System.out.println("Agent Service Stopped.");
	}

}
