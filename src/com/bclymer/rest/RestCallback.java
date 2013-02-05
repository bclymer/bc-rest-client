package com.bclymer.rest;


public abstract class RestCallback<T> {

	/**
	 * Called when the status code of the http request is 200-299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public abstract void onSuccess(Response<T> response);
	
	/**
	 * Called when an exception is thrown of the http status code is < 200 or > 299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public abstract void onFailure(Response<T> response);
	
}