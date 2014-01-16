package com.acertainsupplychain.utility;

import com.acertainsupplychain.OrderProcessingException;

// TODO inspired by the course assignments

/**
 * TODO Data Structure that we use to communicate objects and error messages
 * from the server to the client.
 * 
 */
public class ItemSupplierResponse {
	private OrderProcessingException exception = null;
	private ItemSupplierResult result = null;

	public ItemSupplierResponse() {

	}

	public ItemSupplierResponse(OrderProcessingException exception,
			ItemSupplierResult result) {
		setException(exception);
		setResult(result);
	}

	public OrderProcessingException getException() {
		return exception;
	}

	public void setException(OrderProcessingException exception) {
		this.exception = exception;
	}

	public ItemSupplierResult getResult() {
		return result;
	}

	public void setResult(ItemSupplierResult result) {
		this.result = result;
	}

}
