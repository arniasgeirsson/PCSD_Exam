package com.acertainsupplychain.utility;

/**
 * This is wrapper class around a object to allow for all result objects to have
 * the same type when communicating through HTTP.
 * 
 * @author Arni
 * 
 */
public class ItemSupplierResult {
	private final Object result;

	/**
	 * Initialize the result object with some object.
	 * 
	 * @param result
	 */
	public ItemSupplierResult(Object result) {
		this.result = result;
	}

	/**
	 * Get the object stored in this result instance.
	 * 
	 * @return
	 */
	public Object getResult() {
		return result;
	}
}
