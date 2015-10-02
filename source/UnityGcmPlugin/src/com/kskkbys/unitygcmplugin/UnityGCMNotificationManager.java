package com.kskkbys.unitygcmplugin;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Notification manager class.
 *
 * @author Keisuke Kobayashi
 */
public class UnityGCMNotificationManager {

    private static final String TAG = UnityGCMNotificationManager.class.getSimpleName();

    // Request code for launching unity activity
    private static final int REQUEST_CODE_UNITY_ACTIVITY = 1001;
    // ID of notification
    private static final int ID_NOTIFICATION = 1;
    public static final int INTENT_ID = 101;
    public static final String INTENT_ID_NAME = "uniqueID";
    public static final String NOTIFICATION_MESSAGE = "notifMessage";

    /**
     * Show notification view in status bar
     *
     * @param context
     * @param contentText
     */
    public static void showNotification(Context context, String contentText, String Extras) {
        Log.v(TAG, "showNotification");

        // Intent
        Intent intent = new Intent(context, UnityPlayerActivity.class);
        intent.putExtra(INTENT_ID_NAME, INTENT_ID);
        intent.putExtra(NOTIFICATION_MESSAGE, Extras);
        PendingIntent contentIntent = PendingIntent.getActivity(context, REQUEST_CODE_UNITY_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        int titleResourceId = context.getResources().getIdentifier("notificationTitle", "string", context.getPackageName());
        String contentTitle = titleResourceId != 0 ? context.getString(titleResourceId) : "";

        //　Show notification in status bar
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext());
        builder.setContentIntent(contentIntent);
        builder.setContentTitle(contentTitle);
        builder.setContentText(contentText);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);

        Resources res = context.getResources();

        boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        if (whiteIcon)
        {
            builder.setSmallIcon(res.getIdentifier("app_icon_silhouette", "drawable", context.getPackageName()));
        }
        else
        {
            builder.setSmallIcon(res.getIdentifier("app_icon", "drawable", context.getPackageName()));
        }

        builder.setDefaults(Notification.DEFAULT_SOUND);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_NOTIFICATION, builder.build());
    }

    public static void clearAllNotifications() {
        Log.v(TAG, "clearAllNotifications");

        NotificationManager nm = (NotificationManager) UnityPlayer.currentActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

}
