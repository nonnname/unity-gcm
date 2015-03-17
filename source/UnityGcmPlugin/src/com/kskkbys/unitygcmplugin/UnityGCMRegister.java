package com.kskkbys.unitygcmplugin;

import android.os.AsyncTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Plugin class of GCMRegister
 *
 * @author Keisuke Kobayashi
 */
public class UnityGCMRegister {

    private static final String TAG = UnityGCMRegister.class.getSimpleName();

    private static String gcmId = "";
    private static Context context = null;

    public static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    public static void register(final String senderIds) {
        if (TextUtils.isEmpty(senderIds)) {
            return;
        }

        Activity activity = UnityPlayer.currentActivity;
        String[] senderIdArray = senderIds.split(",");
        if (!"".equals(gcmId)) {
            Util.sendMessage(UnityGCMIntentService.ON_REGISTERED, gcmId);
        } else {
            new UnityGCMAsyncRegister().execute(senderIdArray);
        }
    }

    public static void unregister() {
        Activity activity = UnityPlayer.currentActivity;
        try {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity.getBaseContext());
            if (gcm != null)
                gcm.unregister();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isRegistered() {
        return !"".equals(gcmId);
    }

    public static String getRegistrationId() {
        return gcmId;
    }

    public static void setNotificationsEnabled(boolean enabled) {
        Log.v(TAG, "setNotificationsEnabled: " + enabled);
        Util.notificationsEnabled = enabled;
    }

    private static class UnityGCMAsyncRegister extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                if (checkPlayServices(context)) {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    return gcm.register(strings);
                } else {
                    Log.i(TAG, "No valid Google Play Services APK found.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Registration failure", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String registrationId) {
            if (!"".equals(registrationId)) {
                gcmId = registrationId;
                Util.sendMessage(UnityGCMIntentService.ON_REGISTERED, registrationId);
            }else
            Log.w(TAG, "UnityGCMAsyncRegister registrationId is empty");
        }
    }
}
