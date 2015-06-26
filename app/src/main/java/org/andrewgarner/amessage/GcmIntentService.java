package org.andrewgarner.amessage;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Class to handle GCM messages. Unbundles the information inside and sends it to the database
 * and informs the app of any changes. Also sends out notifications if needed
 * Created by Andrew on 4/10/2015.
 */
public class GcmIntentService extends IntentService implements BitmapHelper.BitmapHelperCallback{
    private static final String TAG = "andrewgarner";

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "GcmIntentService!");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                //sendNotification(null, "AMessage Error","Send error: " + extras.toString());
                Log.e(TAG, "AMessage Send Error");
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                //sendNotification(null, "AMessage Error","Deleted messages on server: " +extras.toString());
                Log.e(TAG, "AMessage Send Error: DELETED");
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.v(TAG, "GCMIntent / extras.toString()="+extras.toString());

                ServerReader sr = new ServerReader(this);
                ServerResponse SR = sr.readMessageAddToDB(extras.getString("message"));

                NotificationHelper NH = new NotificationHelper(this);

                //Check if the serverResponse has a message, user, or request.
                //if it needs to be sent out and/or a notification needs to be made, do so.
                if (SR.hasMessage()) {
                    //if the serverResponse has a message, handle it
                    AMessage msg = SR.getFirstMessage();
                    //send a notification if message is from another user
                    if(!msg.isSelf())
                        NH.messageNotification(msg);
                    try {
                        //send the broadcast to any listeners currently running
                        sendBroadcast(msg);
                    }catch(Exception e){
                        Log.e(TAG, "GCMIntent / Broadcast error!");
                    }
                } else if (SR.hasUser()){
                    AUser user = SR.getFirstUser();
                    if(user.isFriend() && user.shouldNotify()){
                        NH.userNotification(user);
                    } else if(!user.isFriend()){
                        NH.clearNotificationsForUser(user);
                    }
                    try {
                        sendBroadcast(user);
                    }catch(Exception e){
                        Log.e(TAG, "GCMIntent / Broadcast error!");
                    }

                } else if (SR.hasRequest()){
                    ARequest request = SR.getFirstRequest();
                    if(!request.isSelf()){
                        NH.requestNotification(request);
                    }
                    try {
                        sendBroadcast(request);
                    }catch(Exception e){
                        Log.e(TAG, "GCMIntent / Broadcast error!");
                    }

                }

            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }


    //The following three functions send out broadcasts for their respective data types
    //The broadcasts are received within the app (If it is running) and are handled accordingly
    //The message, user, or request is sent as a parcelable in an intent
    private void sendBroadcast(AMessage amsg) {
        Intent intent = new Intent("NewMessageIntent");
        intent.putExtra("message", amsg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void sendBroadcast(AUser user) {
        Intent intent = new Intent("NewMessageIntent");
        intent.putExtra("user", user);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void sendBroadcast(ARequest request) {
        Intent intent = new Intent("NewMessageIntent");
        intent.putExtra("request", request.getUser());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Unused function inherited from bitmapHelper
     */
    @Override
    public void bitmapDownloadPicFinished(){}
}

