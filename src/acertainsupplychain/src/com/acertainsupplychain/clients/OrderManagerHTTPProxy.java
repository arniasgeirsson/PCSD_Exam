/**
 * 
 */
package com.acertainsupplychain.clients;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
//import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utility.ItemSupplierMessageTag;
import com.acertainsupplychain.utility.ItemSupplierResult;
import com.acertainsupplychain.utility.ItemSupplierUtility;

//TODO Inspired by the weekly assignments

/**
 * TODO ReplicationAwareBookStoreHTTPProxy implements the client level
 * synchronous CertainBookStore API declared in the BookStore class. It keeps
 * retrying the API until a consistent reply is returned from the replicas
 * 
 */
public class OrderManagerHTTPProxy implements OrderManager {
	private final HttpClient client;
	// private Set<String> slaveAddresses;
	private final String orderManagerAddress;

	// private final String filePath = "C:/proxy.properties";

	// private volatile long snapshotId = 0;

	// public long getSnapshotId() {
	// return snapshotId;
	// }
	//
	// public void setSnapshotId(long snapShotId) {
	// snapshotId = snapShotId;
	// }

	/**
	 * Initialize the client object
	 */
	public OrderManagerHTTPProxy(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers, int port) throws Exception {
		orderManagerAddress = "http://localhost:" + port;

		// initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		// max concurrent connections to every address
		client.setMaxConnectionsPerAddress(ItemSupplierClientConstants.CLIENT_MAX_CONNECTION_ADDRESS);
		// max threads
		client.setThreadPool(new QueuedThreadPool(
				ItemSupplierClientConstants.CLIENT_MAX_THREADSPOOL_THREADS));
		// seconds timeout if server reply, the request expires
		client.setTimeout(ItemSupplierClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS);
		client.start();

		initializeOrderManager(orderManagerID, suppliers);
	}

	public OrderManagerHTTPProxy(int orderManagerID, int port,
			Map<Integer, Integer> suppliers) throws Exception {
		orderManagerAddress = "http://localhost:" + port;

		// initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		// max concurrent connections to every address
		client.setMaxConnectionsPerAddress(ItemSupplierClientConstants.CLIENT_MAX_CONNECTION_ADDRESS);
		// max threads
		client.setThreadPool(new QueuedThreadPool(
				ItemSupplierClientConstants.CLIENT_MAX_THREADSPOOL_THREADS));
		// seconds timeout if server reply, the request expires
		client.setTimeout(ItemSupplierClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS);
		client.start();

		initializeOrderManagerProxy(orderManagerID, suppliers);
	}

	// TODO use post or get? really just throw error?
	private void initializeOrderManagerProxy(int orderManagerID,
			Map<Integer, Integer> suppliers) throws OrderProcessingException {
		String suppliersXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(suppliers);
		Buffer requestContent = new ByteArrayBuffer(suppliersXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.INIT_ORDERMANAGER_PROXY + "?"
				+ ItemSupplierClientConstants.INIT_ORDERMANAGER_ID + "="
				+ ItemSupplierUtility.encodeInteger(orderManagerID);
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		// We do not care about the responds, only if an exception had occurred
		// try {
		ItemSupplierUtility.sendAndRecv(client, exchange);
		// } catch (OrderProcessingException e) {
		// TODO what to do?
		// System.out.println("211 -- -- - -- - - -What to do?");
		// e.printStackTrace();
		// }
	}

	// TODO use post or get? really just throw error?
	private void initializeOrderManager(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {
		String suppliersXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(suppliers);
		Buffer requestContent = new ByteArrayBuffer(suppliersXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.INIT_ORDERMANAGER + "?"
				+ ItemSupplierClientConstants.INIT_ORDERMANAGER_ID + "="
				+ ItemSupplierUtility.encodeInteger(orderManagerID);
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		// We do not care about the responds, only if an exception had occurred
		// try {
		ItemSupplierUtility.sendAndRecv(client, exchange);
		// } catch (OrderProcessingException e) {
		// TODO what to do?
		// System.out.println("21 -- -- - -- - - -What to do?");
		// e.printStackTrace();
		// }
	}

	public String getOrderManagerAddress() {
		return orderManagerAddress;
	}

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO use post or get?
	@Override
	public void clear() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.CLEAR;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do?
			System.out.println("2 -- -- - -- - - -What to do?");
			// e.printStackTrace();
		}
	}

	// TODO use post or get?
	@Override
	public int registerOrderWorkflow(List<OrderStep> steps)
			throws OrderProcessingException {
		String stepsXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(steps);
		Buffer requestContent = new ByteArrayBuffer(stepsXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.REGISTERWORKFLOW;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		ItemSupplierResult result = null;
		result = ItemSupplierUtility.sendAndRecv(client, exchange);

		return (Integer) result.getResult();
	}

	// TODO use post or get?
	@SuppressWarnings("unchecked")
	@Override
	public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
			throws InvalidWorkflowException {
		// String itemIdsXMLString = ItemSupplierUtility
		// .serializeObjectToXMLString(itemIds);
		// Buffer requestContent = new ByteArrayBuffer(itemIdsXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedOrderWorkflowID = null;

		try {
			urlEncodedOrderWorkflowID = URLEncoder.encode(
					Integer.toString(orderWorkflowId), "UTF-8"); // TODO
																	// constant?
		} catch (UnsupportedEncodingException e) {
			throw new InvalidWorkflowException(
					"Unsupported encoding exception", e);
		}

		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.GETWORKFLOWSTATUS + "?"
				+ ItemSupplierClientConstants.GETWORKFLOWSTATUS_PARAM + "="
				+ urlEncodedOrderWorkflowID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("31 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			throw new InvalidWorkflowException("", e);
		}

		return (List<StepStatus>) result.getResult();
	}

	// TODO use post or get?
	@Override
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedSupplierID = null;

		try {
			urlEncodedSupplierID = URLEncoder.encode(
					Integer.toString(supplierID), "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new InvalidWorkflowException(
					"Unsupported encoding exception", e);
		}

		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.JOBGETSUPID + "?"
				+ ItemSupplierClientConstants.JOBGETSUPPLIER_PARAM + "="
				+ urlEncodedSupplierID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("32 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			throw new InvalidWorkflowException("", e);
		}

		return (ItemSupplier) result.getResult();
	}

	// TODO use post or get?
	@SuppressWarnings("unchecked")
	@Override
	public List<OrderStep> jobGetWorkFlow(int workflowID)
			throws OrderProcessingException {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedWorkflowID = null;

		try {
			urlEncodedWorkflowID = URLEncoder.encode(
					Integer.toString(workflowID), "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new InvalidWorkflowException(
					"Unsupported encoding exception", e);
		}

		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.JOBGETWORKFLOW + "?"
				+ ItemSupplierClientConstants.JOBGETWORKFLOW_PARAM + "="
				+ urlEncodedWorkflowID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("32 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			throw new InvalidWorkflowException("", e);
		}

		return (List<OrderStep>) result.getResult();
	}

	// TODO use post or get?
	@Override
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedWorkflowID = null;

		try {
			urlEncodedWorkflowID = URLEncoder.encode(
					Integer.toString(workflowID), "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new OrderProcessingException(
					"Unsupported encoding exception", e);
		}

		String urlEncodedStepIndex = null;

		try {
			urlEncodedStepIndex = URLEncoder.encode(
					Integer.toString(stepIndex), "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new InvalidWorkflowException(
					"Unsupported encoding exception", e);
		}

		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.JOBSETSTATUS + "?"
				+ ItemSupplierClientConstants.JOBSETSTATUS_PARAM_WID + "="
				+ urlEncodedWorkflowID + "?"
				+ ItemSupplierClientConstants.JOBSETSTATUS_PARAM_STEPINDEX
				+ "=" + urlEncodedStepIndex;

		exchange.setURL(urlString);

		String stepStatusXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(status);
		Buffer requestContent = new ByteArrayBuffer(stepStatusXMLString);
		exchange.setRequestContent(requestContent);

		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("33 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			throw new InvalidWorkflowException("", e);
		}
	}

	// TODO use post or get?
	@Override
	public void waitForJobsToFinish() throws OrderProcessingException {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.WAITFORJOBS;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do?
			System.out.println("2 -- -- - -- - - -What to do?");
			// e.printStackTrace();
		}
	}

	@Override
	public void stopItemSupplierProxies() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getOrderManagerAddress() + "/"
				+ ItemSupplierMessageTag.ORDERMANAGER_STOP;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do?
			System.out.println("2 -- -- - -- - - -What to do?");
			// e.printStackTrace();
		}
	}

}
