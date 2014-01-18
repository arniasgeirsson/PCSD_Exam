/**
 * 
 */
package com.acertainsupplychain.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.clients.ItemSupplierClientConstants;
import com.acertainsupplychain.clients.ItemSupplierHTTPProxy;
import com.acertainsupplychain.impl.OrderManagerImpl;
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
public class OrderManagerHTTPMessageHandler extends AbstractHandler {

	private OrderManager orderManager;

	public OrderManagerHTTPMessageHandler() {
		orderManager = null;
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
		String workflowIDString;
		int workflowID;
		String orderManagerIDString;
		int orderManagerID;

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

			case REGISTERWORKFLOW:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);

				List<OrderStep> steps = (List<OrderStep>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);
				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.registerOrderWorkflow(steps)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case GETWORKFLOWSTATUS:
				workflowIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.GETWORKFLOWSTATUS_PARAM),
								"UTF-8");

				workflowID = Integer.parseInt(workflowIDString);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.getOrderWorkflowStatus(workflowID)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case CLEAR:
				orderManager.clear();
				itemSupplierResponse = new ItemSupplierResponse();
				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case INIT_ORDERMANAGER:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
				Map<Integer, ItemSupplier> suppliers = (Map<Integer, ItemSupplier>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);
				itemSupplierResponse = new ItemSupplierResponse();

				orderManagerIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.INIT_ORDERMANAGER_ID),
								"UTF-8");

				orderManagerID = Integer.parseInt(orderManagerIDString);

				try {
					if (orderManager == null) {
						orderManager = new OrderManagerImpl(orderManagerID,
								suppliers);
					}
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case INIT_ORDERMANAGER_PROXY:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
				Map<Integer, Integer> suppliers2 = (Map<Integer, Integer>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);
				itemSupplierResponse = new ItemSupplierResponse();

				orderManagerIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.INIT_ORDERMANAGER_ID),
								"UTF-8");

				orderManagerID = Integer.parseInt(orderManagerIDString);

				Map<Integer, ItemSupplier> supplierProxies = new HashMap<Integer, ItemSupplier>();

				try {
					if (orderManager == null) {
						for (Integer itemSupplierID : suppliers2.keySet()) {
							supplierProxies.put(itemSupplierID,
									new ItemSupplierHTTPProxy(itemSupplierID,
											suppliers2.get(itemSupplierID)));
						}
						orderManager = new OrderManagerImpl(orderManagerID,
								supplierProxies);
					}
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(e));
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case JOBGETSUPID:
				String supplierIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.JOBGETSUPPLIER_PARAM),
								"UTF-8");
				int supplierID = Integer.parseInt(supplierIDString);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.jobGetSupplier(supplierID)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;
			case JOBGETWORKFLOW:
				workflowIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.JOBGETWORKFLOW_PARAM),
								"UTF-8");
				workflowID = Integer.parseInt(workflowIDString);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.jobGetWorkflow(workflowID)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;
			case JOBSETSTATUS:
				workflowIDString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.JOBSETSTATUS_PARAM_WID),
								"UTF-8");
				workflowID = Integer.parseInt(workflowIDString);

				String stepIndexString = URLDecoder
						.decode(request
								.getParameter(ItemSupplierClientConstants.JOBSETSTATUS_PARAM_STEPINDEX),
								"UTF-8");
				int stepIndex = Integer.parseInt(stepIndexString);

				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
				StepStatus status = (StepStatus) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					orderManager.jobSetStatus(workflowID, stepIndex, status);
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				}

				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;
			case WAITFORJOBS:
				itemSupplierResponse = new ItemSupplierResponse();

				try {
					orderManager.waitForJobsToFinish();
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				}
				response.getWriter()
						.println(
								ItemSupplierUtility
										.serializeObjectToXMLString(itemSupplierResponse));
				break;

			case ORDERMANAGER_STOP:
				orderManager.stopItemSupplierProxies();
				itemSupplierResponse = new ItemSupplierResponse();
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

	private Map<Integer, ItemSupplier> createSupplierMap(
			List<ItemSupplier> suppliers) {
		Map<Integer, ItemSupplier> supplierMap = new HashMap<Integer, ItemSupplier>();
		for (ItemSupplier itemSupplier : suppliers) {
			supplierMap.put(itemSupplier.getSupplierID(), itemSupplier);
		}
		return supplierMap;
	}
}
