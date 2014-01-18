/**
 * 
 */
package com.acertainsupplychain.utility;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;

import com.acertainsupplychain.NetworkException;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.clients.ItemSupplierClientConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

// TODO is inspired by the weekly assignments

/**
 * BookStoreUtility implements utility methods used by bookstore servers and
 * clients
 * 
 */
public final class ItemSupplierUtility {

	/**
	 * Checks if a string is empty or null
	 * 
	 * @param str
	 * @return
	 */
	// public static boolean isEmpty(String str) {
	// return ((str == null) || str.isEmpty());
	// }

	/**
	 * Converts a string to a float if possible else it returns the signal value
	 * for failure passed as parameter
	 * 
	 * @param str
	 * @param failureSignal
	 * @return
	 */
	// public static float convertStringToFloat(String str, float failureSignal)
	// {
	// float returnValue = failureSignal;
	// try {
	// returnValue = Float.parseFloat(str);
	//
	// } catch (NumberFormatException ex) {
	// ;
	// } catch (NullPointerException ex) {
	// ;
	// }
	// return returnValue;
	// }

	/**
	 * Converts a string to a int if possible else it returns the signal value
	 * for failure passed as parameter
	 * 
	 * @param str
	 * @param failureSignal
	 * @return
	 */
	// public static int convertStringToInt(String str) throws
	// BookStoreException {
	// int returnValue = 0;
	// try {
	// returnValue = Integer.parseInt(str);
	// } catch (Exception ex) {
	// throw new BookStoreException(ex);
	// }
	// return returnValue;
	// }

	/**
	 * TODO Convert a request URI to the message tags supported in
	 * CertainBookStore
	 * 
	 * @param requestURI
	 * @return
	 */
	public static ItemSupplierMessageTag convertURItoMessageTag(
			String requestURI) {

		try {
			ItemSupplierMessageTag messageTag = ItemSupplierMessageTag
					.valueOf(requestURI.substring(1).toUpperCase());
			return messageTag;
		} catch (IllegalArgumentException ex) {
			; // Enum type matching failed so non supported message
		} catch (NullPointerException ex) {
			; // RequestURI was empty
		}
		return null;
	}

	/**
	 * TODO Serializes an object to an xml string
	 * 
	 * @param object
	 * @return
	 */
	public static String serializeObjectToXMLString(Object object) {
		XStream xmlStream = new XStream(new StaxDriver());
		return xmlStream.toXML(object);
	}

	/**
	 * TODO De-serializes an xml string to object
	 * 
	 * @param xmlObject
	 * @return
	 */
	public static Object deserializeXMLStringToObject(String xmlObject) {
		XStream xmlStream = new XStream(new StaxDriver());
		return xmlStream.fromXML(xmlObject);
	}

	/**
	 * TODO fix
	 * 
	 * Manages the sending of an exchange through the client, waits for the
	 * response and unpacks the response
	 * 
	 * @param client
	 * @param exchange
	 * @return
	 * @throws BookStoreException
	 */
	public static ItemSupplierResult sendAndRecv(HttpClient client,
			ContentExchange exchange) throws OrderProcessingException {
		int exchangeState;
		try {
			client.send(exchange);
		} catch (IOException ex) {
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_REQUEST_SENDING,
					ex);
		}

		try {
			exchangeState = exchange.waitForDone(); // block until the response
			// is available
		} catch (InterruptedException ex) {
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_REQUEST_SENDING,
					ex);
		}

		if (exchangeState == HttpExchange.STATUS_COMPLETED) {
			try {
				ItemSupplierResponse itemSupplierResponds = (ItemSupplierResponse) ItemSupplierUtility
						.deserializeXMLStringToObject(exchange
								.getResponseContent().trim());
				OrderProcessingException ex = itemSupplierResponds
						.getException();
				if (ex != null)
					throw ex;
				return itemSupplierResponds.getResult();

			} catch (UnsupportedEncodingException ex) {
				throw new NetworkException(
						ItemSupplierClientConstants.strERR_CLIENT_RESPONSE_DECODING,
						ex);
			}
		} else if (exchangeState == HttpExchange.STATUS_EXCEPTED)
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_REQUEST_EXCEPTION);
		else if (exchangeState == HttpExchange.STATUS_EXPIRED)
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_REQUEST_TIMEOUT);
		else
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_UNKNOWN);
	}

	/**
	 * TODO
	 * 
	 * Returns the message of the request as a string
	 * 
	 * @param request
	 * @return xml string
	 * @throws IOException
	 */
	public static String extractPOSTDataFromRequest(HttpServletRequest request)
			throws IOException {
		Reader reader = request.getReader();
		int len = request.getContentLength();

		// Request must be read into a char[] first
		char res[] = new char[len];
		reader.read(res);
		reader.close();
		return new String(res);
	}

	// http://stackoverflow.com/questions/3263130/processbuilder-start-another-process-jvm-howto
	public static Process startProcess(Class<? extends Object> clazz,
			String... mainArgs) throws Exception {
		System.out.println(clazz.getCanonicalName());
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String path = System.getProperty("java.home") + separator + "bin"
				+ separator + "java";
		List<String> commandList = new ArrayList<String>();
		commandList.add(path);
		commandList.add("-cp");
		commandList.add(classpath);
		commandList.add(clazz.getCanonicalName());
		for (String string : mainArgs) {
			commandList.add(string);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(commandList);
		processBuilder.redirectErrorStream(false);

		Process process = processBuilder.start();
		// Wait a second to make sure that the process is more likely to be
		// ready
		Thread.yield();
		Thread.sleep(1000);
		return process;
	}

	public static void stopProcess(Process process) {
		process.destroy();
	}

	public static String encodeInteger(int integer)
			throws OrderProcessingException {
		return encode(Integer.toString(integer));
	}

	public static int decodeInteger(String string)
			throws OrderProcessingException {
		return Integer.parseInt(decode(string));
	}

	public static String encode(String string) throws OrderProcessingException {
		try {
			return URLEncoder.encode(string, "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new OrderProcessingException(
					"Unsupported encoding exception", e);
		}
	}

	public static String decode(String string) throws OrderProcessingException {
		try {
			return URLDecoder.decode(string, "UTF-8"); // TODO constant?
		} catch (UnsupportedEncodingException e) {
			throw new OrderProcessingException(
					"Unsupported encoding exception", e);
		}
	}
}
