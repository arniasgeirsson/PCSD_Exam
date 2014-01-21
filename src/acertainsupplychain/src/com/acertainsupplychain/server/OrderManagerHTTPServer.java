package com.acertainsupplychain.server;

/**
 * Class to start an OrderManager server with an OrderManagerHTTPMessageHandler,
 * the server reads its destined port from the main function arguments.
 * 
 * @author Arni
 * 
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
