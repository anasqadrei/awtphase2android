package com.awtarika.android.app.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by anasqadrei on 10/11/16.
 */
public class NetworkingSingleton {
    private static NetworkingSingleton ourInstance;
    private static Context mContext;
    private RequestQueue mRequestQueue;

    public static NetworkingSingleton getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new NetworkingSingleton(context);
        }
        return ourInstance;
    }

    private NetworkingSingleton(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
