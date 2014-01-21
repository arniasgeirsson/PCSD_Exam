package com.acertainsupplychain.utility;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderStep;

/**
 * This is a utility class used by my test functions only, and is used to gather
 * a set of common helper functions needed in the test classes.
 * 
 * @author Arni
 * 
 */
public class TestUtility {

	/**
	 * Creates and sets a state for a given ItemSupplier and returns the
	 * expected state.
	 * 
	 * @param supplier
	 * @return
	 */
	public static List<ItemQuantity> setUpPreExceptionSupplierState(
			ItemSupplier supplier) {
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 11));
		items.add(new ItemQuantity(2, 12));

		OrderStep step = new OrderStep(supplier.getSupplierID(), items);
		executeStep(supplier, step);

		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 11));
		localList.add(new ItemQuantity(2, 12));

		Set<Integer> itemIds = new HashSet<Integer>();
		itemIds.add(0);
		itemIds.add(1);
		itemIds.add(2);

		return localList;
	}

	/**
	 * This function extracts the set of item IDs from a list of ItemQuantity
	 * instances.
	 * 
	 * @param list
	 * @return
	 */
	public static Set<Integer> extractItemIds(List<ItemQuantity> list) {
		Set<Integer> itemIds = new HashSet<Integer>();

		for (ItemQuantity item : list) {
			itemIds.add(item.getItemId());
		}

		return itemIds;
	}

	/**
	 * This function is merely a wrapper around the executeStep function in an
	 * ItemSupplier when this is needed but the function is under no
	 * circumstances expected to throw any exception.
	 * 
	 * @param supplier
	 * @param step
	 */
	public static void executeStep(ItemSupplier supplier, OrderStep step) {
		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * This function is merely a wrapper around the getOrdersPerItem function in
	 * an ItemSupplier when this is needed but the function is under no
	 * circumstances expected to throw any exception.
	 * 
	 * @param supplier
	 * @param itemIds
	 * @return
	 */
	public static List<ItemQuantity> getOrdersPerItem(ItemSupplier supplier,
			Set<Integer> itemIds) {
		try {
			return supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			// e.printStackTrace();
			fail();
		}
		return null;
	}

	/**
	 * This function is merely a wrapper around the registerOrderWorkflow
	 * function in an OrderManager when this is needed but the function is under
	 * no circumstances expected to throw any exception.
	 * 
	 * @param orderManager
	 * @param steps
	 * @return
	 */
	public static int registerOrderWorkflow(OrderManager orderManager,
			List<OrderStep> steps) {
		try {
			return orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return 0;
	}

	/**
	 * This function is merely a wrapper around the getOrderWorkflowStatus
	 * function in an OrderManager when this is needed but the function is under
	 * no circumstances expected to throw any exception.
	 * 
	 * @param orderManager
	 * @param orderWorkflowId
	 * @return
	 */
	public static List<StepStatus> getOrderWorkflowStatus(
			OrderManager orderManager, int orderWorkflowId) {
		try {
			return orderManager.getOrderWorkflowStatus(orderWorkflowId);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return null;
	}

	public static OrderStep createRandomValidOrderStep(Set<Integer> supplierIDs) {
		return createRandomValidOrderStep(new ArrayList<Integer>(supplierIDs));
	}

	public static OrderStep createRandomValidOrderStep(Integer[] supplierIDs) {
		return createRandomValidOrderStep(Arrays.asList(supplierIDs));
	}

	/**
	 * This function creates a random valid OrderStep. That the OrderStep is
	 * valid means it will pass validation in OrderManagerImpl.
	 * 
	 * @param supplierIDs
	 * @return
	 */
	public static OrderStep createRandomValidOrderStep(List<Integer> supplierIDs) {
		int maxItems = 10;
		int minItemID = -10;
		int maxItemID = 20;
		int minItemQuantity = -50;
		int maxItemQuantity = 100;

		int numberOfItems = TestUtility.nextInt(0, maxItems);

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		for (int i = 0; i < numberOfItems; i++) {
			items.add(new ItemQuantity(TestUtility
					.nextInt(minItemID, maxItemID), TestUtility.nextInt(
					minItemQuantity, maxItemQuantity)));
		}

		return new OrderStep(supplierIDs.get(TestUtility.nextInt(0,
				supplierIDs.size())), items);
	}

	/**
	 * Validates an OrderStep under the same validation rules in
	 * ItemSupplierImpl.
	 * 
	 * @param step
	 * @return
	 */
	public static boolean isStepValid(OrderStep step) {
		if (step == null || step.getItems() == null
				|| step.getItems().isEmpty())
			return false;
		for (ItemQuantity item : step.getItems()) {
			if (item == null || item.getQuantity() < 1)
				return false;
		}
		return true;
	}

	/**
	 * Returns a random int between n and m, including n and excluding m. If m
	 * >= n then n is returned.
	 * 
	 * @param n
	 * @param m
	 * @return
	 */
	public static int nextInt(int n, int m) {
		if (n >= m)
			return n;
		return new Random().nextInt(m - n) + n;
	}

	/**
	 * This function creates the expected StepStatus list that would returned by
	 * the respective OrderManager after processing.
	 * 
	 * @param steps
	 * @return
	 */
	public static List<StepStatus> createStepStatusList(List<OrderStep> steps) {
		List<StepStatus> stepStatus = new ArrayList<StepStatus>();
		for (OrderStep step : steps) {
			if (isStepValid(step)) {
				stepStatus.add(StepStatus.SUCCESSFUL);
			} else {
				stepStatus.add(StepStatus.FAILED);
			}
		}

		return stepStatus;
	}

	/**
	 * Sets up a given OrderManager in some inital state and returns the
	 * expected state.
	 * 
	 * @param orderManager
	 * @param supplierIDs
	 * @return
	 */
	public static Map<Integer, List<StepStatus>> setUpPreExceptionOrderManagerState(
			OrderManager orderManager, Integer[] supplierIDs) {
		List<OrderStep> workflow1 = new ArrayList<OrderStep>();
		workflow1.add(createRandomValidOrderStep(supplierIDs));
		workflow1.add(createRandomValidOrderStep(supplierIDs));
		workflow1.add(createRandomValidOrderStep(supplierIDs));

		List<OrderStep> workflow2 = new ArrayList<OrderStep>();
		workflow2.add(createRandomValidOrderStep(supplierIDs));
		workflow2.add(createRandomValidOrderStep(supplierIDs));
		workflow2.add(createRandomValidOrderStep(supplierIDs));

		Map<Integer, List<StepStatus>> expectedState = new HashMap<Integer, List<StepStatus>>();

		int workflowID1 = registerOrderWorkflow(orderManager, workflow1);
		int workflowID2 = registerOrderWorkflow(orderManager, workflow2);
		expectedState.put(workflowID1, createStepStatusList(workflow1));
		expectedState.put(workflowID2, createStepStatusList(workflow2));

		waitForJobsToFinish(orderManager);

		return expectedState;
	}

	/**
	 * Compares to lists and makes sure the order is the same.
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static <E> boolean compareOrder(List<E> list1, List<E> list2) {
		int size = list1.size();
		if (size != list2.size())
			return false;
		for (int i = 0; i < size; i++) {
			if (!list1.get(i).equals(list2.get(i)))
				return false;
		}
		return true;
	}

	/**
	 * This function is merely a wrapper around the waitForJobsToFinish function
	 * in an OrderManager when this is needed but the function is under no
	 * circumstances expected to throw any exception.
	 * 
	 * @param orderManager
	 */
	public static void waitForJobsToFinish(OrderManager orderManager) {
		try {
			orderManager.waitForJobsToFinish();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
