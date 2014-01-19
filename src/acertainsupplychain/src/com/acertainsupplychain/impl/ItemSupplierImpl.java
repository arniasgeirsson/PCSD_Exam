package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.utility.FileLogger;

public class ItemSupplierImpl implements ItemSupplier {

	private final int supplierID;
	private final Map<Integer, Integer> summedOrders;
	private final FileLogger fileLogger;

	private int logID;

	public ItemSupplierImpl(int supplierID) {
		this.supplierID = supplierID;
		summedOrders = new HashMap<Integer, Integer>();
		logID = 0;

		fileLogger = new FileLogger(this.supplierID + "_Supplier_logfile",
				"How to read this log file?\n");
		fileLogger.logToFile("INITSUP " + this.supplierID + "\n", true);
	}

	@Override
	public void executeStep(OrderStep step) throws OrderProcessingException {
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
			// TODO as one can only buy stuff a negative amount should not be
			// valid
			// TODO 0 is not okay either, as it does not make sense to buy
			// nothing
			if (item.getQuantity() < 1)
				throw new OrderProcessingException("Supplier with id ["
						+ supplierID + "]: You cannot order a non-positive "
						+ "amount of some item");
		}

		// TODO can item ids be negative? Yes

		// TODO must copy the step before adding it to the database.
		// -> nah, I just assume no one alters the object while I use it to
		// update the state

		// Execute the step
		addStepToSummedOrders(step);
	}

	// This function assumes that the step is valid
	private void addStepToSummedOrders(OrderStep step) {
		// TODO lock the ID
		int mylogID = logID++;
		// TODO unlock the ID
		fileLogger.logToFile("EXEC-START " + mylogID + "\n", true);
		for (ItemQuantity item : step.getItems()) {
			int summed = item.getQuantity();
			if (summedOrders.containsKey(item.getItemId())) {
				summed += summedOrders.get(item.getItemId());
			}
			summedOrders.put(item.getItemId(), summed);
			fileLogger.logToFile("WRT " + mylogID + " " + item.getItemId()
					+ " " + item.getQuantity() + "\n", true);
		}
		fileLogger.logToFile("EXEC-DONE " + mylogID + "\n", true);
	}

	@Override
	public List<ItemQuantity> getOrdersPerItem(Set<Integer> itemIds)
			throws InvalidItemException {
		if (itemIds == null)
			throw new InvalidItemException("Supplier with id [" + supplierID
					+ "]: The given Integer set cannot be NULL.");

		List<ItemQuantity> allItems = new ArrayList<ItemQuantity>();

		for (Integer id : itemIds) {
			if (id == null)
				throw new InvalidItemException("Supplier with id ["
						+ supplierID + "]: The given Integer set cannot"
						+ "contain a NULL Integer.");
			if (summedOrders.containsKey(id)) {
				allItems.add(new ItemQuantity(id, summedOrders.get(id)));
			} else
				throw new InvalidItemException("Supplier with id ["
						+ supplierID + "]: Supplier have no records of "
						+ "any orders on item with id [" + id + "]");
		}
		return allItems;
	}

	@Override
	public void clear() {
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

	// TODO
	// http://stackoverflow.com/questions/27581/overriding-equals-and-hashcode-in-java
	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
