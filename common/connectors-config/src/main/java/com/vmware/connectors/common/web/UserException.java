package com.vmware.connectors.common.web;

public class UserException extends RuntimeException {

	/**
	 * Exception to indicate a resource requested by the user was not found on the server.
	 */
	private static final long serialVersionUID = 1L;

	public UserException(String message) {
		super(message);

	}

}
