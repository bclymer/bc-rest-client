package com.bclymer.rest;

import org.apache.http.Header;

public class Response<T> {
	
	public T response;
	public int httpStatusCode;
	public int runningStatus;
	public int errorCode;
	public String rawResponse;
	public Header[] headers;

	public static class ErrorCodes {
		public static final int NONE = 0;
		public static final int CAST_ERROR = 1;
		public static final int NETWORK_ERROR = 2;
	}
	
	public static class RunningStatusCodes {
		public static final int WAITING = 0;
		public static final int RUNNING = 1;
		public static final int FINISHED = 2;
	}
	
	
}
