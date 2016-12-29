package com.awtarika.android.app.util;

import android.content.Context;

import com.logentries.logger.AndroidLogger;

import java.io.IOException;

/**
 * Created by anasqadrei on 29/12/16.
 */
public class LoggerSingleton {
    private static LoggerSingleton ourInstance;
    private static Context mContext;
    private AndroidLogger mLogger;

    public static LoggerSingleton getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new LoggerSingleton(context);
        }
        return ourInstance;
    }

    private LoggerSingleton(Context context) {
        mContext = context;
        try {
            mLogger = AndroidLogger.createInstance(mContext, false, true, false, null, 0, Constants.LogEntries.TOKEN, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        if (mLogger != null) {
            mLogger.log(message);
        }
    }
}
