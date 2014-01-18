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

	// TODO for the sake of testing
	public void clear();

	// TODO, can be passed along to lessen the interaction with the orderManager
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException;

	public List<OrderStep> jobGetWorkflow(int workflowID)
			throws OrderProcessingException;

	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException;

	// TODO how to test it? Do random tests and never will the state ever be
	// Registered after this call?
	public void waitForJobsToFinish() throws OrderProcessingException;

	public void stopItemSupplierProxies();
}
