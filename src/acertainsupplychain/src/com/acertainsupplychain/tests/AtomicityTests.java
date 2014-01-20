package com.acertainsupplychain.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.acertainsupplychain.InvalidItemException;
import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.impl.ItemSupplierImpl;
import com.acertainsupplychain.utility.TestUtility;

public class AtomicityTests {

	@Test
	public void test() {
		ItemSupplier supplier = new ItemSupplierImpl(0);

		int numThreads = 20;
		int numRuns = 1000;
		int singleItemID = 1;

		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(singleItemID, 10));
		items.add(new ItemQuantity(singleItemID, 7));
		items.add(new ItemQuantity(singleItemID, 13));
		items.add(new ItemQuantity(singleItemID, 13));
		items.add(new ItemQuantity(singleItemID, 13));
		items.add(new ItemQuantity(singleItemID, 13));
		OrderStep step = new OrderStep(supplier.getSupplierID(), items);
		Set<Integer> itemIDSet = TestUtility.extractItemIds(items);

		int total = 0;
		for (ItemQuantity item : items) {
			total += item.getQuantity();
		}

		// Create the Runner threads
		List<Runner> runners = new ArrayList<AtomicityTests.Runner>();
		for (int i = 0; i < numThreads; i++) {
			runners.add(new Runner(step, supplier, numRuns));
		}

		// Create
		Checker checker = new Checker(total, itemIDSet, supplier);

		// Start them
		for (Runner runner : runners) {
			runner.start();
		}

		checker.start();

		// Then wait for them to finish
		for (Runner runner : runners) {
			try {
				runner.join();
			} catch (Exception e) {
				e.printStackTrace();
				checker.interrupt();
				fail();
			}
		}

		checker.interrupt();

		assertFalse(checker.getFailed());

		int expectedEndAmount = numThreads * numRuns * total;
		List<ItemQuantity> localList = new ArrayList<ItemQuantity>();
		localList.add(new ItemQuantity(singleItemID, expectedEndAmount));

		assertEquals(localList,
				TestUtility.getOrdersPerItem(supplier, itemIDSet));
	}

	private class Runner extends Thread {

		private final OrderStep step;
		private final ItemSupplier supplier;
		private final int runs;

		public Runner(OrderStep step, ItemSupplier supplier, int runs) {
			this.step = step;
			this.supplier = supplier;
			this.runs = runs;
		}

		@Override
		public void run() {

			for (int i = 0; i < runs; i++) {
				try {
					supplier.executeStep(step);
				} catch (Exception e) {
					// Should not happen
					e.printStackTrace();
				}
			}
		}

	}

	private class Checker extends Thread {

		private final int divider;
		private final ItemSupplier supplier;
		private final Set<Integer> itemID;

		private boolean failed;

		public Checker(int divider, Set<Integer> itemID, ItemSupplier supplier) {
			this.divider = divider;
			this.itemID = itemID;
			this.supplier = supplier;
			failed = false;
		}

		public boolean getFailed() {
			return failed;
		}

		@Override
		public void run() {

			while (!isInterrupted()) {
				try {
					List<ItemQuantity> list = supplier.getOrdersPerItem(itemID);
					int amount = list.get(0).getQuantity();
					if (amount != 0 && amount % divider != 0) {
						System.out.println("Failed! The amount given is ["
								+ amount + "] but it was expected that the "
								+ "amount is a multiplum of [" + divider + "]");
						failed = true;
					}
				} catch (InvalidItemException e) {
					// If this thread is too fast it might get
					// InvalidItemException saying that the supplier has no
					// record of the provided item ID.
					// But that is okay, we just ignore that.
				} catch (Exception e) {
					// Should not happen
					e.printStackTrace();
				}
			}
			System.out.println("Checker finished!");
		}
	}
}
