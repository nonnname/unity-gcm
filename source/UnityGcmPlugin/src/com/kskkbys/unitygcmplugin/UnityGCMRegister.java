package com.kskkbys.unitygcmplugin;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
    private static final String PREF_KEY = "UnityGCMRegister";
    private static final String PROPERTY_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "gcm_app_version";

    protected static String getCachedRegistrationId()
    {
        Context context = UnityPlayer.currentActivity.getBaseContext();
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersionCode(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    protected static void setCachedRegistrationId(String registrationId)
    {
        Context context = UnityPlayer.currentActivity.getBaseContext();
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersionCode = getAppVersionCode(context);

        Log.i(TAG, "Saving regId on app version code " + appVersionCode);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, registrationId);
        editor.putInt(PROPERTY_APP_VERSION, appVersionCode);
        editor.commit();

        Util.sendMessage(UnityGCMIntentService.ON_REGISTERED, registrationId);
    }

    private static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
    }

    private static int getAppVersionCode(Context context)
    {
        int version = 0;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException", e);
        }

        return version;
    }

    public static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    public static void register(final String senderIds) {
        String cachedId = getCachedRegistrationId();
        if (isRegistered()) {
            Util.sendMessage(UnityGCMIntentService.ON_REGISTERED, cachedId);
            return;
        }

        if (TextUtils.isEmpty(senderIds)) {
            return;
        }

        String[] senderIdArray = senderIds.split(",");
        new UnityGCMAsyncRegister().execute(senderIdArray);
    }

    public static void unregister() {
        try {
            Activity activity = UnityPlayer.currentActivity;
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity.getBaseContext());
            if (gcm != null)
                gcm.unregister();

            setCachedRegistrationId("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isRegistered() {
        String cachedRegId = getCachedRegistrationId();
        return !cachedRegId.isEmpty();
    }

    public static void setNotificationsEnabled(boolean enabled) {
        Log.v(TAG, "setNotificationsEnabled: " + enabled);
        Util.notificationsEnabled = enabled;
    }

    public static boolean hasDelayedNotifications()
    {
        Log.v(TAG, "hasDelayedNotifications");

        if(UnityPlayer.currentActivity == null)
        {
            Log.v(TAG, "UnityPlayer.currentActivity == null");
            return false;
        }

        android.content.Intent intent = UnityPlayer.currentActivity.getIntent();

        if(intent != null){
            int intentID = intent.getIntExtra(UnityGCMNotificationManager.INTENT_ID_NAME, -1);
            if(intentID == UnityGCMNotificationManager.INTENT_ID)
            {
                return true;
            }
        }

        return false;
    }

    public static void processDelayedNotifications() {
        Log.v(TAG, "processDelayedNotifications");

        if(UnityPlayer.currentActivity == null)
        {
            Log.v(TAG, "UnityPlayer.currentActivity == null");
            return;
        }

        android.content.Intent intent = UnityPlayer.currentActivity.getIntent();

        if(intent != null)
        {
            String notifMessage = intent.getStringExtra(UnityGCMNotificationManager.NOTIFICATION_MESSAGE);
            if(notifMessage != null)
            {
                Util.sendMessage(UnityGCMIntentService.ON_MESSAGE, notifMessage);
                Log.d(TAG, "Sending JSON: " + notifMessage);
            }
        }

    }


    private static class UnityGCMAsyncRegister extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                Activity activity = UnityPlayer.currentActivity;
                Context context = activity.getBaseContext();
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
            if (registrationId != null && !registrationId.isEmpty()) {
                setCachedRegistrationId(registrationId);
            } else {
                Log.w(TAG, "UnityGCMAsyncRegister registrationId is empty");
            }
        }
    }
}
