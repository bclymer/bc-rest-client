package com.bclymer.rest;

import org.apache.http.Header;

public class RestClientResponse<T> {
	
	public T response;
	public int httpStatusCode;
	public ErrorCode errorCode;
	public String rawResponse;
	public Header[] headers;

	public enum ErrorCode {
		
		NONE(0),
		CAST_ERROR(1),
		NETWORK_ERROR_BAD_STATUS_CODE(2),
		NETWORK_ERROR_UNKNOWN(3),
		REQUEST_CANCELLED(4);
		
		public int value;
		
		ErrorCode(int value) {
			this.value = value;
		}
	}
}
