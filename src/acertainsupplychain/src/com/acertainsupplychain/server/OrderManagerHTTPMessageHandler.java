/**
 * 
 */
package com.acertainsupplychain.server;

import java.io.IOException;
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
		Integer workflowID;
		Integer orderManagerID;
		List<OrderStep> steps;
		Map<Integer, ItemSupplier> supplierObjectMap;
		Map<Integer, Integer> suppliersPortMap;
		Map<Integer, ItemSupplier> supplierProxies;
		Integer supplierID;
		StepStatus status;
		Integer stepIndex;

		messageTag = ItemSupplierUtility.convertURItoMessageTag(requestURI);

		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {
			switch (messageTag) {

			case REGISTERWORKFLOW:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);

				steps = (List<OrderStep>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);
				itemSupplierResponse = new ItemSupplierResponse();
				try {
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.registerOrderWorkflow(steps)));
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case GETWORKFLOWSTATUS:
				itemSupplierResponse = new ItemSupplierResponse();
				try {
					workflowID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.GETWORKFLOWSTATUS_PARAM));

					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.getOrderWorkflowStatus(workflowID)));
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case CLEAR:
				orderManager.clear();
				writeResponse(response, new ItemSupplierResponse());
				break;

			case INIT_ORDERMANAGER:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
				supplierObjectMap = (Map<Integer, ItemSupplier>) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);

				itemSupplierResponse = new ItemSupplierResponse();

				try {
					orderManagerID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.INIT_ORDERMANAGER_ID));
					if (orderManager == null) {
						orderManager = new OrderManagerImpl(orderManagerID,
								supplierObjectMap);
					}
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case INIT_ORDERMANAGER_PROXY:
				itemSupplierResponse = new ItemSupplierResponse();

				supplierProxies = new HashMap<Integer, ItemSupplier>();

				try {
					xml = ItemSupplierUtility
							.extractPOSTDataFromRequest(request);
					suppliersPortMap = (Map<Integer, Integer>) ItemSupplierUtility
							.deserializeXMLStringToObject(xml);

					orderManagerID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.INIT_ORDERMANAGER_ID));
					if (orderManager == null) {
						for (Integer itemSupplierID : suppliersPortMap.keySet()) {
							supplierProxies.put(
									itemSupplierID,
									new ItemSupplierHTTPProxy(itemSupplierID,
											suppliersPortMap
													.get(itemSupplierID)));
						}
						orderManager = new OrderManagerImpl(orderManagerID,
								supplierProxies);
					}
				} catch (OrderProcessingException e) {
					e.printStackTrace();
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					e.printStackTrace();
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case JOBGETSUPID:
				itemSupplierResponse = new ItemSupplierResponse();
				try {
					supplierID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.JOBGETSUPPLIER_PARAM));
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.jobGetSupplier(supplierID)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;
			case JOBGETWORKFLOW:
				itemSupplierResponse = new ItemSupplierResponse();
				try {
					workflowID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.JOBGETWORKFLOW_PARAM));
					itemSupplierResponse.setResult(new ItemSupplierResult(
							orderManager.jobGetWorkflow(workflowID)));
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case JOBSETSTATUS:
				xml = ItemSupplierUtility.extractPOSTDataFromRequest(request);
				status = (StepStatus) ItemSupplierUtility
						.deserializeXMLStringToObject(xml);

				itemSupplierResponse = new ItemSupplierResponse();
				try {
					workflowID = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.JOBSETSTATUS_PARAM_WID));
					stepIndex = ItemSupplierUtility
							.decodeInteger(request
									.getParameter(ItemSupplierClientConstants.JOBSETSTATUS_PARAM_STEPINDEX));

					orderManager.jobSetStatus(workflowID, stepIndex, status);
				} catch (OrderProcessingException ex) {
					itemSupplierResponse.setException(ex);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);

				break;

			case WAITFORJOBS:
				itemSupplierResponse = new ItemSupplierResponse();

				try {
					orderManager.waitForJobsToFinish();
				} catch (OrderProcessingException e) {
					itemSupplierResponse.setException(e);
				} catch (Exception e) {
					itemSupplierResponse
							.setException(new OrderProcessingException(
									"Caught unexpected exception", e));
				}

				writeResponse(response, itemSupplierResponse);
				break;

			case ORDERMANAGER_STOP:
				orderManager.stopItemSupplierProxies();
				writeResponse(response, new ItemSupplierResponse());
				break;

			default:
				System.out.println("Unhandled message tag");
				break;
			}
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);

	}

	// TODO safe to use Object type instead of ItemSupplierResponse ?
	private void writeResponse(HttpServletResponse response,
			Object responseObject) throws IOException {
		response.getWriter().println(
				ItemSupplierUtility.serializeObjectToXMLString(responseObject));
	}

}
