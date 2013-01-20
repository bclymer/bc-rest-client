package com.bclymer.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

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
	
	public <T> T get(String url, long id, Class<T> clazz, RestCallback<T> callback) {
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, new HttpGet(url), clazz);
		mTasks.put(id, d);
		d.execute();
		return (T) null;
	}

	public <T> T post(String url) {
		return (T) null;
	}

	public <T> T put(String url) {
		return (T) null;
	}

	public <T> T delete(String url) {
		
		return (T) null;
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
	            response.statusCode = statusLine.getStatusCode();
	            if(response.statusCode == HttpStatus.SC_OK){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                httpResponse.getEntity().writeTo(out);
	                out.close();
	                response.rawResponse = out.toString();
	                response.response = (T) mMapper.readValue(response.rawResponse, clazz);
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
