package com.acertainsupplychain.clients;

/**
 * 
 * Constant values used through the acertainsupplychain project. Partially
 * greatly inspired by some of the course assignments.
 * 
 */
public final class ItemSupplierClientConstants {

	public static final int ORDERMANAGER_MAX_THREADSPOOL_SIZE = 10;
	public static final String GETWORKFLOWSTATUS_PARAM = "workflowID";
	public static final String JOBGETSUPPLIER_PARAM = "jobSupplierID";
	public static final String JOBGETWORKFLOW_PARAM = "jobWorkflowID";
	public static final String JOBSETSTATUS_PARAM_WID = "jobSetWorkflowID";
	public static final String JOBSETSTATUS_PARAM_STEPINDEX = "jobSetStepIndex";
	public static final String INIT_ITEMSUPPLIER_PARAM = "initSupplierID";
	public static final String INIT_ORDERMANAGER_ID = "initOrderManagerID";

	public static final int CLIENT_MAX_CONNECTION_ADDRESS = 200;
	public static final int CLIENT_MAX_THREADSPOOL_THREADS = 250;
	public static final int CLIENT_MAX_TIMEOUT_MILLISECS = 30000;

	public static final String strERR_CLIENT_REQUEST_SENDING = "ERR_CLIENT_REQUEST_SENDING";
	public static final String strERR_CLIENT_REQUEST_EXCEPTION = "ERR_CLIENT_REQUEST_EXCEPTION";
	public static final String strERR_CLIENT_REQUEST_TIMEOUT = "CLIENT_REQUEST_TIMEOUT";
	public static final String strERR_CLIENT_RESPONSE_DECODING = "CLIENT_RESPONSE_DECODING";
	public static final String strERR_CLIENT_UNKNOWN = "CLIENT_UNKNOWN";
	public static final String strERR_CLIENT_ENCODING = "CLIENT_ENCODING";

}
