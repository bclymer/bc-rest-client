package com.bclymer.rest;


public abstract class BcRestClientCallback<T> {
	
	/**
	 * Called as soon as the async request is made by the user.
	 */
	public void onPreExecute() {}
	
	/**
	 * Called when the request is finished, whether successful or not.
	 */
	public void onPostExecute() {}

	/**
	 * Called when the status code of the http request is 200-299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public void onSuccess(BcRestClientResponse<T> response) {}
	
	/**
	 * Called when an exception is thrown of the http status code is < 200 or > 299.
	 * @param response The response object containing the headers, status code, and response.
	 */
	public void onFailure(BcRestClientResponse<T> response) {}
	
}