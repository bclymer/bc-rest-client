package com.bclymer.rest;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.SparseArray;

import com.google.gson.Gson;

public class RestClient {

	private final Gson gson = new Gson();

	private final AtomicInteger mCount = new AtomicInteger();

	@SuppressWarnings("rawtypes")
	private SparseArray<DownloadWebSourceTask> mTasks = new SparseArray<DownloadWebSourceTask>();
	private Map<String, String> mDefaultHeaders = new HashMap<String, String>();

	private DefaultHttpClient client;

	private static final RestClient instance = new RestClient();

	private RestClient() {}

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
	public <T> int get(String url, Class<T> clazz, RestClientCallback<T> callback, Header[] headers, HttpParams params) {
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
	public <T> RestClientResponse<T> getSync(String url, Class<T> clazz, Header[] headers, HttpParams params) {
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
	public <T> int post(String url, Class<T> clazz, RestClientCallback<T> callback, Header[] headers, HttpParams params,
			Object body, EncodeStyle encodeStyle) {
		HttpPost request = new HttpPost(url);
		setupRequest(request, headers, params);
		if (body != null) {
			request.setEntity(getEntityForObject(body, encodeStyle));
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
	public <T> RestClientResponse<T> postSync(String url, Class<T> clazz, Header[] headers,
			HttpParams params, Object body, EncodeStyle encodeStyle) {
		HttpPost request = new HttpPost(url);
		setupRequest(request, headers, params);
		if (body != null) {
			request.setEntity(getEntityForObject(body, encodeStyle));
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
	public <T> int put(String url, Class<T> clazz, RestClientCallback<T> callback, Header[] headers, HttpParams params,
			Object body, EncodeStyle encodeStyle) {
		HttpPut request = new HttpPut(url);
		setupRequest(request, headers, params);
		if (body != null) {
			request.setEntity(getEntityForObject(body, encodeStyle));
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
	public <T> RestClientResponse<T> putSync(String url, Class<T> clazz, Header[] headers,
			HttpParams params, Object body, EncodeStyle encodeStyle) {
		HttpPut request = new HttpPut(url);
		setupRequest(request, headers, params);
		if (body != null) {
			request.setEntity(getEntityForObject(body, encodeStyle));
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
	public <T> int delete(String url, Class<T> clazz, RestClientCallback<T> callback, Header[] headers, HttpParams params) {
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
	public <T> RestClientResponse<T> deleteSync(String url, Class<T> clazz, Header[] headers,
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
	 * @return An AsyncTask.Status of the request, or <b>null</b> if the task
	 *         isn't found.
	 */
	public Status getStatus(int id) {
		try {
			return mTasks.get(id).getStatus();
		} catch (Exception e) {
			return null;
		}
	}

	private HttpEntity getEntityForObject(Object obj, EncodeStyle encodeStyle) {
		String objAsString = gson.toJson(obj);
		switch (encodeStyle) {
		case FORM_ENCODED:
			try {
				JSONObject j = new JSONObject(objAsString);
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(j.length());
				@SuppressWarnings("unchecked")
				Iterator<String> iter = j.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					String value = j.getString(key);
					nameValuePairs.add(new BasicNameValuePair(key, value));
				}
				return new UrlEncodedFormEntity(nameValuePairs);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		case JSON:
		default:
			try {
				return new StringEntity(objAsString);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
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

	private <T> int performRequest(RestClientCallback<T> callback, HttpUriRequest request, Class<T> clazz) {
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(callback, request, clazz);
		int id = mCount.getAndIncrement();
		mTasks.put(id, d);
		d.execute();
		return id;
	}

	private class DownloadWebSourceTask<T> extends AsyncTask<Void, Void, RestClientResponse<T>> {

		private RestClientCallback<T> callback;
		private HttpUriRequest request;
		private Class<T> clazz;

		public DownloadWebSourceTask(RestClientCallback<T> callback, HttpUriRequest request, Class<T> clazz) {
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
		protected RestClientResponse<T> doInBackground(Void... params) {
			return performSyncRequest(request, clazz);
		}

		@Override
		protected void onCancelled(RestClientResponse<T> response) {
			if (response == null) {
				response = new RestClientResponse<T>();
			}
			response.errorCode = RestClientResponse.ErrorCodes.REQUEST_CANCELLED;
			callback.onFailure(response);
		}

		@Override
		protected void onPostExecute(RestClientResponse<T> response) {
			if (callback == null)
				return;
			if (response.errorCode == 0) {
				callback.onSuccess(response);
			} else {
				callback.onFailure(response);
			}
		}
	}

	private <T> RestClientResponse<T> performSyncRequest(HttpUriRequest request, Class<T> clazz) {
		RestClientResponse<T> response = new RestClientResponse<T>();
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
					if (clazz != null) {
						response.response = (T) gson.fromJson(response.rawResponse, clazz);
					}
				} catch (Exception e) {
					e.printStackTrace();
					response.errorCode = RestClientResponse.ErrorCodes.CAST_ERROR;
				}
			} else {
				httpResponse.getEntity().getContent().close();
				response.errorCode = RestClientResponse.ErrorCodes.NETWORK_ERROR_BAD_STATUS_CODE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.errorCode = RestClientResponse.ErrorCodes.NETWORK_ERROR_UNKNOWN;
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

	/**
	 * A helper class to build and execute a requests.
	 * @author Brian
	 *
	 */
	public static class Builder {

		private String url;
		private Class<?> clazz;
		private Header[] headers;
		private HttpParams params;
		@SuppressWarnings("rawtypes")
		private RestClientCallback callback;
		private Object body;
		private EncodeStyle encodeStyle;
		private RequestType requestType;

		/**
		 * Builder Constructor
		 * @param url URL to hit for the request
		 * @param requestType
		 * <br>RestClient.REQUEST_TYPE_GET
		 * <br>RestClient.REQUEST_TYPE_POST
		 * <br>RestClient.REQUEST_TYPE_PUT
		 * <br>RestClient.REQUEST_TYPE_DELETE 
		 */
		public Builder(String url, RequestType requestType) {
			this.url = url;
			this.requestType = requestType;
		}

		/**
		 * Set the class to cast the request's JSON response to.
		 * @param clazz class to cast
		 * @return This builder
		 */
		public Builder setCastClass(Class<?> clazz) {
			this.clazz = clazz;
			return this;
		}

		/**
		 * Adds headers to the default http headers.
		 * @param headers Header array to add to request
		 * @return This builder
		 */
		public Builder addHeaders(Header[] headers) {
			this.headers = headers;
			return this;
		}

		/**
		 * Set parameters to the http request.
		 * @param params Params to add to request
		 * @return This builder
		 */
		public Builder setParams(HttpParams params) {
			this.params = params;
			return this;
		}

		/**
		 * Sets the callback object for async network requests.
		 * Override onFailure(Response<T> response) and
		 * onSuccess(Response<T> response)
		 * @param callback an instance RestCallback
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		public Builder setRestClientCallback(RestClientCallback callback) {
			this.callback = callback;
			return this;
		}

		/**
		 * Write the body of a POST or PUT request
		 * @param body Any object with properties to encode
		 * @param encodeStyle
		 * <br>RestClient.ENCODE_STYLE_FORM_ENCODED
		 * <br>RestClient.ENCODE_STYLE_JSON
		 * @return
		 */
		public Builder setObjectBody(Object body, EncodeStyle encodeStyle) {
			this.body = body;
			this.encodeStyle = encodeStyle;
			return this;
		}

		/**
		 * Execute the request built by the builder on the current thread.
		 * @param <T>
		 * @return The response object
		 */
		@SuppressWarnings("unchecked")
		public <T> RestClientResponse<T> executeSync() {
			switch (requestType) {
			case GET:
			default:
				return (RestClientResponse<T>) RestClient.getInstance().getSync(url, clazz, headers, params);
			case POST:
				return (RestClientResponse<T>) RestClient.getInstance().postSync(url, clazz, headers, params, body, encodeStyle);
			case PUT:
				return (RestClientResponse<T>) RestClient.getInstance().putSync(url, clazz, headers, params, body, encodeStyle);
			case DELETE:
				return (RestClientResponse<T>) RestClient.getInstance().deleteSync(url, clazz, headers, params);
			}
		}

		/**
		 * Execute the request built by the builder on a background thread
		 * calling onSuccess or onFailure on the RestClientCallback object when done.
		 * @return an int id of the request to check the status on or cancel.
		 */
		@SuppressWarnings("unchecked")
		public int executeAsync() {
			switch (requestType) {
			case GET:
			default:
				return RestClient.getInstance().get(url, clazz, callback, headers, params);
			case POST:
				return RestClient.getInstance().post(url, clazz, callback, headers, params, body, encodeStyle);
			case PUT:
				return RestClient.getInstance().put(url, clazz, callback, headers, params, body, encodeStyle);
			case DELETE:
				return RestClient.getInstance().delete(url, clazz, callback, headers, params);
			}
		}
	}
	
	public enum RequestType {
		GET (0),
		POST (1),
		PUT (2),
		DELETE (3);
		
		public int value;
		
		RequestType(int value) {
			this.value = value;
		}
	}
	
	public enum EncodeStyle {
		JSON (0),
		FORM_ENCODED (1);
		
		public int value;
		
		EncodeStyle(int value) {
			this.value = value;
		}
	}
}
