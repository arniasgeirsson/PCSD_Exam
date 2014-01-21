package com.acertainsupplychain;

/**
 * This exception flags that a network error occurred.
 * 
 * @author Arni
 * 
 */
@SuppressWarnings("serial")
public class NetworkException extends OrderProcessingException {

	/**
	 * Constructor based on Exception constructors.
	 */
	public NetworkException() {
		super();
	}

	/**
	 * Constructor based on Exception constructors.
	 */
	public NetworkException(String message) {
		super(message);
	}

	/**
	 * Constructor based on Exception constructors.
	 */
	public NetworkException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor based on Exception constructors.
	 */
	public NetworkException(Throwable ex) {
		super(ex);
	}

}
