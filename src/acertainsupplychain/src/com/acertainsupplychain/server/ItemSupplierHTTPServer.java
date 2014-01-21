package com.acertainsupplychain.server;

/**
 * Class to start an ItemSupplier server with an ItemSupplierHTTPMessageHandler,
 * the server reads its destined port from the main function arguments.
 * 
 * @author Arni
 * 
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
