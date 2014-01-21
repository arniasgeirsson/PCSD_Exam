package com.acertainsupplychain.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainsupplychain.NetworkException;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.clients.ItemSupplierClientConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * This is a utility class used by the message handler and proxy classes.
 * 
 * @author Arni
 * 
 */
public final class ItemSupplierUtility {

	public static HttpClient setupNewHttpClient() {
		HttpClient client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(ItemSupplierClientConstants.CLIENT_MAX_CONNECTION_ADDRESS);
		client.setThreadPool(new QueuedThreadPool(
				ItemSupplierClientConstants.CLIENT_MAX_THREADSPOOL_THREADS));
		client.setTimeout(ItemSupplierClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS);
		return client;
	}

	/**
	 * Note this function is taken from one of the course assignments.
	 * 
	 * Convert a request URI to the message tags supported
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
	 * Serializes an object to a xml string.
	 * 
	 * @param object
	 * @return
	 */
	public static String serializeObjectToXMLString(Object object) {
		XStream xmlStream = new XStream(new StaxDriver());
		return xmlStream.toXML(object);
	}

	/**
	 * De-serializes a xml string to an object.
	 * 
	 * @param xmlObject
	 * @return
	 */
	public static Object deserializeXMLStringToObject(String xmlObject) {
		XStream xmlStream = new XStream(new StaxDriver());
		return xmlStream.fromXML(xmlObject);
	}

	/**
	 * Note this function is, almost fully, taken from one of the course
	 * assignments.
	 * 
	 * Manages the sending of an exchange through the client, waits for the
	 * response and unpacks the response
	 * 
	 * @param client
	 * @param exchange
	 * @return
	 * @throws OrderProcessingException
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
		} catch (Exception e) {
			throw new NetworkException("Caught unexpected exception", e);
		}

		try {
			exchangeState = exchange.waitForDone(); // block until the response
			// is available
		} catch (InterruptedException ex) {
			throw new NetworkException(
					ItemSupplierClientConstants.strERR_CLIENT_REQUEST_SENDING,
					ex);
		} catch (Exception e) {
			throw new NetworkException("Caught unexpected exception", e);
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
			} catch (Exception e) {
				throw new NetworkException("Caught unexpected exception", e);
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
	 * Note this function is taken from one of the course assignments.
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

	/**
	 * This function starts a new process based on the given class.
	 * 
	 * @param clazz
	 *            , the class which must be started in another JVM.
	 * @param redirectStreams
	 *            , whether or not the output and error streams of the created
	 *            process should be redirected to the callers output stream.
	 * @param mainArgs
	 *            , the main arguments that must be provided to the main
	 *            function.
	 * @return
	 * @throws Exception
	 */
	public static Process startProcess(Class<? extends Object> clazz,
			boolean redirectStreams, String... mainArgs) throws Exception {
		// Build the command string
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

		// Start the new process
		ProcessBuilder processBuilder = new ProcessBuilder(commandList);
		Process process = processBuilder.start();

		// Redirect the stream if needed
		if (redirectStreams) {
			StreamGobbler errorGobbler = new StreamGobbler(
					process.getErrorStream(), "ERROR");

			StreamGobbler outputGobbler = new StreamGobbler(
					process.getInputStream(), "OUTPUT");

			errorGobbler.start();
			outputGobbler.start();
		}

		// Wait a second to make sure that the process is more likely to be
		// ready
		// Note the reason I do this is that sometimes the created Jetty server,
		// which is what I use this function for, is not ready when trying to
		// use it, therefore I added a yield and sleep to make it more plausible
		// that the server is ready for use after this function has returned.
		Thread.yield();
		Thread.sleep(1500);
		return process;
	}

	/**
	 * This function stops a started process.
	 * 
	 * @param process
	 *            , the process to stop.
	 */
	public static void stopProcess(Process process) {
		if (process != null) {
			process.destroy();
		}
	}

	/**
	 * A wrapper function to encode an Integer.
	 * 
	 * @param integer
	 *            , the Integer to encode.
	 * @return the encoded String.
	 * @throws OrderProcessingException
	 */
	public static String encodeInteger(int integer)
			throws OrderProcessingException {
		try {
			return encode(Integer.toString(integer));
		} catch (Exception e) {
			throw new OrderProcessingException(e);
		}
	}

	/**
	 * A wrapper function to decode a String into an Integer.
	 * 
	 * @param string
	 *            , the string to decode.
	 * @return the Integer parsed from the decoded String.
	 * @throws OrderProcessingException
	 */
	public static int decodeInteger(String string)
			throws OrderProcessingException {
		try {
			return Integer.parseInt(decode(string));
		} catch (Exception e) {
			throw new OrderProcessingException(e);
		}
	}

	/**
	 * A wrapper function to encode a given String.
	 * 
	 * @param string
	 *            , the String to encode.
	 * @return the encoded String.
	 * @throws OrderProcessingException
	 */
	public static String encode(String string) throws OrderProcessingException {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new OrderProcessingException(
					"Unsupported encoding exception", e);
		}
	}

	/**
	 * A wrapper function to decode a String.
	 * 
	 * @param string
	 *            , the String to decode.
	 * @return the decoded String.
	 * @throws OrderProcessingException
	 */
	public static String decode(String string) throws OrderProcessingException {
		try {
			return URLDecoder.decode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new OrderProcessingException(
					"Unsupported encoding exception", e);
		}
	}

	/**
	 * Taken from here
	 * http://stackoverflow.com/questions/14165517/processbuilder
	 * -forwarding-stdout-and-stderr-of-started-processes-without-blocki
	 * 
	 */
	private static class StreamGobbler extends Thread {
		InputStream is;
		String type;

		private StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(type + "> " + line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
