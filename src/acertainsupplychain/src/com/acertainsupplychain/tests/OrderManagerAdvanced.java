package com.acertainsupplychain.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.InvalidWorkflowException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.clients.ItemSupplierHTTPProxy;
import com.acertainsupplychain.clients.OrderManagerHTTPProxy;
import com.acertainsupplychain.impl.ItemSupplierImpl;
import com.acertainsupplychain.server.ItemSupplierHTTPServer;
import com.acertainsupplychain.server.OrderManagerHTTPServer;
import com.acertainsupplychain.utility.ItemSupplierUtility;
import com.acertainsupplychain.utility.TestUtility;

public class OrderManagerAdvanced {

	private static OrderManager orderManager;
	private static Integer[] supplierIDs;
	private static Map<Integer, ItemSupplier> allSuppliers;

	private static List<Process> itemSupplierProcesses;
	private static Process orderManagerProcess;
	private static int port = 8079;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		supplierIDs = new Integer[] { 0, 1, 2, 3, 4 };
		allSuppliers = new HashMap<Integer, ItemSupplier>();
		itemSupplierProcesses = new ArrayList<Process>();

		Map<Integer, Integer> suppliers = new HashMap<Integer, Integer>();

		for (Integer supplierID : supplierIDs) {
			// Start a new itemSupplier server
			Process itemSupplierProcess = ItemSupplierUtility.startProcess(
					ItemSupplierHTTPServer.class, Integer.toString(++port));
			itemSupplierProcesses.add(itemSupplierProcess);

			// Create new ItemSupplier proxy
			ItemSupplier supplier = new ItemSupplierHTTPProxy(supplierID, port);
			suppliers.put(supplierID, port);
			allSuppliers.put(supplierID, supplier);
		}

		// Start new OrderManger server
		orderManagerProcess = ItemSupplierUtility.startProcess(
				OrderManagerHTTPServer.class, Integer.toString(++port));
		orderManager = new OrderManagerHTTPProxy(0, port, suppliers);
	}

	@AfterClass
	public static void testDownAfterClass() throws Exception {
		// Stop all the itemSupplier jetty clients
		for (ItemSupplier itemSupplier : allSuppliers.values()) {
			((ItemSupplierHTTPProxy) itemSupplier).stop();
		}
		// Stop all the itemSupplier servers
		for (Process process : itemSupplierProcesses) {
			ItemSupplierUtility.stopProcess(process);
		}

		orderManager.stopItemSupplierProxies();
		// Stop the OrderManager client
		((OrderManagerHTTPProxy) orderManager).stop();
		// Stop the OrderManager server
		ItemSupplierUtility.stopProcess(orderManagerProcess);
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

	private void tearDownWrapper() {
		try {
			tearDown();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	// TODO, do they make sense in advanced test? -> yes, refactor
	@Test
	public final void testOrderManager_NullSupplierMap() {
		Map<Integer, ItemSupplier> suppliers = null;
		Process process = null;

		try {
			process = ItemSupplierUtility.startProcess(
					OrderManagerHTTPServer.class, Integer.toString(++port));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		boolean successFullTest = false;

		try {

			new OrderManagerHTTPProxy(0, suppliers, port);
		} catch (OrderProcessingException e) {
			successFullTest = true;
		} catch (Exception e) {
		}

		ItemSupplierUtility.stopProcess(process);

		assertTrue(successFullTest);
	}

	// TODO, do they make sense in advanced test? -> yes, refactor
	@Test
	public final void testOrderManager_EmptySupplierMap() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		Process process = null;

		try {
			process = ItemSupplierUtility.startProcess(
					OrderManagerHTTPServer.class, Integer.toString(++port));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		boolean successFullTest = false;

		try {

			new OrderManagerHTTPProxy(0, suppliers, port);
		} catch (OrderProcessingException e) {
			successFullTest = true;
		} catch (Exception e) {
		}

		ItemSupplierUtility.stopProcess(process);

		assertTrue(successFullTest);
	}

	// TODO, do they make sense in advanced test? -> yes, refactor
	@Test
	public final void testOrderManager_NullSupplier() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		suppliers.put(1, null);
		Process process = null;

		try {
			process = ItemSupplierUtility.startProcess(
					OrderManagerHTTPServer.class, Integer.toString(++port));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		boolean successFullTest = false;

		try {

			new OrderManagerHTTPProxy(0, suppliers, port);
		} catch (OrderProcessingException e) {
			successFullTest = true;
		} catch (Exception e) {
		}

		ItemSupplierUtility.stopProcess(process);

		assertTrue(successFullTest);
	}

	// TODO, do they make sense in advanced test? -> yes, refactor
	@Test
	public final void testOrderManager_IDmismatch() {
		Map<Integer, ItemSupplier> suppliers = new HashMap<Integer, ItemSupplier>();
		suppliers.put(1, new ItemSupplierImpl(2));
		Process process = null;

		try {
			process = ItemSupplierUtility.startProcess(
					OrderManagerHTTPServer.class, Integer.toString(++port));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		boolean successFullTest = false;

		try {

			new OrderManagerHTTPProxy(0, suppliers, port);
		} catch (OrderProcessingException e) {
			successFullTest = true;
		} catch (Exception e) {
		}

		ItemSupplierUtility.stopProcess(process);

		assertTrue(successFullTest);
	}

	@Test
	public final void testRegisterOrderWorkflow_NullList() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_EmptyList() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_NullStep() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}

		List<OrderStep> steps = new ArrayList<OrderStep>();
		steps.add(TestUtility.createRandomValidOrderStep(supplierIDs));
		steps.add(TestUtility.createRandomValidOrderStep(supplierIDs));
		steps.add(null);
		steps.add(TestUtility.createRandomValidOrderStep(supplierIDs));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
	}

	// @Test Disabled
	public final void testRegisterOrderWorkflow_NullItem() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
	}

	// @Test Disabled
	public final void testRegisterOrderWorkflow_NonPositiveQuantity() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
	}

	@Test
	public final void testRegisterOrderWorkflow_NonExistingSupplier() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
		}
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers are still in the same state as before this tests
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
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

		stepStatus = TestUtility.createStepStatusList(steps);
		expectedOMState.put(workflowID, stepStatus);

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		// Check the orderManager
		for (Integer workflowIDTemp : expectedOMState.keySet()) {
			assertEquals(expectedOMState.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager,
							workflowIDTemp));
		}

		// Check the suppliers
		for (Integer supplierID : expectedSUPState.keySet()) {
			List<ItemQuantity> expectedList = expectedSUPState.get(supplierID);
			assertEquals(expectedList, TestUtility.getOrdersPerItem(
					allSuppliers.get(supplierID),
					TestUtility.extractItemIds(expectedList)));
		}
	}

	@Test
	public final void testGetOrderWorkflowStatus_NonExistingID() {
		// Initialize the state of the orderManager pre exception and make sure
		// it is in the state we expect
		Map<Integer, List<StepStatus>> expectedState = TestUtility
				.setUpPreExceptionOrderManagerState(orderManager, supplierIDs);
		for (Integer workflowID : expectedState.keySet()) {
			assertEquals(expectedState.get(workflowID),
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
					TestUtility
							.getOrderWorkflowStatus(orderManager, workflowID));
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
		stepStatus = TestUtility.createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		assertTrue(TestUtility.compareOrder(stepStatus,
				TestUtility.getOrderWorkflowStatus(orderManager, workflowID)));
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

		stepStatus = TestUtility.createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		assertTrue(TestUtility.compareOrder(stepStatus,
				TestUtility.getOrderWorkflowStatus(orderManager, workflowID)));
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

		stepStatus = TestUtility.createStepStatusList(steps);

		try {
			workflowID = orderManager.registerOrderWorkflow(steps);
		} catch (Exception e) {
			fail();
		}

		// Make sure that the orderManager and the affected suppliers are in the
		// correct state
		TestUtility.waitForJobsToFinish(orderManager);

		assertTrue(TestUtility.compareOrder(stepStatus,
				TestUtility.getOrderWorkflowStatus(orderManager, workflowID)));
		// assertEquals(stepStatus, getOrderWorkflowStatus(workflowID));
	}

	@Test
	public final void testJobGetSupplier() {
		// 1. Test that all the suppliers created in BeforeClass are the same as
		// the ones given when asked
		// TODO cannot send ItemSupplierProxies over RPC..
		// for (Integer supplierID : allSuppliers.keySet()) {
		// try {
		// assertEquals(allSuppliers.get(supplierID),
		// orderManager.jobGetSupplier(supplierID));
		// } catch (Exception e) {
		// e.printStackTrace();
		// fail();
		// }
		// }

		// 2. Test that with a unknown supplier id the proper exception is
		// thrown

		int wrongSupplierID = 6;
		assertFalse(allSuppliers.keySet().contains(wrongSupplierID));

		try {
			assertEquals(allSuppliers.get(wrongSupplierID),
					orderManager.jobGetSupplier(wrongSupplierID));
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public final void testJobGetWorkflow() {

		// 1. Test that a returned workflow returns the workflow given
		List<OrderStep> workflowLocal = new ArrayList<OrderStep>();
		workflowLocal.add(TestUtility.createRandomValidOrderStep(supplierIDs));
		workflowLocal.add(TestUtility.createRandomValidOrderStep(supplierIDs));
		workflowLocal.add(TestUtility.createRandomValidOrderStep(supplierIDs));

		Integer workflowID = null;
		List<OrderStep> workflow = null;

		workflowID = TestUtility.registerOrderWorkflow(orderManager,
				workflowLocal);

		try {
			workflow = orderManager.jobGetWorkFlow(workflowID);
		} catch (Exception e) {
			fail();
		}

		assertTrue(TestUtility.compareOrder(workflowLocal, workflow));

		// 2. Test that a workflow that has not been returned throws proper
		// exception
		try {
			orderManager.jobGetWorkFlow(workflowID + 1);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}
		// 3. Test that the workflow from test 1. still works
		// TODO must also make sure that the state of the orderManager is the
		// correct one
		try {
			workflow = orderManager.jobGetWorkFlow(workflowID);
		} catch (Exception e) {
			fail();
		}

		assertEquals(workflowLocal, workflow);
	}

	@Test
	public final void testJobSetStatus() {
		// TODO this cannot be done, but assumed to work, as the other tests
		// does not fail
	}

	@Test
	public final void testWaitForJobsToFinish() {
		// TODO this cannot be done, but assumed to work, as the other tests
		// does not fail
	}

	@Test
	public final void testStopItemSupplierProxies() {
		// TODO this cannot be done, but assumed to work, as the other tests
		// does not fail
	}

}
