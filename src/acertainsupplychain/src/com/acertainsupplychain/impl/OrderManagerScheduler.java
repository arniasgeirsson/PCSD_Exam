package com.acertainsupplychain.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainsupplychain.OrderManager;
import com.acertainsupplychain.clients.ItemSupplierClientConstants;

public class OrderManagerScheduler {

	private final ExecutorService executor;
	private final List<Future<?>> futures;

	public OrderManagerScheduler() {
		futures = new ArrayList<Future<?>>();
		executor = Executors
				.newFixedThreadPool(ItemSupplierClientConstants.ORDERMANAGER_MAX_THREADSPOOL_SIZE);
	}

	public boolean scheduleJob(OrderManager orderManager, int workflowID) {
		Runnable job = new OrderManagerJob(orderManager, workflowID);
		// TODO sometimes execute is executed in same caller thread? ie not
		// async
		futures.add(executor.submit(job));
		return true;
	}

	public void shutDown() {
		executor.shutdown();
		while (!executor.isTerminated()) {
			;
		}
	}

	public void waitForJobsToFinish() throws InterruptedException,
			ExecutionException {
		for (Future<?> futureResult : futures) {
			futureResult.get();
		}
		futures.clear();
	}
}
