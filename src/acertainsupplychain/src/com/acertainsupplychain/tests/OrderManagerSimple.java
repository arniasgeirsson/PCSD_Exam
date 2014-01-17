package com.acertainsupplychain.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.impl.ItemSupplierImpl;
import com.acertainsupplychain.impl.OrderManagerImpl;

public class OrderManagerSimple {

	private static OrderManager orderManager;
	private static Integer[] supplierIDs;
	private static Map<Integer, ItemSupplier> allSuppliers;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		supplierIDs = new Integer[] { 0, 1, 2, 3, 4 };
		allSuppliers = new HashMap<Integer, ItemSupplier>();

		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		for (Integer supplierID : supplierIDs) {
			ItemSupplier supplier = new ItemSupplierImpl(supplierID);
			suppliers.put(supplierID, supplier);
			allSuppliers.put(supplierID, supplier);
		}
		orderManager = new OrderManagerImpl(suppliers);
	}

	@After
	public void tearDown() throws Exception {
		orderManager.clear();
		// TODO why not also clear the item suppliers -> no need to as they wont
		// effect any state in the orderManager
		// -> wrong, we should also clear the suppliers, so we can be certain of
		// their states, as these also matters in the determination of the
		// correctness of the OrderManager
		for (ItemSupplier supplier : allSuppliers.values()) {
			supplier.clear();
		}
	}

	private int registerOrderWorkflow(List<OrderStep> steps) {
		try {
			return orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return 0;
	}

	private List<StepStatus> getOrderWorkflowStatus(int orderWorkflowId) {
		try {
			return orderManager.getOrderWorkflowStatus(orderWorkflowId);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return null;
	}

	private OrderStep createRandomValidOrderStep(Integer[] supplierIDs) {
		int maxItems = 10;
		int maxItemID = 20;
		int maxItemQuantity = 100;

		Random randGen = new Random(System.currentTimeMillis());

		int numberOfItems = randGen.nextInt(maxItems) + 1;

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		for (int i = 0; i < numberOfItems; i++) {
			items.add(new ItemQuantity(randGen.nextInt(maxItemID), randGen
					.nextInt(maxItemQuantity) + 1));
		}

		return new OrderStep(supplierIDs[randGen.nextInt(supplierIDs.length)],
				items);
	}

	// Defined as coded in ItemSupplierImpl
	private boolean isStepValid(OrderStep step) {
		if (step == null || step.getItems() == null)
			return false;
		for (ItemQuantity item : step.getItems()) {
			if (item == null || item.getQuantity() < 1)
				return false;
		}
		return true;
	}

	// A random int between n and m, including n excluding m where m > n
	private int nextInt(int n, int m) {
		if (n >= m)
			return n;
		return new Random(System.currentTimeMillis()).nextInt(m - n) + n;
	}

	// private List<StepStatus> createStepStatusList(OrderStep step) {
	// return createStepStatusList(new ArrayList<OrderStep>(
	// Arrays.asList(step)));
	// }

	private List<StepStatus> createStepStatusList(List<OrderStep> steps) {
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

	private Map<Integer, List<StepStatus>> setUpPreExceptionOrderManagerState() {
		List<OrderStep> workflow1 = new ArrayList<OrderStep>();
		workflow1.add(createRandomValidOrderStep(supplierIDs));
		workflow1.add(createRandomValidOrderStep(supplierIDs));
		workflow1.add(createRandomValidOrderStep(supplierIDs));

		List<OrderStep> workflow2 = new ArrayList<OrderStep>();
		workflow2.add(createRandomValidOrderStep(supplierIDs));
		workflow2.add(createRandomValidOrderStep(supplierIDs));
		workflow2.add(createRandomValidOrderStep(supplierIDs));

		Map<Integer, List<StepStatus>> expectedState = new HashMap<Integer, List<StepStatus>>();

		int workflowID1 = registerOrderWorkflow(workflow1);
		int workflowID2 = registerOrderWorkflow(workflow2);
		expectedState.put(workflowID1, createStepStatusList(workflow1));
		expectedState.put(workflowID2, createStepStatusList(workflow2));

		waitForJobsToFinish(orderManager);

		return expectedState;
	}

	@Test
	public final void testOrderManager_NullSupplierMap() {
		Map<Integer, ItemSupplier> suppliers = null;
		try {
			new OrderManagerImpl(suppliers);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public final void testOrderManager_EmptySupplierMap() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		try {
			new OrderManagerImpl(suppliers);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public final void testOrderManager_NullSupplier() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		suppliers.put(1, null);
		try {
			new OrderManagerImpl(suppliers);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public final void testOrderManager_IDmismatch() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		suppliers.put(1, new ItemSupplierImpl(2));
		try {
			new OrderManagerImpl(suppliers);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_NullList() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		List<OrderStep> steps = null;

		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_EmptyList() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		List<OrderStep> steps = new ArrayList<OrderStep>();

		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_NullStep() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		List<OrderStep> steps = new ArrayList<OrderStep>();
		steps.add(createRandomValidOrderStep(supplierIDs));
		steps.add(createRandomValidOrderStep(supplierIDs));
		steps.add(null);
		steps.add(createRandomValidOrderStep(supplierIDs));
		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	// @Test Disabled
	public final void testRegisterOrderWorkflow_NullItem() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));
		items.add(null);
		OrderStep step = new OrderStep(supplierIDs[0], items);

		List<OrderStep> steps = new ArrayList<OrderStep>();
		steps.add(step);

		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	// @Test Disabled
	public final void testRegisterOrderWorkflow_NonPositiveQuantity() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, -10));
		OrderStep step = new OrderStep(supplierIDs[0], items);

		List<OrderStep> steps = new ArrayList<OrderStep>();
		steps.add(step);

		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_NonExistingSupplier() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		Integer wrongSupplierID = 10;
		assertFalse(Arrays.asList(supplierIDs).contains(wrongSupplierID));

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));
		OrderStep step = new OrderStep(wrongSupplierID, items);

		List<OrderStep> steps = new ArrayList<OrderStep>();
		steps.add(step);

		try {
			orderManager.registerOrderWorkflow(steps);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	private void tearDownWrapper() {
		try {
			tearDown();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	private Set<Integer> extractItemIds(List<ItemQuantity> list) {
		Set<Integer> itemIds = new HashSet<Integer>();

		for (ItemQuantity item : list) {
			itemIds.add(item.getItemId());
		}

		return itemIds;
	}

	private List<ItemQuantity> getOrdersPerItem(ItemSupplier supplier,
			Set<Integer> itemIds) {
		try {
			return supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return null;
	}

	// TODO split?
	@Test
	public final void testRegisterOrderWorkflow_Valid() {
		// Make sure the OrderManager(<-how?) and the ItemSuppliers(<-how?) have
		// the
		// correct starting state
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();

		List<OrderStep> steps = new ArrayList<OrderStep>();
		OrderStep step = null;
		List<StepStatus> stepStatus = new ArrayList<OrderManager.StepStatus>();
		Integer workflowID = null;
		Map<Integer, List<StepStatus>> expectedOMState = new HashMap<Integer, List<StepStatus>>();
		Map<Integer, List<ItemQuantity>> expectedSUPState = new HashMap<Integer, List<ItemQuantity>>();

		// 1. Add a single valid step (that should cause no fails)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		tearDownWrapper();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();
		expectedSUPState.clear();

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 10));

		step = new OrderStep(supplierIDs[0], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 2. Add several valid steps to same supplier (that should cause no
		// fails)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		tearDownWrapper();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();
		expectedSUPState.clear();

		// Add one step
		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 10));

		step = new OrderStep(supplierIDs[0], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		// Add another
		items = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(2, 100));
		items.add(new ItemQuantity(3, 880));
		localList.add(new ItemQuantity(2, 100));
		localList.add(new ItemQuantity(3, 880));

		step = new OrderStep(supplierIDs[0], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		// Add the third
		items = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(4, 90));
		items.add(new ItemQuantity(5, 1));
		localList.add(new ItemQuantity(4, 90));
		localList.add(new ItemQuantity(5, 1));

		step = new OrderStep(supplierIDs[0], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 3. Add several valid steps to different suppliers (that should cause
		// no fails)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		tearDownWrapper();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();
		expectedSUPState.clear();

		// Add one step
		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 10));

		step = new OrderStep(supplierIDs[0], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		// Add another
		items = new ArrayList<ItemQuantity>();
		localList = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(2, 100));
		items.add(new ItemQuantity(3, 880));
		localList.add(new ItemQuantity(2, 100));
		localList.add(new ItemQuantity(3, 880));

		step = new OrderStep(supplierIDs[1], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		// Add the third
		items = new ArrayList<ItemQuantity>();
		localList = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(4, 90));
		items.add(new ItemQuantity(5, 1));
		localList.add(new ItemQuantity(4, 90));
		localList.add(new ItemQuantity(5, 1));

		step = new OrderStep(supplierIDs[2], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 4. Add a single invalid step (that should cause a fail)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		orderManager.clear();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();

		items.add(new ItemQuantity(0, -10));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 5. Add several invalid steps to same supplier (that should cause a
		// fail)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		orderManager.clear();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();

		// Add one step
		items.add(new ItemQuantity(0, 0));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		// Add another
		items = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(2, -100));
		items.add(new ItemQuantity(3, 880));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		// Add the third
		items = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(4, -90));
		items.add(null);
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 6. Add several invalid steps to different suppliers (that should
		// cause
		// a fail)
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		orderManager.clear();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();

		// Add one step
		items.add(new ItemQuantity(0, -10));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		// Add another
		items = new ArrayList<ItemQuantity>();

		items.add(null);
		items.add(new ItemQuantity(3, 880));
		step = new OrderStep(supplierIDs[1], items);
		steps.add(step);

		// Add the third
		items = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(4, -90));
		items.add(new ItemQuantity(5, 1));
		step = new OrderStep(supplierIDs[2], items);
		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}

		// 7. Add a mix of valid and invalid steps to different suppliers
		// -> and see that the OrderManager and the ItemSuppliers have the
		// correct state
		orderManager.clear();
		items.clear();
		localList.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;
		expectedOMState.clear();

		// Add one step
		items.add(new ItemQuantity(0, -10));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		// Add another
		items = new ArrayList<ItemQuantity>();
		localList = new ArrayList<ItemQuantity>();

		items.add(new ItemQuantity(8, 140));
		items.add(new ItemQuantity(9, 50));
		localList.add(new ItemQuantity(8, 140));
		localList.add(new ItemQuantity(9, 50));

		step = new OrderStep(supplierIDs[3], items);
		expectedSUPState.put(step.getSupplierId(), localList);

		steps.add(step);

		// Add the third
		items = new ArrayList<ItemQuantity>();

		items.add(null);
		items.add(new ItemQuantity(5, 1));
		step = new OrderStep(supplierIDs[2], items);
		steps.add(step);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		stepStatus = createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					getOrderWorkflowStatus(workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(
					expectedList,
					getOrdersPerItem(allSuppliers.get(supplierID),
							extractItemIds(expectedList)));
		}
	}

	@Test
	public final void testGetOrderWorkflowStatus_NonExistingID() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = setUpPreExceptionOrderManagerState();
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}

		Integer wrongSupplierID = 10;
		assertFalse(Arrays.asList(supplierIDs).contains(wrongSupplierID));

		try {
			orderManager.getOrderWorkflowStatus(wrongSupplierID);
			fail();
		} catch (InvalidWorkflowException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the orderManager is the same as before
		// the exception occurred
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					getOrderWorkflowStatus(workflowID));
		}
	}

	// TODO split?
	@Test
	public final void testGetOrderWorkflowStatus_Valid() {
		// NOTE: Main focus is on making sure that the order is correct
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();

		List<OrderStep> steps = new ArrayList<OrderStep>();
		OrderStep step = null;
		List<StepStatus> stepStatus = new ArrayList<OrderManager.StepStatus>();
		Integer workflowID = null;

		// 1. Register a single valid step and see that it matches
		tearDownWrapper();
		items.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));

		step = new OrderStep(supplierIDs[0], items);

		steps.add(step);
		stepStatus = createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		assertTrue(compareOrder(stepStatus, getOrderWorkflowStatus(workflowID)));
		// assertEquals(stepStatus, getOrderWorkflowStatus(workflowID));

		// 2. Register multiple valid steps and make sure they are in the
		// correct order
		tearDownWrapper();
		items.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		items.add(new ItemQuantity(2, 50));
		items.add(new ItemQuantity(3, 20));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		items.add(new ItemQuantity(0, 5));
		items.add(new ItemQuantity(2, 7));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		stepStatus = createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		assertTrue(compareOrder(stepStatus, getOrderWorkflowStatus(workflowID)));
		// assertEquals(stepStatus, getOrderWorkflowStatus(workflowID));

		// 3. Register a mismatch of valid and invalid steps and make sure they
		// are in the correct order
		tearDownWrapper();
		items.clear();
		steps.clear();
		step = null;
		stepStatus.clear();
		workflowID = null;

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		items.add(new ItemQuantity(2, -50));
		items.add(new ItemQuantity(3, 20));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		items.add(new ItemQuantity(0, 5));
		items.add(new ItemQuantity(2, 7));
		step = new OrderStep(supplierIDs[0], items);
		steps.add(step);

		stepStatus = createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		waitForJobsToFinish(orderManager);

		assertTrue(compareOrder(stepStatus, getOrderWorkflowStatus(workflowID)));
		// assertEquals(stepStatus, getOrderWorkflowStatus(workflowID));
	}

	private <E> boolean compareOrder(List<E> list1, List<E> list2) {
		int size = list1.size();
		if (size != list2.size())
			return false;
		for (int i = 0; i < size; i++) {
			if (!list1.get(i).equals(list2.get(i)))
				return false;
		}
		return true;
	}

	private void waitForJobsToFinish(OrderManager orderManager) {
		try {
			orderManager.waitForJobsToFinish();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
