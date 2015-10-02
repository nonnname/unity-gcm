package com.kskkbys.unitygcmplugin;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * GCMIntentService.<br>
 * For each callback, this class sends message to GameObject via UnitySendMessage.
 *
 * @author Keisuke Kobayashi
 */
public class UnityGCMIntentService extends IntentService {

    private static final String TAG = UnityGCMIntentService.class.getSimpleName();

    public static final String ON_ERROR = "OnError";
    public static final String ON_MESSAGE = "OnMessage";
    public static final String ON_REGISTERED = "OnRegistered";
    public static final String ON_DELETE_MESSAGES = "OnDeleteMessages";

    public UnityGCMIntentService() {
        super(UnityGCMIntentService.class.getCanonicalName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                onError(this, extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                onDeletedMessages(this, extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                onMessage(this, intent);
            }
        }
        UnityGCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    protected void onError(Context context, String errorId) {
        Log.v(TAG, "onError");
        Util.sendMessage(ON_ERROR, errorId);
    }

    protected void onMessage(Context context, Intent intent) {
        Log.v(TAG, "onMessage");
        // Notify to C# layer
        Bundle bundle = intent.getExtras();
        Set<String> keys = bundle.keySet();
        JSONObject json = new JSONObject();
        String jsonMessage = null;
        try {
            for (String key : keys) {
                Log.v(TAG, key + ": " + bundle.get(key));
                json.put(key, bundle.get(key));
            }
            jsonMessage = json.toString();
            Util.sendMessage(ON_MESSAGE, jsonMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!Util.notificationsEnabled) {
            return;
        }

        // Show native notification view in status bar if defined fields are put.
        try {
            String aps = intent.getStringExtra("aps");
            Log.d(TAG, "APS string: " + aps);
            if (aps != null) {
                JSONObject obj = new JSONObject(aps);
                Log.d(TAG, "JSON: " + obj);

                //processing alert
                if (obj.has("alert")) {
                    processAlert(obj.getJSONObject("alert"), context, jsonMessage);
                    Log.d(TAG, "alert processed");
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "onMessage failure. " + e);
        }
    }

    protected void processAlert(JSONObject alert, Context context, String Extras) throws JSONException {

        if (alert.isNull("loc-key")) {
            Log.d(TAG, "loc-key is null!");
            return;
        }

        String locKey = alert.getString("loc-key");
        String message;

        int locKeyId = context.getResources().getIdentifier(locKey, "string", context.getPackageName());

        Log.d(TAG, "loc-key resource id = " + locKeyId);
        if (alert.has("loc-args")) {
            JSONArray locArgs = alert.getJSONArray("loc-args");
            int size = locArgs.length();

            Object[] args = new Object[size];
            for (int i = 0; i < size; ++i) {
                args[i] = locArgs.get(i);
            }

            message = (locKeyId != 0) ? context.getString(locKeyId, args) : String.format(locKey, args);
        } else {
            message = (locKeyId != 0) ? context.getString(locKeyId) : locKey;
        }

        UnityGCMNotificationManager.showNotification(this, message, Extras);
    }

    protected void onDeletedMessages(Context context, String total) {
        Log.v(TAG, "onDeleteMessages");
        Util.sendMessage(ON_DELETE_MESSAGES, total);
    }

}
