package com.awtarika.android.app.util;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anasqadrei on 17/11/16.
 */

public class AwtarikaJsonArrayRequest extends JsonArrayRequest {
    public AwtarikaJsonArrayRequest(int method, String url, JSONArray jsonRequest, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
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
