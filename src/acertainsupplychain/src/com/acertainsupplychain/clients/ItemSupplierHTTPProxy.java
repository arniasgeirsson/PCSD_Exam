/**
 * 
 */
package com.acertainsupplychain.clients;

import java.util.List;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
//import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
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
public class ItemSupplierHTTPProxy implements ItemSupplier {
	private final HttpClient client;
	private final String itemSupplierAddress;

	// private final String filePath = "C:/proxy.properties";

	/**
	 * Initialize the client object
	 */
	public ItemSupplierHTTPProxy(int supplierID, int port) throws Exception {
		itemSupplierAddress = "http://localhost:" + port;

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

		initializeItemSupplier(supplierID);
	}

	// TODO use post or get?
	private void initializeItemSupplier(int supplierID) {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedsupplierID = null;

		try {
			urlEncodedsupplierID = ItemSupplierUtility
					.encodeInteger(supplierID);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
			// TODO have to handle the error somehow?
		}

		String urlString = getItemSupplierAddress() + "/"
				+ ItemSupplierMessageTag.INIT_ITEMSUPPLIER + "?"
				+ ItemSupplierClientConstants.INIT_ITEMSUPPLIER_PARAM + "="
				+ urlEncodedsupplierID;
		exchange.setURL(urlString);

		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("381 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			// throw new InvalidWorkflowException("", e);
		}
	}

	public String getItemSupplierAddress() {
		return itemSupplierAddress;
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
	public void executeStep(OrderStep step) throws OrderProcessingException {
		String stepXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(step);
		Buffer requestContent = new ByteArrayBuffer(stepXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getItemSupplierAddress() + "/"
				+ ItemSupplierMessageTag.EXECUTESTEP;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		// We do not care about the responds, only if an exception had occurred
		// in which case it would be thrown inside ItemSupplierUtility
		ItemSupplierUtility.sendAndRecv(client, exchange);
	}

	// TODO use post or get?
	@SuppressWarnings("unchecked")
	@Override
	public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
			throws InvalidItemException {
		String itemIdsXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(itemIds);
		Buffer requestContent = new ByteArrayBuffer(itemIdsXMLString);
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getItemSupplierAddress() + "/"
				+ ItemSupplierMessageTag.GETORDERS;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do? Wrap inside InvalidItemException, or change API?
			// -> Change API
			System.out.println("3 -- -- - -- - - -What to do?");
			// e.printStackTrace();
			throw new InvalidItemException(
					"getOrdersPerItem-Proxy: sendAndRecv threw this error.", e);
		}

		return (List<ItemQuantity>) result.getResult();
	}

	// TODO use post or get?
	@Override
	public void clear() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getItemSupplierAddress() + "/"
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
	public int getSupplierID() {
		// return supplierID;
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = getItemSupplierAddress() + "/"
				+ ItemSupplierMessageTag.GETSUPID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			// TODO what to do?
			System.out.println("1 -- -- - -- - - -What to do?");
			// e.printStackTrace();
		}
		return (Integer) result.getResult();
	}

	// TODO seems kinda stupid..
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ItemSupplierHTTPProxy))
			return false;

		ItemSupplierHTTPProxy item = (ItemSupplierHTTPProxy) obj;
		return itemSupplierAddress == item.itemSupplierAddress;
	}

	// TODO
	// http://stackoverflow.com/questions/27581/overriding-equals-and-hashcode-in-java
	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
