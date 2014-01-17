package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;

public class OrderManagerImpl implements OrderManager {

	private final Map<Integer, List<OrderStep>> workflows;
	private final Map<Integer, List<StepStatus>> status;
	private int lowestFreeWorkflowID;
	private final Map<Integer, ItemSupplier> suppliers;
	private OrderManagerScheduler scheduler;

	public OrderManagerImpl(Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {
		validateSupplierMap(suppliers);

		workflows = new HashMap<Integer, List<OrderStep>>();
		status = new HashMap<Integer, List<StepStatus>>();
		lowestFreeWorkflowID = 0;
		this.suppliers = suppliers;
		scheduler = new OrderManagerScheduler();
	}

	private void validateSupplierMap(Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {
		if (suppliers == null)
			throw new OrderProcessingException(
					"OrderManager: The given map of suppliers cannot be NULL.");
		if (suppliers.isEmpty())
			throw new OrderProcessingException(
					"OrderManager: The given map of suppliers cannot be"
							+ " empty.");
		for (Integer supplierID : suppliers.keySet()) {
			ItemSupplier supplier = suppliers.get(supplierID);
			if (supplier == null)
				throw new OrderProcessingException(
						"OrderManager: The given map of suppliers cannot "
								+ "contain a NULL ItemSupplier.");
			if (supplierID != supplier.getSupplierID())
				throw new OrderProcessingException(
						"OrderManager: The given map of suppliers contains "
								+ "a mismatch in the supplierIDs.");
		}
	}

	@Override
	public int registerOrderWorkflow(List<OrderStep> steps)
			throws OrderProcessingException {
		if (steps == null)
			throw new InvalidWorkflowException("TODO");
		if (steps.isEmpty())
			throw new InvalidWorkflowException("TODO");
		for (OrderStep orderStep : steps) {
			if (orderStep == null)
				throw new InvalidWorkflowException("TODO");
			if (!suppliers.containsKey(orderStep.getSupplierId()))
				throw new InvalidWorkflowException("TODO");
			// for (ItemQuantity item : orderStep.getItems()) {
			// if (item == null)
			// throw new InvalidWorkflowException("TODO");
			// if (item.getQuantity() < 1)
			// throw new InvalidWorkflowException("TODO");
			// }
		}

		int id = getNextWorkflowID();
		if (workflows.containsKey(id))
			throw new OrderProcessingException("Should not be possible");
		if (status.containsKey(id))
			throw new OrderProcessingException("Should not be possible");

		workflows.put(id, steps);
		status.put(id, initializeStatusList(steps.size()));

		scheduler.scheduleJob(this, id);

		return id;
	}

	// TODO untested
	private List<StepStatus> initializeStatusList(int size)
			throws OrderProcessingException {
		if (size < 1)
			throw new OrderProcessingException("Should not happen..");

		List<StepStatus> list = new ArrayList<StepStatus>();
		for (int i = 0; i < size; i++) {
			list.add(StepStatus.REGISTERED);
		}

		return list;
	}

	// TODO untested
	private int getNextWorkflowID() {
		return lowestFreeWorkflowID++;
	}

	@Override
	public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
			throws InvalidWorkflowException {
		if (!workflows.containsKey(orderWorkflowId))
			throw new InvalidWorkflowException(
					"OrderManager: The given orderWorkflowId does not exist in"
							+ " the database [" + orderWorkflowId + "]");

		if (!status.containsKey(orderWorkflowId))
			throw new InvalidWorkflowException(
					"OrderManager: The given orderWorkflowId does not exist in"
							+ " the database of statusses [" + orderWorkflowId
							+ "] This is not supposed to happend.");

		return status.get(orderWorkflowId);
	}

	// TODO untested
	@Override
	public void clear() {
		workflows.clear();
		status.clear();
		// Must stop any working thread
		scheduler.shutDown();
		scheduler = new OrderManagerScheduler();
	}

	// TODO untested
	@Override
	public ItemSupplier jobGetSupplier(int supplierID) {
		return suppliers.get(supplierID);
	}

	// TODO untested
	@Override
	public List<OrderStep> jobGetWorkFlow(int workflowID) {
		return workflows.get(workflowID);
	}

	// TODO untested
	@Override
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status) {
		List<StepStatus> newStatus = this.status.get(workflowID);
		newStatus.set(stepIndex, status);
		this.status.put(workflowID, newStatus);
	}

	// TODO untested
	@Override
	public void waitForJobsToFinish() throws InterruptedException,
			ExecutionException {
		scheduler.waitForJobsToFinish();
	}

}
