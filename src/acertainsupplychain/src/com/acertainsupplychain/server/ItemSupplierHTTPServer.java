/**
 * 
 */
package com.acertainsupplychain.server;

//TODO Inspired by the weekly assignments

/**
 * TODO Starts the master bookstore HTTP server.
 */
public class ItemSupplierHTTPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ItemSupplierHTTPMessageHandler handler = new ItemSupplierHTTPMessageHandler();
		if (ItemSupplierHTTPServerUtility.createServer(
				Integer.parseInt(args[0]), handler)) {
			;
		}
	}

}
