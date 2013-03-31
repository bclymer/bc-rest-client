package com.bclymer.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.util.Base64;
import android.util.SparseArray;

import com.bclymer.rest.RestClientResponse.ErrorCode;
import com.google.gson.Gson;

public class RestClient {

	private final Gson gson = new Gson();

	private final AtomicInteger mCount = new AtomicInteger();

	@SuppressWarnings("rawtypes")
	private SparseArray<DownloadWebSourceTask> mTasks = new SparseArray<DownloadWebSourceTask>();
	private Map<String, String> mDefaultHeaders = new HashMap<String, String>();

	private static final RestClient instance = new RestClient();

	private RestClient() {
		disableConnectionReuseIfNecessary();
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

	private String getStringForObject(Object obj) {
		return gson.toJson(obj);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private <T> int performRequest(Request request) {
		DownloadWebSourceTask<T> d = new DownloadWebSourceTask<T>(request);
		int id = mCount.getAndIncrement();
		mTasks.put(id, d);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			d.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			d.execute();
		}
		return id;
	}

	private class DownloadWebSourceTask<T> extends AsyncTask<Void, Void, RestClientResponse<T>> {

		private Request request;

		public DownloadWebSourceTask(Request request) {
			this.request = request;
		}

		@Override
		protected void onPreExecute() {
			if (request.callback != null) {
				request.callback.onPreExecute();
			}
		}

		@Override
		protected RestClientResponse<T> doInBackground(Void... params) {
			return performSyncRequest(request);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void onCancelled(RestClientResponse<T> response) {
			if (response == null) {
				response = new RestClientResponse<T>();
			}
			response.errorCode = ErrorCode.REQUEST_CANCELLED;
			if (request.callback != null) {
				request.callback.onFailure(response);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(RestClientResponse<T> response) {
			if (request.callback == null)
				return;
			request.callback.onPostExecute();
			if (response.errorCode == ErrorCode.NONE) {
				request.callback.onSuccess(response);
			} else {
				request.callback.onFailure(response);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> RestClientResponse<T> performSyncRequest(Request request) {
		RestClientResponse<T> response = new RestClientResponse<T>();
		try {
			URL url = new URL(request.url);
			for (Entry<String, String> entry : mDefaultHeaders.entrySet()) {
				request.headers.put(entry.getKey(), entry.getValue());
			}
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			if (request.headers.entrySet() != null) {
				for (Entry<String, String> entry : request.headers.entrySet()) {
					connection.addRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			connection.setRequestMethod(request.requestType.name());
			connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
			connection.setRequestProperty("Accept","*/*");
			if (request.requestType == RequestType.POST || request.requestType == RequestType.PUT) {
				connection.setDoOutput(true);
				OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
				writer.write(request.body);
				writer.flush();
			}
			response.httpStatusCode = connection.getResponseCode();
			InputStream in;
			if (response.httpStatusCode / 100 == 2) {
				in = connection.getInputStream();
			} else {
				in = connection.getErrorStream();
			}
			if (in != null) {
				response.rawResponse = readStream(in);
			}
			if (response.rawResponse == null) {
				response.rawResponse = "";
			}
			if (response.httpStatusCode / 100 == 2) {
				try {
					if (request.clazz != null) {
						response.response = (T) gson.fromJson(response.rawResponse, request.clazz);
						if (response.response == null) {
							response.errorCode = ErrorCode.CAST_ERROR;
						} else {
							response.errorCode = ErrorCode.NONE;
						}
					} else {
						response.errorCode = ErrorCode.NONE;
					}
				} catch (Exception e) {
					e.printStackTrace();
					response.errorCode = ErrorCode.CAST_ERROR;
				}
			} else {
				response.errorCode = ErrorCode.NETWORK_ERROR_BAD_STATUS_CODE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.errorCode = ErrorCode.NETWORK_ERROR_UNKNOWN;
		}
		return response;
	}

	private String readStream(InputStream in) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			char[] bytes = new char[1024];
			int count = 0;
			while ((count = reader.read(bytes, 0, 1024)) != -1) {
				str.append(bytes, 0, count);
			}
			return str.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	/**
	 * A helper class to build and execute a requests.
	 * 
	 * @author Brian
	 * 
	 */
	public static class Builder {

		private Request request;

		/**
		 * Builder Constructor
		 * 
		 * @param url
		 *            URL to hit for the request
		 * @param requestType
		 * <br>
		 *            RestClient.RequestType.GET <br>
		 *            RestClient.RequestType.POST <br>
		 *            RestClient.RequestType.PUT <br>
		 *            RestClient.RequestType.DELETE
		 */
		public Builder(String url, RequestType requestType) {
			request = getInstance().new Request();
			request.url = url;
			request.requestType = requestType;
			request.headers = new HashMap<String, String>();
		}

		/**
		 * Set the class to cast the request's JSON response to.
		 * 
		 * @param clazz
		 *            class to cast
		 * @return This builder
		 */
		public Builder setCastClass(Class<?> clazz) {
			request.clazz = clazz;
			return this;
		}

		/**
		 * Adds header to the default http headers.
		 * 
		 * @return This builder
		 */
		public Builder addHeader(String name, String value) {
			request.headers.put(name, value);
			return this;
		}
		
		/**
		 * Sets the callback object for async network requests.
		 * Override onFailure(Response<T> response) and
		 * onSuccess(Response<T> response)
		 * 
		 * @param callback
		 *            an instance of RestClientCallback
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		public Builder setRestClientCallback(RestClientCallback callback) {
			request.callback = callback;
			return this;
		}

		/**
		 * Write the body of a POST or PUT request
		 * 
		 * @param body
		 *            Any object with properties to encode
		 * @return
		 */
		public Builder setObjectBody(Object body) {
			request.body = getInstance().getStringForObject(body);
			return this;
		}

		public Builder addBasicAuthentication(String username, String password) {
			request.headers.put("Authorization",
					"Basic " + Base64.encodeToString("user:password".getBytes(), Base64.NO_WRAP));
			return this;
		}

		/**
		 * Execute the request built by the builder on the current thread.
		 * 
		 * @param <T>
		 * @return The response object
		 */
		public <T> RestClientResponse<T> executeSync() {
			return getInstance().performSyncRequest(request);
		}

		/**
		 * Execute the request built by the builder on a background thread
		 * calling onSuccess or onFailure on the RestClientCallback object when
		 * done.
		 * 
		 * @return an int id of the request to check the status on or cancel.
		 */
		public int executeAsync() {
			return RestClient.getInstance().performRequest(request);
		}
	}

	private class Request {

		public String url;
		public Class<?> clazz;
		public HashMap<String, String> headers;
		@SuppressWarnings("rawtypes")
		public RestClientCallback callback;
		public String body;
		public RequestType requestType;

	}

	public enum RequestType {
		GET(0),
		POST(1),
		PUT(2),
		DELETE(3);

		public int value;

		RequestType(int value) {
			this.value = value;
		}
	}
}
