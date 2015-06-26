package org.andrewgarner.amessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Class to handle sending notifications
 * Created by Andrew on 6/3/2015.
 */
public class NotificationHelper {
    private Context context;
    private String TAG;
    private NotificationManager mNotificationManager;

    public NotificationHelper(Context context){
        this.context = context;
        TAG = context.getString(R.string.TAG);
    }

    /**
     * Notification when a message comes in
     * @param msg message that was received
     */
    public void messageNotification(AMessage msg) {
        AUserHelper UH = new AUserHelper(context);
        BitmapHelper BH = new BitmapHelper(context);
        String uid = msg.getUID();
        String name = UH.selectUserByUID(uid).getName();
        String text = msg.getText();

        AUser user = UH.selectUserByUID(msg.getUID());

        //format the text properly
        text = text.replace("\\n","\n");
        text = text.replace("\\'","'");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Create an intent that bundles the user that sent the message to send into the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.addCategory(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(context.getString(R.string.bundle_message_user), user);

        //add flags to the notification such that it doesn't open a new instance of the app if already open
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //create a pendingintent with the above intent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        //build the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(name)
                        .setLargeIcon(BH.getUserBitmap(uid))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text))
                        .setContentText(text);

        //set the sound when it comes in
        mBuilder.setSound(alarmSound);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setPriority(Notification.PRIORITY_HIGH);

        //vibrate the device in two 85 ms intervals with 85ms between
        mBuilder.setVibrate(new long[] { 0, 85,85,85});
        mBuilder.setAutoCancel(true);

        //create a notification ID based off of the user's UID in order to cancel
        //the notification it if we are on the conversation page for that user
        int id=Integer.parseInt(uid.substring(0,8).replaceAll("[\\D]", ""));
        Log.e(TAG, "GCM NOTIF ID= "+id);
        mNotificationManager.notify(id, mBuilder.build());
    }

    /**
     * Notification when a user accepts their friend request
     * See comments on messageNotification(), they are nearly the same
     * @param user user that accepted the friend request
     */
    public void userNotification(AUser user) {
        BitmapHelper BH = new BitmapHelper(context);
        String uid = user.getUID();
        String name = user.getName();
        String text = "Added you as a friend. Say hello!";

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addCategory(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(context.getString(R.string.bundle_message_user), user);

        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(name)
                        .setLargeIcon(BH.getUserBitmap(uid))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text))
                        .setContentText(text);

        mBuilder.setSound(alarmSound);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setPriority(Notification.PRIORITY_HIGH);


        mBuilder.setVibrate(new long[] { 0, 85,85,85});
        mBuilder.setAutoCancel(true);

        int id=Integer.parseInt(uid.substring(0,8).replaceAll("[\\D]", ""));
        Log.e(TAG, "GCM NOTIF ID= "+id);
        mNotificationManager.notify(id, mBuilder.build());
    }


    /**
     * Notification for when a friend request comes in
     * See comments on messageNotification(), they are nearly the same
     * @param request friend request from another user
     */
    public void requestNotification(ARequest request) {
        AUserHelper UH = new AUserHelper(context);
        String uid = request.getUID();
        String name = "Request from "+UH.selectUserByUID(uid).getName();
        String text = request.getRequest_message();

        text = text.replace("\\n","\n");
        text = text.replace("\\'","'");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addCategory(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(context.getString(R.string.bundle_finder_page), 1);
        intent.putExtra(context.getString(R.string.bundle_pager_position), 0);

        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(name)
                                //.setLargeIcon(BH.getUserBitmap(uid))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text))
                        .setContentText(text);

        mBuilder.setSound(alarmSound);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setPriority(Notification.PRIORITY_HIGH)
                .setVibrate(new long[300]);

        int id=Integer.parseInt(uid.substring(0,8).replaceAll("[\\D]", ""));
        Log.e(TAG, "GCM NOTIF ID= "+id);
        mNotificationManager.notify(id, mBuilder.build());
    }

    public void clearNotificationsForUser(AUser user){
        NotificationManager notifMgr = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = Integer.parseInt(user.getUID().substring(0, 8).replaceAll("[\\D]", ""));
        notifMgr.cancel(id);
    }
}
