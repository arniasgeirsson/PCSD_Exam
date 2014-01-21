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

/**
 * This is an implementation of the OrderManager interface.
 * 
 * @author Arni
 * 
 */
public class OrderManagerImpl implements OrderManager {

	private final Map<Integer, List<OrderStep>> workflows;
	private final Map<Integer, List<StepStatus>> status;
	private int nextWorkflowID;
	private final Map<Integer, ItemSupplier> suppliers;
	private OrderManagerScheduler scheduler;
	private final FileLogger fileLogger;
	private final int orderManagerID;

	private final ReadWriteLock workflowIDLock;
	private final LockMapManager<Integer> lockManager;

	/**
	 * Initialize the OrderManager with a given OrderManager ID and map of
	 * ItemSupplier instances, mapped to their respected ItemSupplier ID.
	 * 
	 * @param orderManagerID
	 * @param suppliers
	 * @throws OrderProcessingException
	 */
	public OrderManagerImpl(int orderManagerID,
			Map<Integer, ItemSupplier> suppliers)
			throws OrderProcessingException {

		// Validate the map of suppliers, to ensure that it is okay.
		validateSupplierMap(suppliers);
		this.orderManagerID = orderManagerID;
		this.suppliers = suppliers;
		nextWorkflowID = 0;
		workflows = new ConcurrentHashMap<Integer, List<OrderStep>>();
		status = new ConcurrentHashMap<Integer, List<StepStatus>>();
		scheduler = new OrderManagerScheduler();
		workflowIDLock = new ReentrantReadWriteLock();
		lockManager = new LockMapManager<Integer>();

		fileLogger = new FileLogger(this.orderManagerID
				+ "_OrderManager_logfile", "How to read this log file?%n");
		fileLogger.logToFile("INITOM " + orderManagerID + System.getProperty("line.separator"), true);
	}

	/**
	 * Validates a map of ItemSuppliers by throwing an OrderProcessingException
	 * if the map is invalid.
	 * 
	 * @param suppliers
	 * @throws OrderProcessingException
	 */
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

		// Must validate the given steps before processing them.
		validateWorkflow(steps);

		int id = getNextWorkflowID();
		// Sanity checks.
		if (workflows.containsKey(id))
			throw new OrderProcessingException("Should not be possible");
		if (status.containsKey(id))
			throw new OrderProcessingException("Should not be possible");

		// As the workflowIDLock makes sure that no two (or more) threads
		// can get the same workflowID, then it does not matter if the next five
		// lines are interleaved, as it will not conflict with any entry in the
		// maps, nor will it break all-or-nothing atomicity.

		lockManager.addToLockMap(id);
		workflows.put(id, steps);
		status.put(id, initializeStatusList(steps.size()));
		logWorkflow(id, steps);
		scheduler.scheduleJob(this, id);

		return id;
	}

	/**
	 * This function validates a given workflow, i.e. a list of OrderSteps by
	 * throwing a OrderProcessingException if the list is not valid.
	 * 
	 * @param steps
	 *            , the steps to validate.
	 * @throws OrderProcessingException
	 */
	private void validateWorkflow(List<OrderStep> steps)
			throws OrderProcessingException {
		if (steps == null)
			throw new InvalidWorkflowException(
					"The given workflow cannot be null.");
		if (steps.isEmpty())
			throw new InvalidWorkflowException(
					"The given workflow is not allowed to be empty.");
		for (OrderStep orderStep : steps) {
			if (orderStep == null)
				throw new InvalidWorkflowException(
						"The given workflow cannot contain a NULL step.");
			if (!suppliers.containsKey(orderStep.getSupplierId()))
				throw new InvalidWorkflowException(
						"The given workflow cannot a step that is intended for"
								+ " ItemSupplier whom this OrderManager knows"
								+ " nothing about.");
			// Note that the below validations has been commented out to allow
			// for any other then SUCCESS steps when trying to execute validated
			// steps with an ItemSupplier.
			// I also assume that the OrderManager should not have this kind of
			// full control knowledge, as it simple just forwards and processes
			// workflows and not determines what an ItemSupplier wants to accept
			// or not.

			// for (ItemQuantity item : orderStep.getItems()) {
			// if (item == null)
			// throw new
			// InvalidWorkflowException("The given workflow cannot contain a step which has a NULL item.");
			// if (item.getQuantity() < 1)
			// throw new
			// InvalidWorkflowException("The given workflow cannot contain a step which has an item with a non-positive quantity.");
			// }
		}
	}

	/**
	 * Logs a workflow to the log file.
	 * 
	 * @param workflowID
	 * @param steps
	 */
	private void logWorkflow(int workflowID, List<OrderStep> steps) {
		String log = "REGISTER " + workflowID + " ";

		log = log + createWorkflowString(steps) + System.getProperty("line.separator");

		fileLogger.logToFile(log, true);
	}

	/**
	 * This function creates a string representation of a workflow to use when
	 * logging.
	 * 
	 * @param steps
	 * @return
	 */
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

	/**
	 * This function create a string representation of a single OrderStep. Is
	 * used during logging.
	 * 
	 * @param step
	 * @return
	 */
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

	/**
	 * This function logs a status update to the log file.
	 * 
	 * @param workflowID
	 * @param stepIndex
	 * @param status
	 */
	private void logStatusUpdate(int workflowID, int stepIndex,
			StepStatus status) {
		String log = "UPDATE ";

		if (status == null) {
			log = log + workflowID + " " + stepIndex + " (null)"
					+ System.getProperty("line.separator");
		} else {
			log = log + workflowID + " " + stepIndex + " " + status 
					+ System.getProperty("line.separator");
		}

		fileLogger.logToFile(log, true);
	}

	/**
	 * This function is a helper function to create a list of stepStatus with
	 * the status REGISTERED. If the given size is non-positive then an
	 * OrderProcessingException is thrown.
	 * 
	 * @param size
	 *            , the size of the list
	 * @return
	 * @throws OrderProcessingException
	 */
	private List<StepStatus> initializeStatusList(int size)
			throws OrderProcessingException {
		if (size < 1)
			throw new OrderProcessingException(
					"Cannot create a list with a non-positive size.");

		List<StepStatus> list = new ArrayList<StepStatus>();
		for (int i = 0; i < size; i++) {
			list.add(StepStatus.REGISTERED);
		}

		return list;
	}

	/**
	 * Returns the next workflow ID to be used. Locking is used to ensure that
	 * function is not interleaved by any other thread.
	 * 
	 * @return a unique workflow ID.
	 */
	private int getNextWorkflowID() {
		workflowIDLock.writeLock().lock();
		int nextID = nextWorkflowID;
		nextWorkflowID++;
		workflowIDLock.writeLock().unlock();
		return nextID;
	}

	@Override
	public List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId)
			throws InvalidWorkflowException {

		// Validate the workflow ID before trying to use it.
		validateOrderWorkflowID(orderWorkflowId);
		lockManager.acquireReadLock(orderWorkflowId);
		List<StepStatus> statuses = status.get(orderWorkflowId);
		lockManager.releaseReadLock(orderWorkflowId);

		return statuses;
	}

	/**
	 * This function validates a given workflow ID to ensure that it exist and
	 * can be used by throwing a InvalidWorkflowException if the ID is invalid.
	 * 
	 * @param orderWorkflowId
	 *            , the ID to validate.
	 * @throws InvalidWorkflowException
	 */
	private void validateOrderWorkflowID(int orderWorkflowId)
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
	}

	@Override
	public void clear() {
		nextWorkflowID = 0;
		workflows.clear();
		status.clear();
		// Must stop any working thread
		scheduler.shutDown();
		scheduler = new OrderManagerScheduler();
		fileLogger.logToFile("CLEARDONE"
				+ System.getProperty("line.separator"), true);
	}

	@Override
	public ItemSupplier jobGetSupplier(int supplierID)
			throws OrderProcessingException {
		if (!suppliers.containsKey(supplierID))
			throw new OrderProcessingException(
					"The provided supplier ID is unknown to this OrderManager ["
							+ orderManagerID + "]");
		return suppliers.get(supplierID);
	}

	@Override
	public List<OrderStep> jobGetWorkflow(int workflowID)
			throws OrderProcessingException {
		validateOrderWorkflowID(workflowID);
		return workflows.get(workflowID);
	}

	// This function assumes that the given stepIndex is valid.
	@Override
	public void jobSetStatus(int workflowID, int stepIndex, StepStatus status)
			throws OrderProcessingException {
		validateOrderWorkflowID(workflowID);

		lockManager.acquireWriteLock(workflowID);
		List<StepStatus> newStatus = this.status.get(workflowID);
		newStatus.set(stepIndex, status);
		this.status.put(workflowID, newStatus);
		lockManager.releaseWriteLock(workflowID);
		logStatusUpdate(workflowID, stepIndex, status);
	}

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
