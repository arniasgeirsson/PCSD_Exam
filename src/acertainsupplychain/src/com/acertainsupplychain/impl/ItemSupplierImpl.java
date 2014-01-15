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

public class ItemSupplierImpl implements ItemSupplier {

	private final int supplierID;
	private final List<OrderStep> allHandledOrders; // A log file
	private final Map<Integer, Integer> summedOrders;

	public ItemSupplierImpl(int supplierID) {
		this.supplierID = supplierID;
		allHandledOrders = new ArrayList<OrderStep>();
		summedOrders = new HashMap<Integer, Integer>();
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

		for (ItemQuantity item : step.getItems()) {
			if (item == null)
				throw new OrderProcessingException("Supplier with id ["
						+ supplierID + "]: No items in the given OrderStep can"
						+ " be NULL.");
		}

		// TODO as one can only buy stuff a negative amount should not be valid
		// TODO 0 is not okay either, as it does not make sense to buy nothing
		for (ItemQuantity item : step.getItems()) {
			if (item.getQuantity() < 1)
				throw new OrderProcessingException("Supplier with id ["
						+ supplierID + "]: You cannot order a non-positive "
						+ "amount of some item");
		}

		// TODO can item ids be negative?

		// TODO should an empty list be allowed?

		// TODO must copy the step before adding it to the database.

		// Execute the step
		if (!allHandledOrders.add(step))
			throw new OrderProcessingException("Supplier with id ["
					+ supplierID + "]: Something unexpected happened when "
					+ "trying to add step to log file.");
		addStepToSummedOrders(step);
	}

	// This function assumes that the step is valid
	private void addStepToSummedOrders(OrderStep step) {
		for (ItemQuantity item : step.getItems()) {
			int summed = item.getQuantity();
			if (summedOrders.containsKey(item.getItemId())) {
				summed += summedOrders.get(item.getItemId());
			}
			summedOrders.put(item.getItemId(), summed);
		}
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
		allHandledOrders.clear();
		summedOrders.clear();
	}

	@Override
	public int getSupplierID() {
		return supplierID;
	}

}
