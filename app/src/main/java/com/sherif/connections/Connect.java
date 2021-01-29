package com.sherif.connections;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class Connect {
    private final String NOT_FOUND = "404 Not Found!!";
    private RequestBody body;
    private String url;
    private ListenJson listenJson;
    private ListenString listenString;
    private final String TAG = "ConnectTag";
    private static Connect connect;
    private Map<String, String> map;
    private final String NO_INTERNET = "تحقق من اتصال الانترنت";
    private final String SERVER_ERROR = "حدث خطأ داخلي في الخادم";
    private static int TIME_OUT = 120;

    private Connect() {
    }

    public static Connect getInstance() {
        connect = new Connect();
        return connect;
    }

    /**
     * if 0 its means no time out
     * @param TIME_OUT
     * @return Connect
     */
    public static Connect getInstance(int TIME_OUT) {
        connect = new Connect();
        Connect.TIME_OUT = TIME_OUT;
        return connect;
    }

    private void start(Context context) {
        Log.v(TAG, "url : " + url);
        try {

            final OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                    .readTimeout(TIME_OUT, TimeUnit.SECONDS).build();
            Request.Builder builder = new Request.Builder()
                    .url(url);

            if (body != null) {
                builder.post(body);
            } else if (map != null) {
                MultipartBody.Builder part = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for (String key : map.keySet()) {
                    String value = map.get(key);
                    part.addFormDataPart(key, value != null ? value : "");
                }
                builder.post(part.build());
            } else {
                builder.get();
            }
            Response response = client.newCall(builder.build()).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                try {
                    final String result = responseBody.string();
                    final int code = response.code();
                    triggerListeners(result, code, context);
                    Log.v(TAG, "response code is : " + response.code());
                } catch (IOException e) {
                    e.printStackTrace();
                    triggerListeners(e.toString(), 0, context);
                    Log.v(TAG, "IOException is : " + e.toString());
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            triggerListeners(e.toString(), 0, context);
            Log.v(TAG, "Exception is : " + e.toString());
        }
    }

    private void showErrToast(Context context, String msg) {
        if (context == null) return;
        try {
            Toast.makeText(context , msg , Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void triggerListeners(String result, int code, Context context) {
//        Log.v(TAG, "currentThread 109 :" + Thread.currentThread().getName());
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
//            Log.v(TAG, "currentThread  112 :" + Thread.currentThread().getName());
            if (listenString != null) {
                listenString.result(result, code);
            }
            if (listenJson != null) {
                JSONObject object = null;
                try {
                    object = new JSONObject(result);
                    Log.v(TAG, "result object : " + object.toString(1));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.v(TAG, "result string : " + result);
                }
                if (object == null) {
                    object = new JSONObject();
                }
                listenJson.result(object, code);
                if (code == 200) return;
                switch (code) {
                    case 0:
                        showErrToast(context, NO_INTERNET);
                        break;
                    case 500:
                        showErrToast(context, SERVER_ERROR);
                        break;
                    case 404:
                        showErrToast(context, NOT_FOUND);
                        break;
                    default:
                        if (object.has("message")) {
                            String message = object.optString("message");
                            showErrToast(context, message);
                        } else {
                            String message = "Error Code : " + code;
                            showErrToast(context, message);
                        }
                        break;
                }

            }
        });
    }

    public Connect BodyPart(MultipartBody.Builder part) {
        this.body = part.build();
        return connect;
    }

    public Connect Url(String url) {
        this.url = url;
//        Log.v(TAG, "url : " + url);
        return connect;
    }

    public Connect BodyMap(Map<String, String> map) {
        this.map = map;
        try {
            String mapString = map.toString();
            mapString = mapString.replaceAll("=", ":");
            mapString = mapString.replaceAll(",", "\n");
            Log.v(TAG, "BodyMap to String : " + mapString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connect;
    }

    // to make post request with empty body
    public Connect BodyMap() {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("", "");
        this.map = bodyMap;
        return connect;
    }

    public void callJson(Context context, ListenJson listenJson) {
        this.listenJson = listenJson;
        Runnable runnable = () -> start(context);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void callJson(ListenJson listenJson) {
        this.listenJson = listenJson;
        Runnable runnable = () -> start(null);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void callString(Context context, ListenString listenString) {
        this.listenString = listenString;
        Runnable runnable = () -> start(context);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public interface ListenJson {
        void result(@NonNull JSONObject object, int code);
    }

    public interface ListenString {
        void result(@NonNull String result, int code);
    }
}