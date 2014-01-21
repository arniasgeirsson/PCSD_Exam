package com.acertainsupplychain.clients;

import java.util.List;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utility.ItemSupplierMessageTag;
import com.acertainsupplychain.utility.ItemSupplierResult;
import com.acertainsupplychain.utility.ItemSupplierUtility;

/**
 * This class works as a proxy to an actual ItemSupplier server. This class uses
 * a Jetty HttpClient to synchronously communicate with the underlying
 * ItemSupplier server.
 * 
 */
public class ItemSupplierHTTPProxy implements ItemSupplier {
	private final HttpClient client;
	private final String itemSupplierAddress;

	/**
	 * Initialize the ItemSupplier proxy.
	 */
	public ItemSupplierHTTPProxy(int supplierID, int port) throws Exception {
		itemSupplierAddress = "http://localhost:" + port;
		client = ItemSupplierUtility.setupNewHttpClient();
		client.start();
		initializeItemSupplier(supplierID);
	}

	/**
	 * Sends a HTTP request to initialize an ItemSupplier with the given
	 * supplier ID on the server side.
	 * 
	 * @param supplierID
	 *            , the provided supplier ID that the itemsupplier must have
	 */
	private void initializeItemSupplier(int supplierID) {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET");

		String urlEncodedsupplierID = null;

		try {
			urlEncodedsupplierID = ItemSupplierUtility
					.encodeInteger(supplierID);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
			return;
		}

		String urlString = itemSupplierAddress + "/"
				+ ItemSupplierMessageTag.INIT_ITEMSUPPLIER + "?"
				+ ItemSupplierClientConstants.INIT_ITEMSUPPLIER_PARAM + "="
				+ urlEncodedsupplierID;
		exchange.setURL(urlString);

		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops the HttpClient within this proxy.
	 */
	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void executeStep(OrderStep step) throws OrderProcessingException {
		String stepXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(step);
		Buffer requestContent = new ByteArrayBuffer(stepXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = itemSupplierAddress + "/"
				+ ItemSupplierMessageTag.EXECUTESTEP;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		// We do not care about the responds, only if an exception had occurred
		// in which case it would be thrown inside ItemSupplierUtility
		ItemSupplierUtility.sendAndRecv(client, exchange);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
			throws InvalidItemException {
		String itemIdsXMLString = ItemSupplierUtility
				.serializeObjectToXMLString(itemIds);
		Buffer requestContent = new ByteArrayBuffer(itemIdsXMLString);

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = itemSupplierAddress + "/"
				+ ItemSupplierMessageTag.GETORDERS;
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			throw new InvalidItemException(
					"getOrdersPerItem-Proxy: sendAndRecv threw this error.", e);
		}

		return (List<ItemQuantity>) result.getResult();
	}

	@Override
	public void clear() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = itemSupplierAddress + "/"
				+ ItemSupplierMessageTag.CLEAR;
		exchange.setURL(urlString);

		// We do not care about the responds, only if an exception had occurred
		try {
			ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
		}
	}

	// This function could have been optimized if the ItemSupplier ID were
	// stored in the proxy class, but I decided not to do that to allow the
	// flexibility of an ItemSupplier proxy could switch ItemSupplier address.
	@Override
	public int getSupplierID() {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		String urlString = itemSupplierAddress + "/"
				+ ItemSupplierMessageTag.GETSUPID;
		exchange.setURL(urlString);

		ItemSupplierResult result = null;
		try {
			result = ItemSupplierUtility.sendAndRecv(client, exchange);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
		}
		return (Integer) result.getResult();
	}

	// @Override
	// public boolean equals(Object obj) {
	// if (obj == null)
	// return false;
	// if (obj == this)
	// return true;
	// if (!(obj instanceof ItemSupplierHTTPProxy))
	// return false;
	//
	// ItemSupplierHTTPProxy item = (ItemSupplierHTTPProxy) obj;
	// return itemSupplierAddress == item.itemSupplierAddress;
	// }

}
