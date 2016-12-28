package com.awtarika.android.app.util;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anasqadrei on 10/11/16.
 */

public class AwtarikaJsonObjectRequest extends JsonObjectRequest {
    public AwtarikaJsonObjectRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String>  params = new HashMap<String, String>();
        params.put("Accept", "application/json");
        params.put("User-Agent", System.getProperty("http.agent") + " awtarika android app");
        return params;
    }
}
