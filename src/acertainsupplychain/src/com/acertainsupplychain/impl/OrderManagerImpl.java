package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.clients.ItemSupplierHTTPProxy;
import com.acertainsupplychain.utility.FileLogger;
import com.acertainsupplychain.utility.LockMapManager;

public class OrderManagerImpl implements OrderManager {

	private final Map<Integer, List<OrderStep>> workflows;
	private final Map<Integer, List<StepStatus>> status;
	private int lowestFreeWorkflowID;
	private final Map<Integer, ItemSupplier> suppliers;
	private OrderManagerScheduler scheduler;
	private final FileLogger fileLogger;
	private final int orderManagerID;

	private final ReadWriteLock workflowIDLock;
	private final LockMapManager<Integer> lockManager;

	public OrderManagerImpl(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {
		validateSupplierMap(suppliers);
		this.orderManagerID = orderManagerID;

		// TODO do I really need this? -> means I must pass the workflow along
		// -> I need it, to enable flexibility in the design. hm really?
		workflows = new ConcurrentHashMap<Integer, List<OrderStep>>();

		status = new ConcurrentHashMap<Integer, List<StepStatus>>();
		lowestFreeWorkflowID = 0;
		this.suppliers = suppliers;
		scheduler = new OrderManagerScheduler();

		workflowIDLock = new ReentrantReadWriteLock();
		lockManager = new LockMapManager<Integer>();

		fileLogger = new FileLogger(this.orderManagerID
				+ "_OrderManager_logfile", "How to read this log file?\n");
		fileLogger.logToFile("INITOM " + orderManagerID + "\n", true);
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

		// As the workflowIDLock makes sure that only no two (or more) threads
		// can get the same workflowID then it does not matter if the next five
		// lines are interleaved, as it will not conflict with any entry in the
		// maps.

		lockManager.addToLockMap(id);
		workflows.put(id, steps);
		status.put(id, initializeStatusList(steps.size()));
		logWorkflow(id, steps);
		scheduler.scheduleJob(this, id);

		return id;
	}

	private void logWorkflow(int workflowID, List<OrderStep> steps) {
		String log = "REGISTER " + workflowID + " ";

		log = log + createWorkflowString(steps) + "\n";

		fileLogger.logToFile(log, true);
	}

	private String createWorkflowString(List<OrderStep> steps) {
		if (steps == null)
			return "(null)";
		String string = "";

		for (OrderStep orderStep : steps) {
			string = string + createStepString(orderStep) + " ";
		}

		if (string.endsWith(",")) {
			string = string.substring(0, string.length() - 1);
		}

		return string;
	}

	private String createStepString(OrderStep step) {
		if (step == null)
			return "(null)";
		String string = "[" + step.getSupplierId() + ",";

		for (ItemQuantity itemQuantity : step.getItems()) {
			if (itemQuantity == null) {
				string = string + "(null)";
			} else {
				string = string + "(" + itemQuantity.getItemId() + ","
						+ itemQuantity.getQuantity() + "),";
			}
		}

		if (string.endsWith(",")) {
			string = string.substring(0, string.length() - 1);
		}

		string = string + "]";
		return string;
	}

	private void logStatusUpdate(int workflowID, int stepIndex,
			StepStatus status) {
		String log = "UPDATE ";

		if (status == null) {
			log = log + workflowID + " " + stepIndex + " (null)\n";
		} else {
			log = log + workflowID + " " + stepIndex + " " + status + "\n";
		}

		fileLogger.logToFile(log, true);
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
		workflowIDLock.writeLock().lock();
		int nextID = lowestFreeWorkflowID;
		lowestFreeWorkflowID++;
		workflowIDLock.writeLock().unlock();
		return nextID;
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

		lockManager.acquireReadLock(orderWorkflowId);
		List<StepStatus> statuses = status.get(orderWorkflowId);
		lockManager.releaseReadLock(orderWorkflowId);

		return statuses;
	}

	// TODO untested
	@Override
	public void clear() {
		workflows.clear();
		status.clear();
		// Must stop any working thread
		scheduler.shutDown();
		scheduler = new OrderManagerScheduler();
		fileLogger.logToFile("CLEARDONE\n", true);
	}

	// TODO untested
	@Override
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException {
		if (!suppliers.containsKey(supplierID))
			throw new OrderProcessingException("TODO");
		return suppliers.get(supplierID);
	}

	// TODO untested
	@Override
	public List<OrderStep> jobGetWorkflow(int workflowID)
			throws OrderProcessingException {
		if (!workflows.containsKey(workflowID))
			throw new OrderProcessingException("TODO");
		return workflows.get(workflowID);
	}

	// TODO untested
	@Override
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException {
		lockManager.acquireWriteLock(workflowID);
		List<StepStatus> newStatus = this.status.get(workflowID);
		newStatus.set(stepIndex, status);
		this.status.put(workflowID, newStatus);
		lockManager.releaseWriteLock(workflowID);
		logStatusUpdate(workflowID, stepIndex, status);
	}

	// TODO untested
	@Override
	public void waitForJobsToFinish() throws OrderProcessingException {
		try {
			scheduler.waitForJobsToFinish();
		} catch (InterruptedException e) {
			throw new OrderProcessingException(e);
		} catch (ExecutionException e) {
			throw new OrderProcessingException(e);
		}
	}

	@Override
	public void stopItemSupplierProxies() {
		for (ItemSupplier supplier : suppliers.values()) {
			if (supplier instanceof ItemSupplierHTTPProxy) {
				((ItemSupplierHTTPProxy) supplier).stop();
			}
		}
	}

}
