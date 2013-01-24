package com.bclymer.rest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.SparseArray;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestClient {

	private final ObjectMapper mMapper = new ObjectMapper();
	private final AtomicInteger mCount = new AtomicInteger();

	@SuppressWarnings("rawtypes")
	private SparseArray<DownloadWebSourceTask> mTasks = new SparseArray<DownloadWebSourceTask>();
	private Map<String, String> mDefaultHeaders = new HashMap<String, String>();

	private DefaultHttpClient client;

	private static final RestClient instance = new RestClient();

	private RestClient() {
	}

	public static RestClient getInstance() {
		return instance;
	}

	/**
	 * Adds a header to every request the client makes.
	 * 
	 * @param key
	 *            Header key
	 * @param value
	 *            Header value
	 */
	public void addDefaultHeader(String key, String value) {
		mDefaultHeaders.put(key, value);
	}

	/**
	 * Removes a key from the list of headers that are added to all requests.
	 * 
	 * @param key
	 *            Header key to remove
	 */
	public void removeDefaultHeader(String key) {
		mDefaultHeaders.remove(key);
	}

	/**
	 * Makes a HTTP GET request <b>asynchronously</b>, immediately returning
	 * an int which will be the ID of that request.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param callback
	 *            Class to call methods in upon completion of request.
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @return An int which will be the ID of that request. You can use this to
	 *         cancel the request.
	 */
	public <T> int get(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpGet request = new HttpGet(url);
		setupRequest(request, headers, params);
		return performRequest(callback, request, clazz);
	}

	/**
	 * Makes a HTTP GET request <b>synchronously</b>
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @return A Response object containing the response data and error
	 *         information.
	 */
	public <T> Response<T> getSync(String url, Class<T> clazz, Header[] headers, HttpParams params) {
		HttpGet request = new HttpGet(url);
		setupRequest(request, headers, params);
		return performSyncRequest(request, clazz);
	}

	/**
	 * Makes a HTTP POST request <b>asynchronously</b>, immediately returning
	 * an int which will be the ID of that request.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param callback
	 *            Class to call methods in upon completion of request.
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @param body
	 *            An object to be converted to JSON and attached to the request.
	 * @return An int which will be the ID of that request. You can use this to
	 *         cancel the request.
	 */
	public <T> int post(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params,
			Object body) {
		HttpPost request = new HttpPost(url);
		setupRequest(request, headers, params);
		if (body != null) {
			try {
				StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
				request.setEntity(bodyString);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return -1;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return -1;
			}
		}
		return performRequest(callback, request, clazz);
	}

	/**
	 * Makes a HTTP POST request <b>synchronously</b>.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @param body
	 *            An object to be converted to JSON and attached to the request.
	 * @return A Response object containing the response data and error
	 *         information.
	 */
	public <T> Response<T> postSync(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers,
			HttpParams params, Object body) {
		HttpPost request = new HttpPost(url);
		setupRequest(request, headers, params);
		if (body != null) {
			try {
				StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
				request.setEntity(bodyString);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return null;
			}
		}
		return performSyncRequest(request, clazz);
	}

	/**
	 * Makes a HTTP PUT request <b>asynchronously</b>, immediately returning
	 * an int which will be the ID of that request.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param callback
	 *            Class to call methods in upon completion of request.
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @param body
	 *            An object to be converted to JSON and attached to the request.
	 * @return An int which will be the ID of that request. You can use this to
	 *         cancel the request.
	 */
	public <T> int put(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params,
			Object body) {
		HttpPut request = new HttpPut(url);
		setupRequest(request, headers, params);
		if (body != null) {
			try {
				StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
				request.setEntity(bodyString);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return -1;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return -1;
			}
		}
		return performRequest(callback, request, clazz);
	}

	/**
	 * Makes a HTTP PUT request <b>synchronously</b>.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @param body
	 *            An object to be converted to JSON and attached to the request.
	 * @return A Response object containing the response data and error
	 *         information.
	 */
	public <T> Response<T> putSync(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers,
			HttpParams params, Object body) throws UnsupportedEncodingException, JsonProcessingException {
		HttpPut request = new HttpPut(url);
		setupRequest(request, headers, params);
		if (body != null) {
			StringEntity bodyString = new StringEntity(mMapper.writeValueAsString(body));
			request.setEntity(bodyString);
		}
		return performSyncRequest(request, clazz);
	}

	/**
	 * Makes a HTTP DELETE request <b>asynchronously</b>, immediately returning
	 * an int which will be the ID of that request.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param callback
	 *            Class to call methods in upon completion of request.
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @param body
	 *            An object to be converted to JSON and attached to the request.
	 * @return An int which will be the ID of that request. You can use this to
	 *         cancel the request.
	 */
	public <T> int delete(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers, HttpParams params) {
		HttpDelete request = new HttpDelete(url);
		setupRequest(request, headers, params);
		return performRequest(callback, request, clazz);
	}

	/**
	 * Makes a HTTP DELETE request <b>synchronously</b>.
	 * 
	 * @param url
	 *            URL for request
	 * @param clazz
	 *            Class to cast response to
	 * @param headers
	 *            Array of headers to be added to this request.
	 * @param params
	 *            HttpParams to be added to this request.
	 * @return A Response object containing the response data and error
	 *         information.
	 */
	public <T> Response<T> deleteSync(String url, Class<T> clazz, RestCallback<T> callback, Header[] headers,
			HttpParams params) {
		HttpDelete request = new HttpDelete(url);
		setupRequest(request, headers, params);
		return performSyncRequest(request, clazz);
	}

	/**
	 * Cancel an asynchronous request.
	 * 
	 * @param id
	 *            ID of the task to cancel.
	 * @return whether the task was successfully cancelled or not.
	 */
	public boolean cancel(int id) {
		try {
			return mTasks.get(id).cancel(true);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Gets the status of the network request.
	 * 
	 * @param id
	 *            ID of the task to get the status of.
	 * @return An AsyncTask.Status of the request, or <b>null</b> if the task isn't found.
	 */
	public Status getStatus(int id) {
		try {
			return mTasks.get(id).getStatus();
		} catch (Exception e) {
			return null;
		}
	}

	private void setupRequest(HttpUriRequest request, Header[] headers, HttpParams params) {
		if (headers != null) {
			for (Header h : headers) {
				request.addHeader(h);
			}
		}
		if (params != null) {
			request.setParams(params);
		}
	}

	private <T> int performRequest(RestCallback<T> callback, HttpUriRequest request, Class<T> clazz) {
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		int id = mCount.getAndIncrement();
		mTasks.put(id, d);
		d.execute();
		return id;
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
		protected void onPreExecute() {
			for (Entry<String, String> entry : mDefaultHeaders.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}

		@Override
		protected Response<T> doInBackground(Void... params) {
			return performSyncRequest(request, clazz);
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
			if (callback == null)
				return;
			if (response.errorCode == 0) {
				callback.onSuccess(response);
			} else {
				callback.onFailure(response);
			}
		}
	}

	private <T> Response<T> performSyncRequest(HttpUriRequest request, Class<T> clazz) {
		Response<T> response = new Response<T>();
		HttpResponse httpResponse;
		try {
			httpResponse = getThreadSafeClient().execute(request);
			StatusLine statusLine = httpResponse.getStatusLine();
			response.httpStatusCode = statusLine.getStatusCode();
			if (response.httpStatusCode / 100 == 2) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				httpResponse.getEntity().writeTo(out);
				response.headers = httpResponse.getAllHeaders();
				out.close();
				response.rawResponse = out.toString();
				try {
					if (response.response instanceof Void == false && clazz != null) {
						response.response = (T) mMapper.readValue(response.rawResponse, clazz);
					}
				} catch (Exception e) {
					e.printStackTrace();
					response.errorCode = Response.ErrorCodes.CAST_ERROR;
				}
			} else {
				httpResponse.getEntity().getContent().close();
				response.errorCode = Response.ErrorCodes.NETWORK_ERROR_BAD_STATUS_CODE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.errorCode = Response.ErrorCodes.NETWORK_ERROR_UNKNOWN;
		}
		return response;
	}

	private synchronized DefaultHttpClient getThreadSafeClient() {
		if (client != null)
			return client;

		client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);

		return client;
	}
}
