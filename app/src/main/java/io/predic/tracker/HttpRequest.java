package io.predic.tracker;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

class HttpRequest {
    private final static HttpRequest ourInstance = new HttpRequest();
    private static RequestQueue queue = null;

    static HttpRequest getInstance() {
        return ourInstance;
    }

    private HttpRequest() {}

    public static void initialize(Context context) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }

    public void sendHttpStringRequest(final String url, final HttpRequestResponseCallback callback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("PREDICIO", url + " - worked");

                        if (callback != null) {
                            callback.onStringResponseSuccess(response);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("PREDICIO", url + " - error");
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });

        queue.add(stringRequest);
    }

    public void sendHttpJSONRequest(final String url, JSONObject obj) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        Log.d("PREDICIO", url + " - worked");

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("PREDICIO", url + " - error");
            }
        });
        queue.add(jsonRequest);
    }
}
