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
import com.acertainsupplychain.clients.ItemSupplierClientConstants;
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

	private ItemSupplier supplier;

	public ItemSupplierHTTPMessageHandler() {
		supplier = null;
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
		Integer supplierID;

		messageTag = ItemSupplierUtility.convertURItoMessageTag(requestURI);

		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {
			switch (messageTag) {

			case INIT_ITEMSUPPLIER:
				itemSupplierResponse = new ItemSupplierResponse();

				try {
					supplierID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.INIT_ITEMSUPPLIER_PARAM));

					if (supplier == null) {
						supplier = new ItemSupplierImpl(supplierID);
					}

				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

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

			default:
				System.out.println("Unhandled message tag");
				break;
			}
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);

	}
}
