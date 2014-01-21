package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.clients.ItemSupplierClientConstants;

/**
 * This class acts as a simple wrapper around a ExecutorService and acts as a
 * thread pool.
 * 
 * @author Arni
 * 
 */
public class OrderManagerScheduler {

	private final ExecutorService executor;
	private final List<Future<?>> futures;

	/**
	 * Initialize a scheduler object.
	 */
	public OrderManagerScheduler() {
		futures = new ArrayList<Future<?>>();
		executor = Executors
				.newFixedThreadPool(ItemSupplierClientConstants.ORDERMANAGER_MAX_THREADSPOOL_SIZE);
	}

	/**
	 * This function submits a new processing task based on the given
	 * OrderManager and workflow ID and starts the new thread asynchronously.
	 * 
	 * @param orderManager
	 * @param workflowID
	 * @return
	 */
	public boolean scheduleJob(OrderManager orderManager, int workflowID) {
		Runnable job = new OrderManagerJob(orderManager, workflowID);
		futures.add(executor.submit(job));
		return true;
	}

	/**
	 * shutDown shuts down the thread pool and must be reallocated to be of use
	 * again.
	 */
	public void shutDown() {
		executor.shutdown();
		while (!executor.isTerminated()) {
			;
		}
	}

	/**
	 * Blocks the scheduler until all working threads are done.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void waitForJobsToFinish() throws InterruptedException,
			ExecutionException {
		for (Future<?> futureResult : futures) {
			futureResult.get();
		}
		futures.clear();
	}
}
