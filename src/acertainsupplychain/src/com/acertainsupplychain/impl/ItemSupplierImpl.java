package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utility.FileLogger;
import com.acertainsupplychain.utility.LockMapManager;

/**
 * This class is an implementation of the ItemSupplier interface.
 * 
 * @author Arni
 * 
 */
public class ItemSupplierImpl implements ItemSupplier {

	private final int supplierID;
	private final Map<Integer, Integer> summedOrders;
	private final FileLogger fileLogger;
	private int logID;
	private final ReadWriteLock logIDLock;
	private final LockMapManager<Integer> lockManager;

	/**
	 * Initializes the ItemSupplier with a given supplier ID.
	 * 
	 * @param supplierID
	 */
	public ItemSupplierImpl(int supplierID) {
		this.supplierID = supplierID;
		logID = 0;
		summedOrders = new ConcurrentHashMap<Integer, Integer>();
		lockManager = new LockMapManager<Integer>();
		logIDLock = new ReentrantReadWriteLock();

		fileLogger = new FileLogger(this.supplierID + "_Supplier_logfile", "");
		fileLogger.logToFile("INITSUP " + this.supplierID + "\n", true);
	}

	@Override
	public void executeStep(OrderStep step) throws OrderProcessingException {
		// Validate the step before processing it.
		validateStep(step);

		// TODO must copy the step before adding it to the database.
		// -> nah, I just assume no one alters the object while I use it to
		// update the state

		// Update the lockMap before executing the step to ensure that the
		// needed locks exist
		lockManager.addToLockMap(extractSortedItemIDs(step));

		// Execute the step
		addStepToSummedOrders(step);
	}

	/**
	 * Validates a given OrderStep. If the step is not valid then an
	 * OrderProcessingException is thrown, otherwise the function simply returns
	 * with no exception.
	 * 
	 * @param step
	 *            , the OrderStep to be validated.
	 * @throws OrderProcessingException
	 */
	private void validateStep(OrderStep step) throws OrderProcessingException {
		if (step == null)
			throw new OrderProcessingException("Supplier with id ["
					+ supplierID + "]: The given OrderStep cannot be NULL.");

		if (step.getSupplierId() != supplierID)
			throw new OrderProcessingException("Supplier with id ["
					+ supplierID + "]: The given OrderStep does not have a "
					+ "matching supplier id [" + step.getSupplierId() + "]");

		if (step.getItems() == null)
			throw new OrderProcessingException("Supplier with id ["
					+ supplierID
					+ "]: The given OrderStep contains a NULL item");

		if (step.getItems().isEmpty())
			throw new OrderProcessingException("Supplier with id ["
					+ supplierID + "]: The given OrderStep cannot contain an "
					+ "empty list of items");

		for (ItemQuantity item : step.getItems()) {
			if (item == null)
				throw new OrderProcessingException("Supplier with id ["
						+ supplierID + "]: No items in the given OrderStep can"
						+ " be NULL.");
			if (item.getQuantity() < 1)
				throw new OrderProcessingException("Supplier with id ["
						+ supplierID + "]: You cannot order a non-positive "
						+ "amount of some item");
		}
	}

	/**
	 * Returns the next log ID and guarantees that no one else can get the same
	 * ID as you.
	 * 
	 * @return a unique log ID.
	 */
	private int getNextLogID() {
		logIDLock.writeLock().lock();
		int nextID = logID;
		logID++;
		logIDLock.writeLock().unlock();
		return nextID;
	}

	/**
	 * Extracts a list of item IDs from a given OrderStep and sorts the list in
	 * ascending order before returning it.
	 * 
	 * @param step
	 *            , the step to be extracted.
	 * @return the sorted list of item IDs.
	 */
	private List<Integer> extractSortedItemIDs(OrderStep step) {
		List<Integer> itemIDs = new ArrayList<Integer>();

		for (ItemQuantity item : step.getItems()) {
			itemIDs.add(item.getItemId());
		}

		Collections.sort(itemIDs);
		return itemIDs;
	}

	/**
	 * This function executes the OrderStep by adding it atomically to the map
	 * of summed orders. This function assumes that the step is valid
	 * 
	 * @param step
	 *            , the OrderStep to be executed.
	 */
	private void addStepToSummedOrders(OrderStep step) {
		int mylogID = getNextLogID();

		fileLogger.logToFile("EXEC-START " + mylogID + "\n", true);

		// To make sure that this function is atomic, we must 'prepare' how the
		// map is going to be after this execution
		Map<Integer, Integer> preparedMap = new HashMap<Integer, Integer>();
		List<Integer> itemIDs = extractSortedItemIDs(step);
		lockManager.acquireWriteLocks(itemIDs);

		for (ItemQuantity item : step.getItems()) {
			int summed = item.getQuantity();

			if (preparedMap.containsKey(item.getItemId())) {
				summed += preparedMap.get(item.getItemId());
			} else if (summedOrders.containsKey(item.getItemId())) {
				summed += summedOrders.get(item.getItemId());
			}
			preparedMap.put(item.getItemId(), summed);

		}

		// Do the atomic write
		summedOrders.putAll(preparedMap);

		// The logs in the loop below is supposed be inside the puAll function
		// and happen right after an element has been written to the
		// summedOrders map. Although instead of overwritten the putAll method
		// and forcing myself into a possible daring challenge I will for now
		// just do the logging afterwards.
		for (ItemQuantity item : step.getItems()) {
			fileLogger.logToFile("WRT " + mylogID + " " + item.getItemId()
					+ " " + item.getQuantity() + "\n", true);
		}

		fileLogger.logToFile("EXEC-DONE " + mylogID + "\n", true);
		lockManager.releaseWriteLocks(itemIDs);
	}

	@Override
	public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
			throws InvalidItemException {
		// Validate the set of item IDs before processing them.
		validateItemIDs(itemIds);

		List<ItemQuantity> allItems = new ArrayList<ItemQuantity>();

		List<Integer> itemIdList = new ArrayList<Integer>(itemIds);
		lockManager.acquireReadLocks(itemIdList);

		for (Integer id : itemIds) {
			allItems.add(new ItemQuantity(id, summedOrders.get(id)));
		}
		lockManager.releaseReadLocks(itemIdList);
		return allItems;
	}

	/**
	 * Validates a given set of item IDs. The function throws an
	 * InvalidItemException if the set is invalid, otherwise just returns
	 * normally.
	 * 
	 * @param itemIds
	 *            , the set of item IDs to be validated.
	 * @throws InvalidItemException
	 */
	private void validateItemIDs(Set<Integer> itemIds)
			throws InvalidItemException {
		if (itemIds == null)
			throw new InvalidItemException("Supplier with id [" + supplierID
					+ "]: The given Integer set cannot be NULL.");
		for (Integer id : itemIds) {
			if (id == null)
				throw new InvalidItemException("Supplier with id ["
						+ supplierID + "]: The given Integer set cannot"
						+ "contain a NULL Integer.");
			if (!summedOrders.containsKey(id))
				throw new InvalidItemException("Supplier with id ["
						+ supplierID + "]: Supplier have no records of "
						+ "any orders on item with id [" + id + "]");
		}
	}

	@Override
	public void clear() {
		logID = 0;
		summedOrders.clear();
		fileLogger.logToFile("CLEARDONE\n", true);
	}

	@Override
	public int getSupplierID() {
		return supplierID;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ItemSupplierImpl))
			return false;

		ItemSupplierImpl item = (ItemSupplierImpl) obj;
		return supplierID == item.supplierID
				&& summedOrders.equals(item.summedOrders);
	}

}
