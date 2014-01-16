/**
 * 
 */
package com.acertainsupplychain.server;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.impl.ItemSupplierImpl;
import com.acertainsupplychain.utility.ItemSupplierMessageTag;
import com.acertainsupplychain.utility.ItemSupplierResponse;
import com.acertainsupplychain.utility.ItemSupplierResult;
import com.acertainsupplychain.utility.ItemSupplierUtility;

//TODO Inspired by the course assignments

/**
 * 
 * MasterBookStoreHTTPMessageHandler implements the message handler class which
 * is invoked to handle messages received by the master book store HTTP server
 * It decodes the HTTP message and invokes the MasterCertainBookStore API
 * 
 */
public class ItemSupplierHTTPMessageHandler extends AbstractHandler {

	private final ItemSupplier supplier;

	public ItemSupplierHTTPMessageHandler() {
		supplier = new ItemSupplierImpl(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		ItemSupplierMessageTag messageTag;
		String requestURI;

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		requestURI = request.getRequestURI();

		String xml;
		ItemSupplierResponse itemSupplierResponse;
		OrderStep step;
		Set<Integer> itemIds;

		// // Need to do request multi-plexing
		// if (!ItemSupplierUtility.isEmpty(requestURI)
		// && requestURI.toLowerCase().startsWith("/stock")) {
		// messageTag = ItemSupplierUtility.convertURItoMessageTag(requestURI
		// .substring(6)); // the request is from store
		// // manager, more
		// // sophisticated security
		// // features could be added
		// // here
		// } else {
		messageTag = ItemSupplierUtility.convertURItoMessageTag(requestURI);
		// }
		// the RequestURI before the switch
		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {
			switch (messageTag) {

			case EXECUTESTEP:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);

				step = (OrderStep) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					supplier.executeStep(step);
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case GETORDERS:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);

				itemIds = (Set<Integer>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							supplier.getOrdersPerItem(itemIds)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case CLEAR:
				supplier.clear();
				itemSupplierResponse = new ItemSupplierResponse();
				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case GETSUPID:
				itemSupplierResponse = new ItemSupplierResponse();

				itemSupplierResponse.setResult(new ItemSupplierResult(supplier
						.getSupplierID()));

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			// case BUYBOOKS:
			// xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
			// Set<BookCopy> bookCopiesToBuy = (Set<BookCopy>)
			// ItemSupplierUtility
			// .deserializeXMLStringToObject(xml);
			//
			// // Make the purchase
			// bookStoreresponse = new ItemSupplierResponse();
			// try {
			// bookStoreresponse.setResult(MasterCertainBookStore
			// .getInstance().buyBooks(bookCopiesToBuy));
			// } catch (BookStoreException ex) {
			// bookStoreresponse.setException(ex);
			// }
			// response.getWriter().println(
			// ItemSupplierUtility
			// .serializeObjectToXMLString(bookStoreresponse));
			// break;
			//
			// case GETBOOKS:
			// xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
			// Set<Integer> isbnSet = (Set<Integer>) ItemSupplierUtility
			// .deserializeXMLStringToObject(xml);
			//
			// bookStoreresponse = new ItemSupplierResponse();
			// try {
			// bookStoreresponse.setResult(MasterCertainBookStore
			// .getInstance().getBooks(isbnSet));
			// } catch (BookStoreException ex) {
			// bookStoreresponse.setException(ex);
			// }
			// response.getWriter().println(
			// ItemSupplierUtility
			// .serializeObjectToXMLString(bookStoreresponse));
			// break;
			//
			// case EDITORPICKS:
			// numBooksString = URLDecoder
			// .decode(request
			// .getParameter(BookStoreConstants.BOOK_NUM_PARAM),
			// "UTF-8");
			// bookStoreresponse = new ItemSupplierResponse();
			// try {
			// numBooks = ItemSupplierUtility
			// .convertStringToInt(numBooksString);
			// bookStoreresponse.setResult(MasterCertainBookStore
			// .getInstance().getEditorPicks(numBooks));
			// } catch (BookStoreException ex) {
			// bookStoreresponse.setException(ex);
			// }
			// response.getWriter().println(
			// ItemSupplierUtility
			// .serializeObjectToXMLString(bookStoreresponse));
			// break;

			default:
				System.out.println("Unhandled message tag");
				break;
			}
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);

	}
}
