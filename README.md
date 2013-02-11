AndroidRestClient
=================
Asynchronous JSON REST client for Android.

Example Usage
=================
```
new Builder("http://bclymer.com/json", RestClient.RequestType.GET)
  .setCastClass(Stuff.class)
  .setRestClientCallback(new RestClientCallback<Stuff>() {
  
    @Override
    public void onPreExecute() {
      // Update UI to show we're loading something.
    }
  
  	@Override
  	public void onSuccess(RestClientResponse<Stuff> response) {
  		Log.e("YAY", response.response.cityName);
  	}
  
  	@Override
  	public void onFailure(RestClientResponse<Stuff> response) {
  		Log.e("YAY", response.response.cityName);
  	}
    
    @Override
    onPostExecute() {
      // Change UI back, we're done loading. This is method just exists
      // so you don't need the same code in onFailure and onSuccess
    }
  })
  .executeAsync();
```

With `Stuff` being any Java bean.

You can override any, all, or none of the methods in the callback. If you're just POSTing and don't care about the response you can not set a callback and call `executeAsync` and then not worry.

If the user doesn't set the CastClass the `RestClientResponse.response` object will be null be `RestClientResponse.rawResponse` will still exist.

The library can also return RestClientResponse<Stuff> directly if `executeSync` is called instead of `executeAsync`.

You can also POST or PUT with the client (DELETE too) with

```
Stuff f = new Stuff();
f.cityName = "Test";
new Builder("http://bclymer.com/post.php", RestClient.RequestType.POST)
  .setObjectBody(f, RestClient.EncodeStyle.FORM_ENCODED)
  .setRestClientCallback(new RestClientCallback<Void>() {
  
  	@Override
  	public void onSuccess(RestClientResponse<Void> response) {
  		Log.e("YAY", response.rawResponse);
  	}
  
  	@Override
  	public void onFailure(RestClientResponse<Void> response) {
  		Log.e("YAY", response.rawResponse);
  	}
  })
  .executeAsync();
```

The encode styles supported are `FORM_ENCODED` and `JSON`. You simply throw any object (a Java bean probably) into the setObjectBody method and it will be encoded and written to the POST or PUT body.

It can also be used syncronously, in it's simplest form like
```
String htmlAndStuff = new Builder("http://www.google.com", RestClient.RequestType.GET).executeSync().rawResponse;
```

Or without the builder
```
RestClientResponse<Stuff> response = RestClient.getInstance().getSync("http://www.google.com", null, null, null, null);
```

Managing async requests

Any async method call will return a unique int id that will let the user call
```
public Status getStatus(int id);
```
Which will return a `android.os.AsyncTask.Status` enum giving the status of the request.
You may also call
```
public boolean cancel(int id);
```
To cancel a pending or current request, returning whether the request was cancelled or not.
