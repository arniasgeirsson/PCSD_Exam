package com.acertainsupplychain.clients;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utility.ItemSupplierMessageTag;
import com.acertainsupplychain.utility.ItemSupplierResult;
import com.acertainsupplychain.utility.ItemSupplierUtility;

//import org.eclipse.jetty.io.ByteArrayBuffer;

/**
 * This class works as a proxy to an actual OrderManager server. This class uses
 * a Jetty HttpClient to synchronously communicate with the underlying
 * OrderManager server.
 * 
 * @author Arni
 * 
 */
public class OrderManagerHTTPProxy implements OrderManager {
	private final HttpClient client;
	private final String orderManagerAddress;

	/**
	 * Initialize the OrderManager proxy object with a given list of
	 * ItemSupplier objects, mapped to their suppler IDs.
	 */
	public OrderManagerHTTPProxy(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers, int port) throws Exception {

		orderManagerAddress = "http://localhost:" + port;
		client = ItemSupplierUtility.setupNewHttpClient();
		client.start();
		initializeOrderManager(orderManagerID, suppliers);
	}

	/**
	 * Initialize the OrderManager proxy object with a given list of server
	 * ports mapped to the ID of their respected ItemSupplier.
	 * 
	 * The main reason for this function and the associated
	 * initializeOrderManagerProxy function is that XStream had trouble
	 * serializing and de-serializing the proxy objects.
	 * 
	 * @param orderManagerID
	 * @param port
	 * @param suppliers
	 * @throws Exception
	 */
	public OrderManagerHTTPProxy(int orderManagerID, int port,
			Map<Integer, Integer> suppliers) throws Exception {

		orderManagerAddress = "http://localhost:" + port;
		client = ItemSupplierUtility.setupNewHttpClient();
		client.start();
		initializeOrderManagerProxy(orderManagerID, suppliers);
	}

	/**
	 * Sends a HTTP request to the underlying OrderManager server to initialize
	 * the OrderManager with a set of ItemSupplier ports and let the
	 * OrderManager it self create the needed ItemSupplier proxies.
	 * 
	 * @param orderManagerID
	 * @param suppliers
	 * @throws OrderProcessingException
	 */
	private void initializeOrderManagerProxy(int orderManagerID,
			Map<Integer, Integer> suppliers) throws OrderProcessingException {

		String suppliersXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(suppliers);
		Buffer requestContent = new ByteArrayBuffer(suppliersXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.INIT_ORDERMANAGER_PROXY + "?"
				+ ItemSupplierClientConstants.INIT_ORDERMANAGER_ID + "="
				+ ItemSupplierUtility.encodeInteger(orderManagerID);
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		// We do not care about the responds, only if an exception had occurred
		ItemSupplierUtility.sendAndRecv(client, exchange);
	}

	/**
	 * Initialize the underlying OrderManager with a HTTP request including the
	 * ItemSupplier objects the OrderManager should have access to.
	 * 
	 * @param orderManagerID
	 * @param suppliers
	 * @throws OrderProcessingException
	 */
	private void initializeOrderManager(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {

		String suppliersXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(suppliers);
		Buffer requestContent = new ByteArrayBuffer(suppliersXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.INIT_ORDERMANAGER + "?"
				+ ItemSupplierClientConstants.INIT_ORDERMANAGER_ID + "="
				+ ItemSupplierUtility.encodeInteger(orderManagerID);
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		// We do not care about the responds, only if an exception had occurred
		ItemSupplierUtility.sendAndRecv(client, exchange);
	}

	/**
	 * Stop the HttpClient object.
	 */
	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void clear() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.CLEAR;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int registerOrderWorkflow(List<OrderStep> steps)
			throws OrderProcessingException {

		String stepsXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(steps);
		Buffer requestContent = new ByteArrayBuffer(stepsXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.REGISTERWORKFLOW;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		return (Integer) ItemSupplierUtility.sendAndRecv(client, exchange)
				.getResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
			throws InvalidWorkflowException {

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");

		String urlEncodedOrderWorkflowID = null;

		try {
			urlEncodedOrderWorkflowID = ItemSupplierUtility
					.encodeInteger(orderWorkflowId);
		} catch (OrderProcessingException e) {
			throw new InvalidWorkflowException(
					"Unsupported encoding exception", e);
		}

		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.GETWORKFLOWSTATUS + "?"
				+ ItemSupplierClientConstants.GETWORKFLOWSTATUS_PARAM + "="
				+ urlEncodedOrderWorkflowID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			throw new InvalidWorkflowException(e);
		}

		return (List<StepStatus>) result.getResult();
	}

	@Override
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException {

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");

		String urlEncodedSupplierID = ItemSupplierUtility
				.encodeInteger(supplierID);

		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.JOBGETSUPID + "?"
				+ ItemSupplierClientConstants.JOBGETSUPPLIER_PARAM + "="
				+ urlEncodedSupplierID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			throw new InvalidWorkflowException(e);
		}

		return (ItemSupplier) result.getResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<OrderStep> jobGetWorkflow(int workflowID)
			throws OrderProcessingException {

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");

		String urlEncodedWorkflowID = ItemSupplierUtility
				.encodeInteger(workflowID);

		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.JOBGETWORKFLOW + "?"
				+ ItemSupplierClientConstants.JOBGETWORKFLOW_PARAM + "="
				+ urlEncodedWorkflowID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			throw new InvalidWorkflowException(e);
		}

		return (List<OrderStep>) result.getResult();
	}

	@Override
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException {

		String stepStatusXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(status);
		Buffer requestContent = new ByteArrayBuffer(stepStatusXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");

		String urlEncodedWorkflowID = ItemSupplierUtility
				.encodeInteger(workflowID);
		String urlEncodedStepIndex = ItemSupplierUtility
				.encodeInteger(stepIndex);

		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.JOBSETSTATUS + "?"
				+ ItemSupplierClientConstants.JOBSETSTATUS_PARAM_WID + "="
				+ urlEncodedWorkflowID + "?"
				+ ItemSupplierClientConstants.JOBSETSTATUS_PARAM_STEPINDEX
				+ "=" + urlEncodedStepIndex;
		exchange.setURL(urlString);

		exchange.setRequestContent(requestContent);

		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			throw new InvalidWorkflowException(e);
		}
	}

	@Override
	public void waitForJobsToFinish() throws OrderProcessingException {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.WAITFORJOBS;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		ItemSupplierUtility.sendAndRecv(client, exchange);
	}

	@Override
	public void stopItemSupplierProxies() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");
		String urlString = orderManagerAddress + "/"
				+ ItemSupplierMessageTag.ORDERMANAGER_STOP;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
		}
	}

}
