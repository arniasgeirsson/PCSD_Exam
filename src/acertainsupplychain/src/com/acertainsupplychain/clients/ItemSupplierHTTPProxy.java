/**
 * 
 */
package com.acertainsupplychain.clients;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
	// private Set<String> slaveAddresses;
	private final String itemSupplierAddress;

	// private final String filePath = "C:/proxy.properties";

	// private volatile long snapshotId = 0;

	// public long getSnapshotId() {
	// return snapshotId;
	// }
	//
	// public void setSnapshotId(long snapShotId) {
	// snapshotId = snapShotId;
	// }

	private final int supplierID;

	/**
	 * Initialize the client object
	 */
	public ItemSupplierHTTPProxy(int supplierID, int port) throws Exception {
		this.supplierID = supplierID;
		itemSupplierAddress = "http://localhost:" + port;

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

		initializeItemSupplier(supplierID);
	}

	// TODO use post or get?
	private void initializeItemSupplier(int supplierID) {
		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("GET"); // TODO correct?

		String urlEncodedsupplierID = null;

		try {
			urlEncodedsupplierID = URLEncoder.encode(
					Integer.toString(supplierID), "UTF-8"); // TODO
															// constant?
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			// throw new InvalidWorkflowException(
			// "Unsupported encoding exception", e);
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
			e.printStackTrace();
			// throw new InvalidWorkflowException("", e);
		}
	}

	// private void initializeReplicationAwareMappings() throws IOException {
	//
	// Properties props = new Properties();
	// slaveAddresses = new HashSet<String>();
	//
	// props.load(new FileInputStream(filePath));
	// masterAddress = props.getProperty(BookStoreConstants.KEY_MASTER);
	// if (!masterAddress.toLowerCase().startsWith("http://")) {
	// masterAddress = new String("http://" + masterAddress);
	// }
	//
	// String slaveAddresses = props.getProperty(BookStoreConstants.KEY_SLAVE);
	// for (String slave : slaveAddresses
	// .split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
	// if (!slave.toLowerCase().startsWith("http://")) {
	// slave = new String("http://" + slave);
	// }
	// this.slaveAddresses.add(slave);
	// }
	// }

	// public String getReplicaAddress() {
	// int slaveIndex = new Random(System.currentTimeMillis())
	// .nextInt(slaveAddresses.size() + 1);
	// if (slaveIndex == slaveAddresses.size())
	// return masterAddress;
	// return slaveAddresses.toArray(new String[0])[slaveIndex];
	// }

	public String getItemSupplierAddress() {
		return itemSupplierAddress;
	}

	// @Override
	// public void buyBooks(Set<BookCopy> isbnSet) throws BookStoreException {
	//
	// String listISBNsxmlString = BookStoreUtility
	// .serializeObjectToXMLString(isbnSet);
	// Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);
	//
	// BookStoreResult result = null;
	//
	// ContentExchange exchange = new ContentExchange();
	// String urlString = getMasterServerAddress() + "/"
	// + BookStoreMessageTag.BUYBOOKS;
	// exchange.setMethod("POST");
	// exchange.setURL(urlString);
	// exchange.setRequestContent(requestContent);
	// result = BookStoreUtility.SendAndRecv(client, exchange);
	// setSnapshotId(result.getSnapshotId());
	// }

	//

	//
	// @SuppressWarnings("unchecked")
	// public List<Book> getEditorPicks(int numBooks) {
	// ContentExchange exchange = new ContentExchange();
	// String urlEncodedNumBooks = null;
	//
	// try {
	// urlEncodedNumBooks = URLEncoder.encode(Integer.toString(numBooks),
	// "UTF-8");
	// } catch (UnsupportedEncodingException ex) {
	// throw new BookStoreException("unsupported encoding of numbooks", ex);
	// }
	//
	// BookStoreResult result = null;
	// do {
	// String urlString = getReplicaAddress() + "/"
	// + BookStoreMessageTag.EDITORPICKS + "?"
	// + BookStoreConstants.BOOK_NUM_PARAM + "="
	// + urlEncodedNumBooks;
	// exchange.setURL(urlString);
	// result = BookStoreUtility.SendAndRecv(client, exchange);
	// } while (result.getSnapshotId() < getSnapshotId());
	// setSnapshotId(result.getSnapshotId());
	//
	// return (List<Book>) result.getResultList();
	// }

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// @SuppressWarnings("unchecked")
	// public List<Book> getBooks(Set<Integer> isbnSet) {
	//
	// String listISBNsxmlString = BookStoreUtility
	// .serializeObjectToXMLString(isbnSet);
	// Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);
	//
	// BookStoreResult result = null;
	// do {
	// ContentExchange exchange = new ContentExchange();
	// String urlString = getReplicaAddress() + "/"
	// + BookStoreMessageTag.GETBOOKS;
	// exchange.setMethod("POST");
	// exchange.setURL(urlString);
	// exchange.setRequestContent(requestContent);
	// result = BookStoreUtility.SendAndRecv(client, exchange);
	// } while (result.getSnapshotId() < getSnapshotId());
	// setSnapshotId(result.getSnapshotId());
	// return (List<Book>) result.getResultList();
	// }

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

}
