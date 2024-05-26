package pk.gov.pbs.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StaticUtils {
    private static Handler handler;
    public static Handler getHandler(){
        synchronized (StaticUtils.class){
            Looper looper = Looper.myLooper();
            if (handler == null)
                handler= new Handler(looper == null ? Looper.getMainLooper() : looper);
            return handler;
        }
    }

    private static Gson gson;
    public static Gson getGson(boolean prettify, boolean excludeNonExpose, boolean replace) {
        synchronized (StaticUtils.class){
            if (gson == null || replace) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                if (excludeNonExpose)
                    gsonBuilder.excludeFieldsWithoutExposeAnnotation();
                if (prettify)
                    gsonBuilder.setPrettyPrinting();
                gson = gsonBuilder.create();
            }
            return gson;
        }
    }

    public static Gson getGson(boolean prettify, boolean excludeNonExpose) {
        return getGson(prettify, excludeNonExpose, true);
    }

    public static Gson getGson(boolean prettify) {
        return getGson(prettify, false, true);
    }

    public static Gson getGson() {
        return getGson(false, false, false);
    }

    public static String toPrettyJson(Object object) {
        return getGson(true).toJson(object);
    }

    public static String toJson(Object object) {
        return getGson(false).toJson(object);
    }

    private static RequestQueue webRequestQueue;
    public static RequestQueue getVolleyWebQueue(Context context) {
        synchronized (StaticUtils.class) {
            if (webRequestQueue == null)
                webRequestQueue = Volley.newRequestQueue(context);
            return webRequestQueue;
        }
    }
}
