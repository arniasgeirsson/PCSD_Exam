package com.acertainsupplychain.performance;

/**
 * This class in an implementation of the PerformanceLog interface and is used
 * to log performance output during the experiment test.
 * 
 * @author Arni
 * 
 */
public class PerformanceLogImpl implements PerformanceLog {

	private final int numberOfClients;
	private final int numberOfOps;
	private final int numberOfExecuteSteps;
	private final int numberOfGetOrdersPerItem;
	private final long totalRunTimeInNS;
	private final int numberOfDifItemIDs;

	/**
	 * Create a new PerformanceLogImpl instance with the given values.
	 * 
	 * @param numberOfClients
	 * @param numberOfOps
	 * @param numberOfExecuteSteps
	 * @param numberOfGetOrdersPerItem
	 * @param totalRunTimeInNS
	 * @param numberOfDifItemIDs
	 */
	public PerformanceLogImpl(int numberOfClients, int numberOfOps,
			int numberOfExecuteSteps, int numberOfGetOrdersPerItem,
			long totalRunTimeInNS, int numberOfDifItemIDs) {
		this.numberOfClients = numberOfClients;
		this.numberOfOps = numberOfOps;
		this.numberOfExecuteSteps = numberOfExecuteSteps;
		this.numberOfGetOrdersPerItem = numberOfGetOrdersPerItem;
		this.totalRunTimeInNS = totalRunTimeInNS;
		this.numberOfDifItemIDs = numberOfDifItemIDs;
	}

	@Override
	public Object[] toList() {
		long timeInMS = totalRunTimeInNS / 1000000;
		float percentExec = ((float) numberOfExecuteSteps / (float) numberOfOps);
		float percentGet = ((float) numberOfGetOrdersPerItem / (float) numberOfOps);

		return new Object[] {
				numberOfClients,
				numberOfOps,
				Float.toString((float) numberOfOps / (float) numberOfClients),
				numberOfExecuteSteps,
				Float.toString(percentExec * 100),
				numberOfGetOrdersPerItem,
				Float.toString(percentGet * 100),
				Float.toString((totalRunTimeInNS * percentExec)
						/ numberOfExecuteSteps),
				Float.toString((timeInMS * percentExec) / numberOfExecuteSteps),
				Float.toString((totalRunTimeInNS * percentGet)
						/ numberOfGetOrdersPerItem),
				Float.toString((timeInMS * percentGet)
						/ numberOfGetOrdersPerItem), numberOfDifItemIDs,
				Float.toString((float) totalRunTimeInNS / (float) numberOfOps),
				Float.toString((float) numberOfOps / (float) totalRunTimeInNS),
				Float.toString((float) timeInMS / (float) numberOfOps),
				Float.toString((float) numberOfOps / (float) timeInMS),
				Long.toString(totalRunTimeInNS), Long.toString(timeInMS)

		};
	}
}
