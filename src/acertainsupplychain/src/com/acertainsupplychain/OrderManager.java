package com.acertainsupplychain;

import java.util.List;

/**
 * The OrderManager interface abstracts an integration broker for a supply chain
 * scenario. An order manager allows clients to submit and track order
 * workflows, which are recorded durably.
 */
public interface OrderManager {

	/**
	 * An enumeration listing possible states of an order step. REGISTERED means
	 * the step has been durably accepted by the order manager, but its
	 * processing is still ongoing. FAILED means that an unrecoverable exception
	 * has occurred in the processing of the order step. SUCCESSFUL means that
	 * the order step has been executed against the item supplier.
	 */
	public enum StepStatus {
		REGISTERED, FAILED, SUCCESSFUL
	}

	/**
	 * Registers an order workflow with the order manager.
	 * 
	 * @param steps
	 *            - the order steps to be executed.
	 * @return the ID of the order workflow.
	 * @throws OrderProcessingException
	 *             - an exception thrown if steps are malformed or another error
	 *             condition occurs (you may specialize exceptions deriving from
	 *             OrderProcessingException if you want).
	 */
	public int registerOrderWorkflow(List<OrderStep> steps)
			throws OrderProcessingException;

	/**
	 * Queries the current state of a given order workflow registered with the
	 * order manager.
	 * 
	 * @param orderWorkflowId
	 *            - the ID of the workflow being queried.
	 * @return the list of states of the multiple steps of the given workflow
	 *         (order matters).
	 * @throw InvalidWorkflowException - if the workflow ID given is not valid.
	 */
	public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
			throws InvalidWorkflowException;

	/**
	 * This function is added for the sake of testing. It puts the ItemSupplier
	 * in a same state as if it was reallocated.
	 */
	public void clear();

	/**
	 * This function is added so the thread processing the workflow can fetch
	 * the ItemSupplier associated to that ID. The function returns the
	 * ItemSupplier with the given ID.
	 * 
	 * @param supplierID
	 * @return
	 * @throws OrderProcessingException
	 */
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException;

	/**
	 * This function is added so the thread processing the workflow can fetch
	 * the workflow associated with that ID. The function returns the workflow
	 * with the given ID.
	 * 
	 * @param workflowID
	 * @return
	 * @throws OrderProcessingException
	 */
	public List<OrderStep> jobGetWorkflow(int workflowID)
			throws OrderProcessingException;

	/**
	 * This function is used by the workflow processing threads to update the
	 * status of a processed step. The function marks a specific step to be in
	 * the given StepStatus.
	 * 
	 * @param workflowID
	 * @param stepIndex
	 * @param status
	 * @throws OrderProcessingException
	 */
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException;

	/**
	 * This function is added for the sake of testing. This function blocks the
	 * OrderManager until every workflow as been properly processed.
	 * 
	 * @throws OrderProcessingException
	 */
	public void waitForJobsToFinish() throws OrderProcessingException;

	/**
	 * This functions stops any ItemSupplier proxies this OrderManager might
	 * control.
	 */
	public void stopItemSupplierProxies();
}
