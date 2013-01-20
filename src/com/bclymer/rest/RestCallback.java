package com.bclymer.rest;


public abstract class RestCallback<T> {

	public abstract void onSuccess(Response<T> r);
	public abstract void onFailure(Response<T> r);
	
}