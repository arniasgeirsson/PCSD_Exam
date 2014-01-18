package com.acertainsupplychain.impl;

import java.util.List;

import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;

public class OrderManagerJob implements Runnable {

	private final OrderManager parent;
	private final int workflowID;

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
			// - fetch next step
			// - call the supplier
			ItemSupplier supplier = null;
			try {
				supplier = parent.jobGetSupplier(orderStep.getSupplierId());
			} catch (OrderProcessingException e) {
				e.printStackTrace();
				return;
			}
			StepStatus status = StepStatus.SUCCESSFUL;
			try {
				// - wait for responds
				supplier.executeStep(orderStep);
			} catch (OrderProcessingException e) {
				status = StepStatus.FAILED;
			}

			// - update status in db
			try {
				parent.jobSetStatus(workflowID, i, status);
			} catch (OrderProcessingException e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
