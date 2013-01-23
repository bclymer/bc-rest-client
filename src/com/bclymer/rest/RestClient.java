package com.bclymer.rest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestClient {
	
	private final HttpClient mHttpClient = new DefaultHttpClient();
	private final ObjectMapper mMapper = new ObjectMapper();
	
	@SuppressWarnings("rawtypes")
	private Map<Long,DownloadWebSourceTask> mTasks = new HashMap<Long,DownloadWebSourceTask>();
	private Map<String,String> mDefaultHeaders = new HashMap<String,String>();

	private static final RestClient instance = new RestClient();
	 
    private RestClient() {}
 
    public static RestClient getInstance() {
        return instance;
    }
    
    public void addDefaultHeader(String key, String value) {
    	mDefaultHeaders.put(key, value);
    }
    
    public void removeDefaultHeader(String key, String value) {
    	mDefaultHeaders.remove(key);
    }
	
	public <T> void get(String url, long id, Class<T> clazz, RestCallback<T> callback) {
		get(url, id, clazz, callback, null, null);
	}
	
	public <T> void get(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpGet request = new HttpGet(url);
		if (headers != null) {
			for (Header h : headers) {
				request.addHeader(h);
			}
		}
		if (params != null) {
			request.setParams(params);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz, id);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void post(String url, long id, Class<T> clazz, RestCallback<T> callback) throws UnsupportedEncodingException, JsonProcessingException {
		post(url, id, clazz, callback, null, null, null);
	}
	
	public <T> void post(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params, Object body) throws UnsupportedEncodingException, JsonProcessingException {
		HttpPost request = new HttpPost(url);
		if (headers != null) {
			for (Header h : headers) {
				request.addHeader(h);
			}
		}
		if (params != null) {
			request.setParams(params);
		}
		if (body != null) {
			StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
			request.setEntity(bodyString);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz, id);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void put(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params, Object body) throws UnsupportedEncodingException, JsonProcessingException {
		HttpPut request = new HttpPut(url);
		if (headers != null) {
			for (Header h : headers) {
				request.addHeader(h);
			}
		}
		if (params != null) {
			request.setParams(params);
		}
		if (body != null) {
			StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
			request.setEntity(bodyString);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz, id);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void put(String url, long id, Class<T> clazz, RestCallback<T> callback) throws UnsupportedEncodingException, JsonProcessingException {
		put(url, id, clazz, callback, null, null, null);
	}

	public <T> void delete(String url, long id, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpDelete request = new HttpDelete(url);
		if (headers != null) {
			for (Header h : headers) {
				request.addHeader(h);
			}
		}
		if (params != null) {
			request.setParams(params);
		}
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz, id);
		mTasks.put(id, d);
		d.execute();
	}

	public <T> void delete(String url, long id, Class<T> clazz, RestCallback<T> callback) {
		delete(url, id, clazz, callback, null, null);
	}
	
	public boolean cancel(long id) {
		try {
			return mTasks.get(id).cancel(true);
		} catch (Exception e) {
			return false;
		}
	}
	
	private class DownloadWebSourceTask<T> extends AsyncTask<Void, Void, Response<T>> {

		private RestCallback<T> callback;
		private HttpUriRequest request;
		private Class<T> clazz;
		private boolean wasSuccess;
		private long id;
		
		public DownloadWebSourceTask(RestCallback<T> callback, HttpUriRequest request, Class<T> clazz, long id) {
			this.callback = callback;
			this.request = request;
			this.clazz = clazz;
			this.id = id;
		}
		
		@Override
		protected void onPreExecute() {
			for (Entry<String, String> entry : mDefaultHeaders.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}
		
		@Override
		protected Response<T> doInBackground(Void... params) {
			Response<T> response = new Response<T>();
			HttpResponse httpResponse;
	        try {
	            httpResponse = mHttpClient.execute(request);
	            StatusLine statusLine = httpResponse.getStatusLine();
	            response.httpStatusCode = statusLine.getStatusCode();
	            if(response.httpStatusCode / 100 == 2){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                httpResponse.getEntity().writeTo(out);
	                response.headers = httpResponse.getAllHeaders();
	                out.close();
	                response.rawResponse = out.toString();
	                try {
		                if (response.response instanceof Void == false && clazz != null) {
		                	response.response = (T) mMapper.readValue(response.rawResponse, clazz);
		                }
	                	wasSuccess = true;
	                } catch (Exception e) {
	                	response.errorCode = Response.ErrorCodes.CAST_ERROR;
	                	wasSuccess = false;
	                }
	            } else{
	                //Closes the connection.
	                httpResponse.getEntity().getContent().close();
	                response.errorCode = Response.ErrorCodes.NETWORK_ERROR_BAD_STATUS_CODE;
                	wasSuccess = false;
	            }
	        } catch (Exception e) {
                response.errorCode = Response.ErrorCodes.NETWORK_ERROR_UNKNOWN;
            	wasSuccess = false;
	        }
			return response;
		}
		
		@Override
		protected void onCancelled(Response<T> response) {
			if (response == null) {
				response = new Response<T>();
			}
			response.errorCode = Response.ErrorCodes.REQUEST_CANCELLED;
			callback.onFailure(response);
		}
		
		@Override
		protected void onPostExecute(Response<T> response) {
			if (wasSuccess) {
				callback.onSuccess(response);
			} else {
				callback.onFailure(response);
			}
			mTasks.remove(id);
		}
		
	}
}
