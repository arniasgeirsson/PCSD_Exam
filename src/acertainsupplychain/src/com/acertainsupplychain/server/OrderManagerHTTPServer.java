/**
 * 
 */
package com.acertainsupplychain.server;

//TODO Inspired by the weekly assignments

/**
 * TODO Starts the master bookstore HTTP server.
 */
public class OrderManagerHTTPServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OrderManagerHTTPMessageHandler handler = new OrderManagerHTTPMessageHandler();
		if (ItemSupplierHTTPServerUtility.createServer(
				Integer.parseInt(args[0]), handler)) {
			;
		}
	}
}
