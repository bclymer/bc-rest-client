package com.bclymer.rest;

import org.apache.http.Header;

public class RestClientResponse<T> {
	
	public T response;
	public int httpStatusCode;
	public int errorCode;
	public String rawResponse;
	public Header[] headers;

	public static class ErrorCodes {
		public static final int NONE = 0;
		public static final int CAST_ERROR = 1;
		public static final int NETWORK_ERROR_BAD_STATUS_CODE = 2;
		public static final int NETWORK_ERROR_UNKNOWN = 3;
		public static final int REQUEST_CANCELLED = 3;
	}
}
