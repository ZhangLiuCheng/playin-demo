package com.tech.playin.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {

    public static final String PRE_NAME = "playInSdk";
    public static final String KEY_SDK = "sdkKey";

    public static String getSdkKey(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_SDK, null);
    }

    public static void setSdkKey(Context context, String sdkKey) {
        SharedPreferences preferences = context.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SDK, sdkKey).apply();
    }
}
