package com.acertainsupplychain.utility;

import static org.junit.Assert.fail;

import java.util.ArrayList;
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

public class TestUtility {

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

	public static Set<Integer> extractItemIds(List<ItemQuantity> list) {
		Set<Integer> itemIds = new HashSet<Integer>();

		for (ItemQuantity item : list) {
			itemIds.add(item.getItemId());
		}

		return itemIds;
	}

	// This is a wrapper function whom intended use when you want to call the
	// executeStep function but does not expect an exception to be thrown
	public static void executeStep(ItemSupplier supplier, OrderStep step) {
		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			// e.printStackTrace();
			fail();
		}
	}

	// Same comment as above
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

	public static OrderStep createRandomValidOrderStep(Integer[] supplierIDs) {
		int maxItems = 10;
		int minItemID = -10;
		int maxItemID = 20;
		int minItemQuantity = -50;
		int maxItemQuantity = 100;

		int numberOfItems = TestUtility.nextInt(1, maxItems + 1);

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		for (int i = 0; i < numberOfItems; i++) {
			items.add(new ItemQuantity(TestUtility
					.nextInt(minItemID, maxItemID), TestUtility.nextInt(
					minItemQuantity, maxItemQuantity)));
		}

		return new OrderStep(supplierIDs[TestUtility.nextInt(0,
				supplierIDs.length)], items);
	}

	// Defined as coded in ItemSupplierImpl
	public static boolean isStepValid(OrderStep step) {
		if (step == null || step.getItems() == null)
			return false;
		for (ItemQuantity item : step.getItems()) {
			if (item == null || item.getQuantity() < 1)
				return false;
		}
		return true;
	}

	// A random int between n and m, including n excluding m where m > n
	public static int nextInt(int n, int m) {
		if (n >= m)
			return n;
		return new Random(System.currentTimeMillis()).nextInt(m - n) + n;
	}

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

	public static void waitForJobsToFinish(OrderManager orderManager) {
		try {
			orderManager.waitForJobsToFinish();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
