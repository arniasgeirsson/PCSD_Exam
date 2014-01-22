package com.acertainsupplychain.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.acertainsupplychain.ItemQuantity;
import com.acertainsupplychain.ItemSupplier;
import com.acertainsupplychain.OrderStep;
import com.acertainsupplychain.impl.ItemSupplierImpl;
import com.acertainsupplychain.performance.PerformanceLogImpl;
import com.acertainsupplychain.performance.PerformanceLogger;
import com.acertainsupplychain.utility.TestUtility;

/**
 * This JUnit test class is used to test the performance of my ItemSupplierImpl
 * implementation and how well it scales with concurrency.
 * 
 * @author Arni
 * 
 */
public class Experiment {

	@Test
	public void testScaleNumberOfOrderManagers() {
		PerformanceLogger logger = new PerformanceLogger();
		ItemSupplier supplier = new ItemSupplierImpl(0);
		Map<Integer, ItemSupplier> supplierMap = new HashMap<Integer, ItemSupplier>();
		supplierMap.put(supplier.getSupplierID(), supplier);
		List<Future<Long>> futures = new ArrayList<Future<Long>>();
		List<OrderManagerThread> threads = null;

		// Scale this number to scale the number of OrderManagers.
		int numberOfOrderManagers = 2000;

		ExecutorService executor = Executors
				.newFixedThreadPool(numberOfOrderManagers);

		List<OrderStep> workflow = new ArrayList<OrderStep>();
		List<ItemQuantity> items = new ArrayList<ItemQuantity>();
		items.add(new ItemQuantity(0, 10));
		items.add(new ItemQuantity(1, 10));
		items.add(new ItemQuantity(2, 10));
		items.add(new ItemQuantity(3, 10));
		workflow.add(new OrderStep(supplier.getSupplierID(), items));
		workflow.add(new OrderStep(supplier.getSupplierID(), items));
		workflow.add(new OrderStep(supplier.getSupplierID(), items));
		workflow.add(new OrderStep(supplier.getSupplierID(), items));

		Integer numberOfClients = null;
		Integer numberOfOps = null;
		Integer numberOfExecuteSteps = null;
		Integer numberOfGetOrdersPerItem = 0;
//		Long totalRunTimeInNS = 0l;
		Integer numberOfDifItemIDs = TestUtility.extractItemIds(items).size();

		Long totalTestTimeInNS = 0l;
		
		futures.clear();
		numberOfClients = numberOfOrderManagers;
		numberOfOps = numberOfClients * workflow.size();
		numberOfExecuteSteps = numberOfOps;

		// Now create all the workers/OrderManagers
		threads = createOrderManagersThreads(numberOfClients, supplier,
				workflow);

		// Now 'start' them
		totalTestTimeInNS = System.nanoTime();
		for (OrderManagerThread thread : threads) {
			futures.add(executor.submit(thread));
		}

		// Now wait for them to be finished
		for (Future<Long> futureResult : futures) {
			try {
//				totalRunTimeInNS += futureResult.get();
				futureResult.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		totalTestTimeInNS = System.nanoTime() - totalTestTimeInNS;
		
		// Log the result
		PerformanceLogImpl log = new PerformanceLogImpl(numberOfClients,
				numberOfOps, numberOfExecuteSteps, numberOfGetOrdersPerItem,
				totalTestTimeInNS, numberOfDifItemIDs);
		logger.writeLog(log);
	}

	/**
	 * Creates a list of a given size filled with new OrderManagerThread
	 * instances.
	 * 
	 * @param amount
	 * @param supplier
	 * @param workflow
	 * @return
	 */
	private List<OrderManagerThread> createOrderManagersThreads(int amount,
			ItemSupplier supplier, List<OrderStep> workflow) {
		List<OrderManagerThread> threads = new ArrayList<Experiment.OrderManagerThread>();
		for (int i = 0; i < amount; i++) {
			threads.add(new OrderManagerThread(supplier, workflow));
		}
		return threads;
	}

	private static class OrderManagerThread implements Callable<Long> {

		ItemSupplier supplier;
		List<OrderStep> workflow;

		private OrderManagerThread(ItemSupplier supplier,
				List<OrderStep> workflow) {
			this.supplier = supplier;
			this.workflow = workflow;
		}

		@Override
		public Long call() throws Exception {
			long startTimeInNS = 0;
			long endTimeInNS = 0;
			try {
				startTimeInNS = System.nanoTime();
				for (OrderStep orderStep : workflow) {
					supplier.executeStep(orderStep);
				}
				endTimeInNS = System.nanoTime();
			} catch (Exception e) {
				// Should not happen, test does not count.
				e.printStackTrace();
			}
			return endTimeInNS - startTimeInNS;
		}
	}
}
