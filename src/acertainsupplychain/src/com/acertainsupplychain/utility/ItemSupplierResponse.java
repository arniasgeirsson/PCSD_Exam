package com.acertainsupplychain.utility;

import com.acertainsupplychain.OrderProcessingException;

/**
 * This class is a datastructure that is used to communicate result and error
 * messages from the server to the client.
 * 
 * This class is greatly inspired by the course assignments.
 */
public class ItemSupplierResponse {
	private OrderProcessingException exception = null;
	private ItemSupplierResult result = null;

	/**
	 * Initialize an empty response.
	 */
	public ItemSupplierResponse() {
	}

	/**
	 * Initialize a response with the given exception and result.
	 * 
	 * @param exception
	 * @param result
	 */
	public ItemSupplierResponse(OrderProcessingException exception,
			ItemSupplierResult result) {
		setException(exception);
		setResult(result);
	}

	/**
	 * Get the current exception stored in this object.
	 * 
	 * @return
	 */
	public OrderProcessingException getException() {
		return exception;
	}

	/**
	 * Update the exception stored in this object.
	 * 
	 * @param exception
	 */
	public void setException(OrderProcessingException exception) {
		this.exception = exception;
	}

	/**
	 * Get the current result stored in this object.
	 * 
	 * @return
	 */
	public ItemSupplierResult getResult() {
		return result;
	}

	/**
	 * Update the result stored in this object.
	 * 
	 * @param exception
	 */
	public void setResult(ItemSupplierResult result) {
		this.result = result;
	}

}
