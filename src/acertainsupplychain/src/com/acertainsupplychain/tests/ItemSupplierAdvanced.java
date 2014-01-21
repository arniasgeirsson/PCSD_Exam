package com.acertainsupplychain.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderProcessingException;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.clients.ItemSupplierHTTPProxy;
import com.acertainsupplychain.server.ItemSupplierHTTPServer;
import com.acertainsupplychain.utility.ItemSupplierUtility;
import com.acertainsupplychain.utility.TestUtility;

/**
 * This JUnit test class is used to test that the server side and proxy of an
 * ItemSupplier works as intended.
 * 
 * @author Arni
 * 
 */
public class ItemSupplierAdvanced {

	private static ItemSupplier supplier;
	private static Process itemSupplierProcess;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		int port = 8010;
		itemSupplierProcess = ItemSupplierUtility.startProcess(
				ItemSupplierHTTPServer.class, true, Integer.toString(port));
		supplier = new ItemSupplierHTTPProxy(0, port);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		((ItemSupplierHTTPProxy) supplier).stop();
		ItemSupplierUtility.stopProcess(itemSupplierProcess);
	}

	@After
	public void tearDown() throws Exception {
		supplier.clear();
	}

	@Test
	public final void testGetOrdersPerItem_Valid() {
		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();
		List<ItemQuantity> summedItems = new ArrayList<ItemQuantity>();
		Set<Integer> itemIds = new HashSet<Integer>();
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();

		// 1. Test that an empty set provides an empty list in empty supplier
		supplier.clear();
		localList.clear();
		itemIds.clear();
		summedItems.clear();
		items.clear();

		try {
			summedItems = supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, summedItems);

		// 2. Test that an empty set still provides an empty list in a non-empty
		// supplier
		supplier.clear();
		localList.clear();
		itemIds.clear();
		summedItems.clear();
		items.clear();

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 20));
		items.add(new ItemQuantity(2, 30));

		TestUtility.executeStep(supplier,
				new OrderStep(supplier.getSupplierID(), items));

		try {
			summedItems = supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, summedItems);

		// 3. Test that with 3 items in stock only the correct one is returned
		// when calling with one
		localList.clear();
		itemIds.clear();
		summedItems.clear();
		items.clear();

		itemIds.add(0);
		localList.add(new ItemQuantity(0, 10));

		try {
			summedItems = supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, summedItems);

		// 4. Test that all three items are returned if calling with all 3
		localList.clear();
		itemIds.clear();
		summedItems.clear();
		items.clear();

		itemIds.add(0);
		itemIds.add(1);
		itemIds.add(2);
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 20));
		localList.add(new ItemQuantity(2, 30));

		try {
			summedItems = supplier.getOrdersPerItem(itemIds);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, summedItems);
	}

	@Test
	public final void testGetOrdersPerItem_NullSet() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		Set<Integer> itemIds2 = null;

		try {
			supplier.getOrdersPerItem(itemIds2);
			fail();
		} catch (InvalidItemException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testGetOrdersPerItem_NullInteger() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		Set<Integer> itemIds2 = new HashSet<Integer>();
		itemIds2.addAll(itemIds);
		itemIds2.add(null);

		try {
			supplier.getOrdersPerItem(itemIds2);
			fail();
		} catch (InvalidItemException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testGetOrdersPerItem_ItemDoesNotExist() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		Set<Integer> itemIds2 = new HashSet<Integer>();
		Integer wrongId = 5;
		assertFalse(itemIds.contains(wrongId));
		itemIds2.add(wrongId);

		try {
			supplier.getOrdersPerItem(itemIds2);
			fail();
		} catch (InvalidItemException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_Valid() {
		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		Set<Integer> itemIds = new HashSet<Integer>();

		OrderStep step = null;

		// 1. Try to add a simple quantity
		supplier.clear();
		items.clear();
		localList.clear();

		items.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(0, 10));
		itemIds = TestUtility.extractItemIds(localList);

		step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		// 2. Try to add two different ones at one time
		supplier.clear();
		items.clear();
		localList.clear();

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 20));
		localList.add(new ItemQuantity(0, 10));
		localList.add(new ItemQuantity(1, 20));
		itemIds = TestUtility.extractItemIds(localList);

		step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		// 3. Try to add two same ones at one time
		supplier.clear();
		items.clear();
		localList.clear();

		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(0, 20));
		localList.add(new ItemQuantity(0, 30));
		itemIds = TestUtility.extractItemIds(localList);

		step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		// 4. Try to add item with negative item id to show that it is allowed
		supplier.clear();
		items.clear();
		localList.clear();

		items.add(new ItemQuantity(-5, 10));
		localList.add(new ItemQuantity(-5, 10));
		itemIds = TestUtility.extractItemIds(localList);

		step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
		} catch (Exception e) {
			fail();
		}

		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	// NOTE When checking that the state of the supplier is the same both before
	// and after an exception I check by making sure that the quantities I have
	// added before are the same as after the exception, and wont check that
	// nothing else have slipped in.

	@Test
	public final void testExecuteStep_NullOrderStep() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		OrderStep step = null;
		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_NegativeQuantity() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, -50));

		OrderStep step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_WrongID() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		int wrongID = supplier.getSupplierID() + 1;
		assertTrue(wrongID != supplier.getSupplierID());

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));

		OrderStep step = new OrderStep(wrongID, items);

		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_NullList() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		List<ItemQuantity> items = null;
		OrderStep step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_NullItem() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));
		items.add(null);
		items.add(new ItemQuantity(1, 6));

		OrderStep step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

	@Test
	public final void testExecuteStep_EmptyItems() {
		// Initialize the state of the supplier pre exception and make sure it
		// is in the state we expect
		List<ItemQuantity> localList = TestUtility
				.setUpPreExceptionSupplierState(supplier);
		Set<Integer> itemIds = TestUtility.extractItemIds(localList);
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();

		OrderStep step = new OrderStep(supplier.getSupplierID(), items);

		try {
			supplier.executeStep(step);
			fail();
		} catch (OrderProcessingException e) {
		} catch (Exception e) {
			fail();
		}

		// Make sure that the state of the supplier is the same as before the
		// exception occurred
		assertEquals(localList, TestUtility.getOrdersPerItem(supplier, itemIds));
	}

}
