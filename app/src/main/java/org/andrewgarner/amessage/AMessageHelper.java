package org.andrewgarner.amessage;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Helper class to access database for messages
 * Created by andrewgarner on 3/27/15.
 */
public class AMessageHelper {
    private String TAG;
    MyDBHelper DB;
    AUserHelper AUH;

    public AMessageHelper(Context context){
        DB = new MyDBHelper(context);
        TAG = context.getString(R.string.TAG);
        AUH = new AUserHelper(context);
    }

    /**
     * Private method to assist the other methods in this helper class.
     * Pass in the cursor result from a DB selection and it will return a message object
     * @param cursor cursor result from a db selection
     * @return AMessage object
     */
    private AMessage selectMessageHelper(Cursor cursor){
        String mid     = cursor.getString(cursor.getColumnIndex(MyDBHelper.MID));
        String touid   = cursor.getString(cursor.getColumnIndex(MyDBHelper.UID));
        String text  = cursor.getString(cursor.getColumnIndex(MyDBHelper.MESSAGE_TEXT));
        long date  = Long.parseLong(cursor.getString(cursor.getColumnIndex(MyDBHelper.MESSAGE_DATE)));

        String selfString  = cursor.getString(cursor.getColumnIndex(MyDBHelper.SELF));
        boolean self = selfString.equals("1");
        AMessage temp = new AMessage(mid, touid, self);
        temp.setText(text);
        temp.setDate(date);
        return temp;
    }

    /**
     * Find all messages from a user by their UID
     * @param uid UID
     * @return
     */
    public List<AMessage> selectMessagesByUID(String uid){
        List<AMessage> messageList = new ArrayList<>();
        Cursor cursor = DB.selectMessagesByUID(uid);
        do{
            messageList.add(selectMessageHelper(cursor));

        }while (cursor.moveToNext());
        return messageList;
    }

    /**
     * Find a message by it's message ID (MID)
     * @param mid id of the message
     * @return a message
     */
    public AMessage selectMessageByMID(String mid){
        List<AMessage> messageList = new ArrayList<>();
        Cursor cursor = DB.selectMessagesByMID(mid);
        do{
            messageList.add(selectMessageHelper(cursor));

        }while (cursor.moveToNext());
        return messageList.get(0);
    }
    public List<AMessage> selectRecentMessages(){
        List<AMessage> messageList = new ArrayList<>();
        Cursor cursor = DB.selectRecentMessages();
        do{
            messageList.add(selectMessageHelper(cursor));
        }while (cursor.moveToNext());

        for (Iterator<AMessage> iterator = messageList.iterator(); iterator.hasNext();) {
            AMessage msg = iterator.next();
            if (!AUH.isFriend(AUH.selectUserByUID(msg.getUID()))) {
                iterator.remove();
            }
        }
        return messageList;
    }

    /**
     * Method for manually creating a message and storing it into DB. Returns message object.
     * @param uid User ID
     * @param text Message text
     * @param messagePhoto Path to photo
     * @param self If it is sent from the logged-in user
     * @return AMessage object
     */
    public AMessage createMessageReturnAMessage(String uid, String text, String messagePhoto, boolean self){
        long messageDate = System.currentTimeMillis();
        String  uuid = UUID.randomUUID().toString();
        try{
            long rowNumber = DB.createMessage(uuid, uid, text, messagePhoto, messageDate, self);
            //Log.i(TAG, "Message rowNumber = "+rowNumber);
            return selectMessageByMID(uuid);
        }catch (Exception e){
            Log.i(TAG, "AMessageHelper / Error adding Message");
        }
        return selectMessageByMID(uuid);
    }

    /**
     *
     * @param msg AMessage object
     * @return
     */
    public boolean createMessage(AMessage msg){
        try{
            long rowNumber = DB.createMessage(msg);
            //Log.i(TAG, "Message rowNumber = "+rowNumber);
            return true;
        }catch (Exception e){
            Log.i(TAG, "AMessageHelper / Error adding Message");
        }
        return false;
    }


}
