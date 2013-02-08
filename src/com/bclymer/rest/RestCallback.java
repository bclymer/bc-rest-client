package com.bclymer.rest;


public class RestCallback<T> {

	/**
	 * Called when the status code of the http request is 200-299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public void onSuccess(Response<T> response) {}
	
	/**
	 * Called when an exception is thrown of the http status code is < 200 or > 299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public void onFailure(Response<T> response) {}
	
}