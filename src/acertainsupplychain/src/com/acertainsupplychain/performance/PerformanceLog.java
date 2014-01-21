package com.acertainsupplychain.performance;

/**
 * This is an interface that allows any object to be logged with the
 * PerformanceLogger class.
 * 
 * @author Arni
 * 
 */
public interface PerformanceLog {

	/**
	 * This function must return a list of objects to be written into a row in a
	 * excel document. The list must either contain strings and/or integers.
	 * 
	 * @return the row represented as a list.
	 */
	public Object[] toList();
}
