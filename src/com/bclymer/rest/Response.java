package com.bclymer.rest;

public class Response<T> {
	
	public T response;
	public int statusCode;
	public int errorCode;
	public String rawResponse;

	public static class ErrorCodes {
		public static final int NONE = 0;
		public static final int CAST_ERROR = 1;
		public static final int NETWORK_ERROR = 2;
	}
	
}
