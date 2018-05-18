package io.predic.tracker;

import com.android.volley.VolleyError;

public interface HttpRequestResponseCallback {
    void onStringResponseSuccess(String response);
    void onError(VolleyError e);
}
