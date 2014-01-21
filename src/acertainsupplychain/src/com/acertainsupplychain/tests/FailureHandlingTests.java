package com.acertainsupplychain.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.OrderManager.StepStatus;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.clients.ItemSupplierHTTPProxy;
import com.acertainsupplychain.clients.OrderManagerHTTPProxy;
import com.acertainsupplychain.server.ItemSupplierHTTPServer;
import com.acertainsupplychain.server.OrderManagerHTTPServer;
import com.acertainsupplychain.utility.ItemSupplierUtility;
import com.acertainsupplychain.utility.TestUtility;

/**
 * This JUnit test class is used to test that failure handling is done as
 * expected.
 * 
 * @author Arni
 * 
 */
public class FailureHandlingTests {

	private static ItemSupplier itemSupplier1;
	private static ItemSupplier itemSupplier2;

	private static Process itemSupplierProcess1;
	private static Process itemSupplierProcess2;

	private static OrderManager orderManager1;
	private static OrderManager orderManager2;

	private static Process orderManagerProcess1;
	private static Process orderManagerProcess2;

	private static int port = 8089;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Map<Integer, Integer> supplierProxyPorts = new HashMap<Integer, Integer>();

		// Create the first ItemSupplier server and proxy
		itemSupplierProcess1 = ItemSupplierUtility.startProcess(
				ItemSupplierHTTPServer.class, true, Integer.toString(++port));
		itemSupplier1 = new ItemSupplierHTTPProxy(0, port);

		supplierProxyPorts.put(0, port);

		// Create the second ItemSupplier server and proxy
		itemSupplierProcess2 = ItemSupplierUtility.startProcess(
				ItemSupplierHTTPServer.class, true, Integer.toString(++port));
		itemSupplier2 = new ItemSupplierHTTPProxy(1, port);

		supplierProxyPorts.put(1, port);

		// Create the first OrderManager server and proxy
		orderManagerProcess1 = ItemSupplierUtility.startProcess(
				OrderManagerHTTPServer.class, true, Integer.toString(++port));
		orderManager1 = new OrderManagerHTTPProxy(0, port, supplierProxyPorts);

		// Create the second OrderManager server and proxy
		orderManagerProcess2 = ItemSupplierUtility.startProcess(
				OrderManagerHTTPServer.class, true, Integer.toString(++port));
		orderManager2 = new OrderManagerHTTPProxy(1, port, supplierProxyPorts);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (orderManager1 != null) {
			orderManager1.stopItemSupplierProxies();
			if (orderManager1 instanceof OrderManagerHTTPProxy) {
				((OrderManagerHTTPProxy) orderManager1).stop();
			}
		}
		if (orderManagerProcess1 != null) {
			ItemSupplierUtility.stopProcess(orderManagerProcess1);
		}

		if (orderManager2 != null) {
			orderManager2.stopItemSupplierProxies();
			if (orderManager2 instanceof OrderManagerHTTPProxy) {
				((OrderManagerHTTPProxy) orderManager2).stop();
			}
		}
		if (orderManagerProcess2 != null) {
			ItemSupplierUtility.stopProcess(orderManagerProcess2);
		}

		if (itemSupplier1 != null) {
			if (itemSupplier1 instanceof ItemSupplierHTTPProxy) {
				((ItemSupplierHTTPProxy) itemSupplier1).stop();
			}
		}
		if (itemSupplierProcess1 != null) {
			ItemSupplierUtility.stopProcess(itemSupplierProcess1);
		}

		if (itemSupplier2 != null) {
			if (itemSupplier2 instanceof ItemSupplierHTTPProxy) {
				((ItemSupplierHTTPProxy) itemSupplier2).stop();
			}
		}
		if (itemSupplierProcess2 != null) {
			ItemSupplierUtility.stopProcess(itemSupplierProcess2);
		}
	}

	@Test
	public void testFailureHandling() {

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();
		List<StepStatus> stepStatus = new ArrayList<OrderManager.StepStatus>();
		Map<Integer, List<StepStatus>> orderManager1State = new HashMap<Integer, List<StepStatus>>();
		Map<Integer, List<StepStatus>> orderManager2State = new HashMap<Integer, List<StepStatus>>();
		Integer workflowID;
		Integer supplierID1 = itemSupplier1.getSupplierID();
		Integer supplierID2 = itemSupplier2.getSupplierID();

		// 1. Setup initial state of all components

		// Initialize all the components (all to the same state to simplify the
		// test)
		items.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(0, 10));
		stepStatus.add(StepStatus.SUCCESSFUL);

		// Register the workflow at the first OrderManager
		List<OrderStep> workflow = new ArrayList<OrderStep>();
		workflow.add(new OrderStep(supplierID1, items));
		workflowID = TestUtility.registerOrderWorkflow(orderManager1, workflow);
		TestUtility.waitForJobsToFinish(orderManager1);
		orderManager1State.put(workflowID, stepStatus);

		// Register the workflow at the second OrderManager
		workflow = new ArrayList<OrderStep>();
		workflow.add(new OrderStep(supplierID2, items));
		workflowID = TestUtility.registerOrderWorkflow(orderManager2, workflow);
		TestUtility.waitForJobsToFinish(orderManager2);
		orderManager2State.put(workflowID, stepStatus);

		// Assert the initial state
		// - First the suppliers
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier1,
						TestUtility.extractItemIds(localList)));
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier2,
						TestUtility.extractItemIds(localList)));

		// - Then the OrderManagers
		for (Integer workflowIDTemp : orderManager1State.keySet()) {
			assertEquals(orderManager1State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager1,
							workflowIDTemp));
		}

		for (Integer workflowIDTemp : orderManager2State.keySet()) {
			assertEquals(orderManager2State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager2,
							workflowIDTemp));
		}

		// 2. Now shut down one of the ItemSupplier
		if (itemSupplier2 instanceof ItemSupplierHTTPProxy) {
			((ItemSupplierHTTPProxy) itemSupplier2).stop();
		}
		itemSupplier2 = null;
		ItemSupplierUtility.stopProcess(itemSupplierProcess2);
		itemSupplierProcess2 = null;

		// Then assert the state of the rest of the components
		// - First the suppliers
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier1,
						TestUtility.extractItemIds(localList)));

		// - Then the OrderManagers
		for (Integer workflowIDTemp : orderManager1State.keySet()) {
			assertEquals(orderManager1State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager1,
							workflowIDTemp));
		}

		for (Integer workflowIDTemp : orderManager2State.keySet()) {
			assertEquals(orderManager2State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager2,
							workflowIDTemp));
		}

		// 3. Now progress the components to next state to show that they are
		// still working
		items.clear();
		stepStatus = new ArrayList<OrderManager.StepStatus>();

		items.add(new ItemQuantity(1, 10));
		localList.add(new ItemQuantity(1, 10));
		stepStatus.add(StepStatus.SUCCESSFUL);

		// Register the workflow at the first OrderManager
		workflow = new ArrayList<OrderStep>();
		workflow.add(new OrderStep(supplierID1, items));
		workflowID = TestUtility.registerOrderWorkflow(orderManager1, workflow);
		TestUtility.waitForJobsToFinish(orderManager1);
		orderManager1State.put(workflowID, stepStatus);

		stepStatus = new ArrayList<OrderManager.StepStatus>();
		stepStatus.add(StepStatus.FAILED);

		// Register the workflow at the second OrderManager
		workflow = new ArrayList<OrderStep>();
		workflow.add(new OrderStep(supplierID2, items));
		workflowID = TestUtility.registerOrderWorkflow(orderManager2, workflow);
		TestUtility.waitForJobsToFinish(orderManager2);
		orderManager2State.put(workflowID, stepStatus);

		// Then assert that they are in the next state
		// - First the ItemSupplier
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier1,
						TestUtility.extractItemIds(localList)));

		// - Then the OrderManagers
		for (Integer workflowIDTemp : orderManager1State.keySet()) {
			assertEquals(orderManager1State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager1,
							workflowIDTemp));
		}

		for (Integer workflowIDTemp : orderManager2State.keySet()) {
			assertEquals(orderManager2State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager2,
							workflowIDTemp));
		}

		// 4. Now shut down one of the OrderManagers
		orderManager2.stopItemSupplierProxies();
		if (orderManager2 instanceof OrderManagerHTTPProxy) {
			((OrderManagerHTTPProxy) orderManager2).stop();
		}
		orderManager2 = null;

		ItemSupplierUtility.stopProcess(orderManagerProcess2);
		orderManagerProcess2 = null;

		// Then assert the rest of the components
		// - First the ItemSupplier
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier1,
						TestUtility.extractItemIds(localList)));

		// - Then the OrderManager
		for (Integer workflowIDTemp : orderManager1State.keySet()) {
			assertEquals(orderManager1State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager1,
							workflowIDTemp));
		}

		// 5. Now progress to the last state
		items.clear();
		stepStatus = new ArrayList<OrderManager.StepStatus>();

		items.add(new ItemQuantity(2, 355));
		localList.add(new ItemQuantity(2, 355));
		stepStatus.add(StepStatus.SUCCESSFUL);

		// Register the workflow at the first OrderManager
		workflow = new ArrayList<OrderStep>();
		workflow.add(new OrderStep(supplierID1, items));
		workflowID = TestUtility.registerOrderWorkflow(orderManager1, workflow);
		TestUtility.waitForJobsToFinish(orderManager1);
		orderManager1State.put(workflowID, stepStatus);

		// And make sure they are able to reach the last state
		// and conclude that failing components do that ruin the life of the
		// others
		// - First the ItemSupplier
		assertEquals(
				localList,
				TestUtility.getOrdersPerItem(itemSupplier1,
						TestUtility.extractItemIds(localList)));

		// - Then the OrderManager
		for (Integer workflowIDTemp : orderManager1State.keySet()) {
			assertEquals(orderManager1State.get(workflowIDTemp),
					TestUtility.getOrderWorkflowStatus(orderManager1,
							workflowIDTemp));
		}

		// TODO
		// 6. Bonus: Try and revive the dead components and see that they are
		// now working again.
	}
}
