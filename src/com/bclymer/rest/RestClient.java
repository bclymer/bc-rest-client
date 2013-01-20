package com.bclymer.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestClient {
	
	private final HttpClient mHttpClient = new DefaultHttpClient();
	private final ObjectMapper mMapper = new ObjectMapper();
	
	@SuppressWarnings("rawtypes")
	private Map<Long,DownloadWebSourceTask> mTasks = new HashMap<Long,DownloadWebSourceTask>();

	private static final RestClient instance = new RestClient();
	 
    private RestClient() {}
 
    public static RestClient getInstance() {
        return instance;
    }
	
	public <T> void get(String url, long id, Class<T> clazz, RestCallback<T> callback) {
		get(url, id, clazz, callback, null, null);
	}
	
	public <T> void get(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpGet request = new HttpGet(url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		if (params != null) {
			request.setParams(params);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void post(String url, long id, Class<T> clazz, RestCallback<T> callback) throws UnsupportedEncodingException, JsonProcessingException {
		post(url, id, clazz, callback, null, null, null);
	}
	
	public <T> void post(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params, Object body) throws UnsupportedEncodingException, JsonProcessingException {
		HttpPost request = new HttpPost(url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		if (params != null) {
			request.setParams(params);
		}
		if (body != null) {
			StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
			request.setEntity(bodyString);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void put(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params, Object body) throws UnsupportedEncodingException, JsonProcessingException {
		HttpPut request = new HttpPut(url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		if (params != null) {
			request.setParams(params);
		}
		if (body != null) {
			StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
			request.setEntity(bodyString);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void put(String url, long id, Class<T> clazz, RestCallback<T> callback) throws UnsupportedEncodingException, JsonProcessingException {
		put(url, id, clazz, callback, null, null, null);
	}

	public <T> void delete(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpDelete request = new HttpDelete(url);
		if (headers != null) {
			request.setHeaders(headers);
		}
		if (params != null) {
			request.setParams(params);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void delete(String url, long id, Class<T> clazz, RestCallback<T> callback) {
		delete(url, id, clazz, callback, null, null);
	}
	
	private class DownloadWebSourceTask<T> extends AsyncTask<Void, Void, Response<T>> {

		private RestCallback<T> callback;
		private HttpUriRequest request;
		private Class<T> clazz;
		
		public DownloadWebSourceTask(RestCallback<T> callback, HttpUriRequest request, Class<T> clazz) {
			this.callback = callback;
			this.request = request;
			this.clazz = clazz;
		}
		
		@Override
		protected Response<T> doInBackground(Void... params) {
			Response<T> response = new Response<T>();
			HttpResponse httpResponse;
	        try {
	            httpResponse = mHttpClient.execute(request);
	            StatusLine statusLine = httpResponse.getStatusLine();
	            response.httpStatusCode = statusLine.getStatusCode();
	            if(response.httpStatusCode >= 200 && response.httpStatusCode <= 202){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                httpResponse.getEntity().writeTo(out);
	                response.headers = httpResponse.getAllHeaders();
	                out.close();
	                response.rawResponse = out.toString();
	                if (response.response instanceof Void == false && clazz != null) {
	                	Log.e("", "Doing it");
	                	response.response = (T) mMapper.readValue(response.rawResponse, clazz);
	                }
	                callback.onSuccess(response);
	            } else{
	                //Closes the connection.
	                httpResponse.getEntity().getContent().close();
	                throw new IOException(statusLine.getReasonPhrase());
	            }
	        } catch (ClientProtocolException e) {
	        	e.printStackTrace();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
			return null;
		}
		
	}
}
