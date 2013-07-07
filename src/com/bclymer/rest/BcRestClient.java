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

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.bclymer.rest.BcRestClientResponse.ErrorCode;
import com.google.gson.Gson;

public class BcRestClient {

	private final Gson gson = new Gson();
	private static LoggingLevel loggingLevel = LoggingLevel.NONE;

	private Map<String, String> mDefaultHeaders = new HashMap<String, String>();
	
	private Runnable mFailureCallback;
	private BcRestClientResponse<?> mFailureResponse;

	private static final BcRestClient instance = new BcRestClient();

	private BcRestClient() {
		disableConnectionReuseIfNecessary();
	}

	public static BcRestClient getInstance() {
		return instance;
	}
	
	/**
	 * This runnable will be run if the network request fails for any reason.
	 * It will be called for both async and sync calls.
	 * The BcRestClientResponse can be gotten via RestClient.getInstance().getFailureResponse();
	 * @param runnable The runnable to execute on a failure
	 */
	public void setFailureCallback(Runnable runnable) {
		mFailureCallback = runnable;
	}
	
	public BcRestClientResponse<?> getFailureResponse() {
		return mFailureResponse;
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
		log("Adding default header with key " + key + " and value " + value, LoggingLevel.VERBOSE);
		mDefaultHeaders.put(key, value);
	}

	/**
	 * Removes a key from the list of headers that are added to all requests.
	 * 
	 * @param key
	 *            Header key to remove
	 */
	public void removeDefaultHeader(String key) {
		log("Removing default header with key " + key, LoggingLevel.VERBOSE);
		mDefaultHeaders.remove(key);
	}
	
	/**
	 * Set the logging level for the RestClient.
	 * VERBOSE - Log everything (well, almost everything).
	 * WARNING - Log anything that might go wrong.
	 * ERROR - Log when an error happens.
	 * NONE - Don't log a thing.
	 */
	public static void setLoggingLevel(LoggingLevel level) {
		loggingLevel = level;
		log("Logging level set to " + loggingLevel.name(), LoggingLevel.VERBOSE);
	}

	private String getStringForObject(Object obj) {
		String serialization = gson.toJson(obj);
		log("Serialized object as " + serialization, LoggingLevel.VERBOSE);
		return serialization;
	}

	private <T> void performRequest(Request request) {
		log("Adding async task to queue", LoggingLevel.VERBOSE);
		executeAsyncRequest(request);
	}

	private <T> void executeAsyncRequest(final Request request) {
		BcThreadManager.runOnUi(new Runnable() {
			@Override
			public void run() {
				log("onPreExecute Id " + request.taskId, LoggingLevel.VERBOSE);
				if (request.callback != null) {
					request.callback.onPreExecute();
				}
				BcThreadManager.runInBackground(new Runnable() {
					
					@Override
					public void run() {
						log("Executing Task " + request.taskId, LoggingLevel.VERBOSE);
						final BcRestClientResponse<T> response = performSyncRequest(request);
						BcThreadManager.runOnUi(new Runnable() {
							
							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								log("onPostExecute Task " + request.taskId, LoggingLevel.VERBOSE);
								if (request.callback == null)
									return;
								request.callback.onPostExecute();
								if (response.errorCode == ErrorCode.NONE) {
									request.callback.onSuccess(response);
								} else {
									request.callback.onFailure(response);
								}
							}
						});
					}
				});
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> BcRestClientResponse<T> performSyncRequest(Request request) {
		BcRestClientResponse<T> response = new BcRestClientResponse<T>();
		response.url = request.url;
		try {
			log(request.url, LoggingLevel.VERBOSE);
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
			log("Status code " + response.httpStatusCode, LoggingLevel.VERBOSE);
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
			log("Raw Response: " + response.rawResponse, LoggingLevel.VERBOSE);
			if (response.httpStatusCode / 100 == 2) {
				try {
					if (request.clazz != null) {
						response.response = (T) gson.fromJson(response.rawResponse, request.clazz);
						if (response.response == null) {
							log("CAST ERROR - Failed to cast to " + request.clazz.getSimpleName(), LoggingLevel.ERROR);
							response.errorCode = ErrorCode.CAST_ERROR;
						} else {
							response.errorCode = ErrorCode.NONE;
						}
					} else {
						response.errorCode = ErrorCode.NONE;
					}
				} catch (Exception e) {
					if (loggingLevel.value > LoggingLevel.VERBOSE.value) {
						e.printStackTrace();
					}
					log("CAST ERROR - Failed to cast to " + request.clazz.getSimpleName(), LoggingLevel.ERROR);
					response.errorCode = ErrorCode.CAST_ERROR;
				}
			} else {
				response.errorCode = ErrorCode.NETWORK_ERROR_BAD_STATUS_CODE;
			}
		} catch (Exception e) {
			if (loggingLevel.value > LoggingLevel.VERBOSE.value) {
				e.printStackTrace();
			}
			log("NETWORK ERROR UNKNOWN", LoggingLevel.ERROR);
			response.errorCode = ErrorCode.NETWORK_ERROR_UNKNOWN;
		}
		if (response.errorCode != ErrorCode.NONE && mFailureCallback != null) {
			mFailureResponse = response;
			mFailureCallback.run();
			mFailureResponse = null;
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
		public Builder setRestClientCallback(BcRestClientCallback callback) {
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
					"Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
			return this;
		}

		/**
		 * Execute the request built by the builder on the current thread.
		 * 
		 * @param <T>
		 * @return The response object
		 */
		public <T> BcRestClientResponse<T> executeSync() {
			return getInstance().performSyncRequest(request);
		}

		/**
		 * Execute the request built by the builder on a background thread
		 * calling onSuccess or onFailure on the RestClientCallback object when
		 * done.
		 * 
		 * @return an int id of the request to check the status on or cancel.
		 */
		public void executeAsync() {
			BcRestClient.getInstance().performRequest(request);
		}
	}

	private class Request {

		public String url;
		public Class<?> clazz;
		public HashMap<String, String> headers;
		@SuppressWarnings("rawtypes")
		public BcRestClientCallback callback;
		public String body;
		public RequestType requestType;
		public int taskId;

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
	
	public enum LoggingLevel {
		VERBOSE(0),
		WARNING(1),
		ERROR(2),
		NONE(3);
		
		public int value;

		LoggingLevel(int value) {
			this.value = value;
		}
	}
	
	private static void log(String message, LoggingLevel loggingLevel) {
		if (BcRestClient.loggingLevel.value <= loggingLevel.value) {
			switch (loggingLevel) {
			case ERROR:
				Log.e("RestClient", message);
				break;
			case VERBOSE:
				Log.i("RestClient", message);
				break;
			case WARNING:
				Log.w("RestClient", message);
				break;
			default:
				break;
			}
		}
	}
}
