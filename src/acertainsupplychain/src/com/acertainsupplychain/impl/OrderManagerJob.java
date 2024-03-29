package com.acertainsupplychain.impl;

import java.util.List;

import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.NetworkException;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;

/**
 * This class is a worker processing thread that given a workflow ID and a
 * OrderManager parent processes a workflow.
 * 
 * @author Arni
 * 
 */
public class OrderManagerJob implements Runnable {

	private final OrderManager parent;
	private final int workflowID;

	/**
	 * Initializes the worker thread with a given OrderManager parent and a
	 * workflowID.
	 * 
	 * @param parent
	 * @param workflowID
	 */
	public OrderManagerJob(OrderManager parent, int workflowID) {
		this.parent = parent;
		this.workflowID = workflowID;
	}

	@Override
	public void run() {
		// loop until no steps are left:
		List<OrderStep> steps;
		try {
			steps = parent.jobGetWorkflow(workflowID);
		} catch (OrderProcessingException e) {
			e.printStackTrace();
			return;
		}
		int size = steps.size();
		for (int i = 0; i < size; i++) {
			OrderStep orderStep = steps.get(i);
			ItemSupplier supplier = null;
			try {
				supplier = parent.jobGetSupplier(orderStep.getSupplierId());
			} catch (OrderProcessingException e) {
				e.printStackTrace();
				return;
			}
			StepStatus status = executeStep(supplier, orderStep);

			// - update status in db
			try {
				parent.jobSetStatus(workflowID, i, status);
			} catch (OrderProcessingException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * A helper function that performs the actual execution of a step and an
	 * ItemSupplier.
	 * 
	 * @param supplier
	 * @param step
	 * @return
	 */
	private StepStatus executeStep(ItemSupplier supplier, OrderStep step) {
		StepStatus status = StepStatus.SUCCESSFUL;
		try {
			// - wait for responds
			supplier.executeStep(step);
		} catch (NetworkException e) {
			// We assume that the component failed (ie is 'dead' somehow)
			// This is where we would retry the step to adhere to the
			// exactly-once semantics, but this is avoided to ease testing

			// status = executeStep(supplier, step);
			status = StepStatus.FAILED;
		} catch (OrderProcessingException e) {
			status = StepStatus.FAILED;
		} catch (Exception e) {
			// An unexpected exception occurred, mark status as FAILED
			status = StepStatus.FAILED;
		}
		return status;
	}
}
