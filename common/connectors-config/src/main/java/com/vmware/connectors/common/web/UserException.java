package com.vmware.connectors.common.web;

import org.springframework.http.HttpStatus;

public class UserException extends RuntimeException {

	/**
	 * Exception to indicate a resource requested by the user was not found on the
	 * server.
	 */
	private static final long serialVersionUID = 1L;

	private HttpStatus statusCode;
	private String message;

	public UserException(String message, HttpStatus statusCode) {
		super();
		this.message = message;
		this.statusCode = statusCode;

	}
	
	public UserException(String message) {
		super(message);
	}

	public HttpStatus getStatus() {
		return statusCode;
	}

	public void setStatus(HttpStatus status) {
		this.statusCode = status;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
