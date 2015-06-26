package org.andrewgarner.amessage;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrewgarner on 3/27/15.
 */
public class AUserHelper {

    private String TAG;
    private final String MY_PREFS;
    MyDBHelper DB;
    Context mContext;

    public AUserHelper(Context context){
        DB = new MyDBHelper(context);
        mContext = context;
        TAG = mContext.getString(R.string.TAG);
        MY_PREFS = mContext.getString(R.string.MY_PREFS);

    }

    private AUser selectUserHelper(Cursor cursor){
        String uid = cursor.getString(cursor.getColumnIndex(MyDBHelper.UID));
        String name = cursor.getString(cursor.getColumnIndex(MyDBHelper.NAME));
        String email = cursor.getString(cursor.getColumnIndex(MyDBHelper.EMAIL));
        String photo_id = cursor.getString(cursor.getColumnIndex(MyDBHelper.PHOTO_ID));

        boolean photo_id_device = cursor.getString(cursor.getColumnIndex(MyDBHelper.PHOTO_ID_DEVICE)).equals("1");
        boolean self = cursor.getString(cursor.getColumnIndex(MyDBHelper.SELF)).equals("1");
        boolean friend = cursor.getString(cursor.getColumnIndex(MyDBHelper.FRIEND)).equals("1");

        String lastSeenString = cursor.getString(cursor.getColumnIndex(MyDBHelper.LAST_SEEN));
        long lastSeen;
        try {
            lastSeen = Long.parseLong(lastSeenString);
        } catch (Exception e) {
            Log.i(TAG, "UHelper / selectUserHelperID / null lastSeen");
            lastSeen = 0;
        }

        //Log.v(TAG, uid2 + " " + name + " " + email + " " + photo_id + " " +self+ " "+lastSeen);
        AUser temp = new AUser(uid, name, email, photo_id, self, friend);
        temp.setLastSeen(lastSeen);
        temp.setPhoto_Is_On_Device(photo_id_device);

        return temp;
    }
    private ARequest selectRequestHelper(Cursor cursor){
        String rid = cursor.getString(cursor.getColumnIndex(MyDBHelper.RID));
        String uid = cursor.getString(cursor.getColumnIndex(MyDBHelper.UID));
        String message = cursor.getString(cursor.getColumnIndex(MyDBHelper.REQUEST_MESSAGE));
        String date = cursor.getString(cursor.getColumnIndex(MyDBHelper.REQUEST_DATE));
        String selfString = cursor.getString(cursor.getColumnIndex(MyDBHelper.SELF));

        AUser user = selectUserByUID(uid);
        return new ARequest(rid, user, selfString,message, date);
    }
    /**************************************************/
    public AUser selectUserByUID(String uid){
        //Log.i(TAG, "-UH / SelectUserbyid= " + uid + "-");
        Cursor cursor = DB.selectUserByUID(uid);
        if(cursor.getCount() >= 1) {
           return selectUserHelper(cursor);
        } else{
            Log.e(TAG, "AUserHelper / selectUserByUID / No results!");
            return null;
        }
    }
    public AUser selectUserBySelf(){
        Cursor cursor = DB.selectUserBySelf();
        if(cursor.getCount() >= 1) {
            return selectUserHelper(cursor);
        } else{
            Log.e(TAG, "AUserHelper / selectUserByUID / No results!");
            return null;
        }
    }

    public boolean isUser(AUser user){
        Cursor cursor = DB.selectUserByUID(user.getUID());
        if(cursor.getCount() >= 1) {
            return true;
        } else{
            Log.e(TAG, "AUserHelper / selectUserByUID / No results!");
            return false;
        }
    }
    public boolean isFriend(AUser user){
        Cursor cursor = DB.selectFriendByUID(user.getUID());
        if(cursor.getCount() >= 1) {
            return true;
        } else{
            //Log.e(TAG, "AUserHelper / selectFriendByUID / No results!");
            return false;
        }
    }

    public List<AUser> selectAllUsersList() {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        List<AUser> userList = new ArrayList<>();
        Cursor cursor = DB.selectAllUsers();
        if (cursor.getCount() >= 1){
            do {
                userList.add(selectUserHelper(cursor));
            } while (cursor.moveToNext());
        return userList;
        }else{
            //Log.e(TAG, "AUserHelper / selectAllUserList / No results!");
            return new ArrayList<>();
        }
    }

    public List<AUser> selectAllFriendsList() {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        List<AUser> userList = new ArrayList<>();
        Cursor cursor = DB.selectAllFriends();
        if (cursor.getCount() >= 1){
            do {
                userList.add(selectUserHelper(cursor));
            } while (cursor.moveToNext());
            return userList;
        }else{
            //Log.e(TAG, "AUserHelper / selectAllFriendsList / No results!");
            return new ArrayList<>();
        }
    }
    public List<AUser> selectAllFriendsOrRequestsList() {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        List<AUser> userList = new ArrayList<>();
        Cursor cursor = DB.selectAllFriendsOrRequests();
        if (cursor.getCount() >= 1){
            do {
                userList.add(selectUserHelper(cursor));
            } while (cursor.moveToNext());
            return userList;
        }else{
            Log.e(TAG, "AUserHelper / selectAllFriendsList / No results!");
            return new ArrayList<>();
        }
    }

    public List<AUser> selectAllUsersNoPictureList(){
        //Log.i(TAG, "-UH / SelectAllUsersNoPictureList-");
        List<AUser> userList = new ArrayList<>();
        Cursor cursor = DB.selectAllUsersNoPicture();
        if (cursor.getCount() >= 1) {
            do {
                userList.add(selectUserHelper(cursor));
            } while (cursor.moveToNext());
            return userList;
        }else{
                Log.e(TAG, "AUserHelper / selectAllUserList / No results!");
                return new ArrayList<>();
            }
    }

    public boolean createUser(AUser user){
        Log.i(TAG, "Create User"+user.getEmail()+"...");
        try {
            long rowNumber = DB.createUser(user);
            Log.i(TAG, "AUH / CreateUser"+user.getEmail()+" rowNumber = "+rowNumber);
            try{
                DB.deleteRequestsByUID(user.getUID());
            }catch(Exception e){
                e.printStackTrace();
            }
            return true;
        }catch(SQLiteConstraintException e){
            Log.e(TAG, "AUserHelper / This email ("+user.getEmail()+") is already taken");
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean createRequest(ARequest request){
        try {
           // try {
           //     DB.createUser(request.getUser());
           // }catch(Exception e){Log.e(TAG,"AUserHelper / CreateRequest / user error: "+e);}

            if(!hasRequest(request.getUser())) {
                long rowNumber = DB.createRequest(request);
                Log.i(TAG, "AUH / Request / " + request.getUser().getName() + " rowNumber = " + rowNumber);
                return true;
            }else{
                Log.i(TAG, "AUH / Request / user already has a request");
            }
        }catch(SQLiteConstraintException e){
            Log.e(TAG, "AUserHelper / Request Add Error"+e);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean hasRequest(AUser user){
        try{
            Cursor c = DB.selectRequestByUser(user);
            Log.i(TAG, "AUH / hasRequest / User has "+c.getCount()+ " requests");
            return c.getCount()>=1;
        }catch(Exception e){
            return false;
        }
    }
    public boolean hasSentRequestTo(AUser user){
        try{
            Cursor c = DB.selectRequestToUser(user.getUID());
            //Log.i(TAG, "AUH / hasSentRequestTo / I've sent "+c.getCount()+ " requests to "+user.getEmail());
            return c.getCount()>=1;
        }catch(Exception e){
            return false;
        }
    }
    public boolean hasRequestFrom(AUser user){
        try{
            Cursor c = DB.selectRequestFromUser(user);
            //Log.i(TAG, "AUH / hasSentRequestFrom / I've received "+c.getCount()+ " requests From "+user.getEmail());
            return c.getCount()>=1;
        }catch(Exception e){
            return false;
        }
    }
    public void acceptRequest(ARequest request){

        try{
            DB.deleteRequestsByUID(request.getUser().getUID());
            DB.updateFriendStatus(request.getUser().getUID(), true);
        }catch(SQLiteConstraintException e){
            Log.e(TAG, "AUserHelper / Accept Friend Error"+e);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void rejectRequest(ARequest request){
        try{
            DB.deleteRequestByRID(request.getRID());
        }catch(SQLiteConstraintException e){
            Log.e(TAG, "AUserHelper / Accept Friend Error"+e);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public List<ARequest> selectAllRequests() {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        List<ARequest> requestList = new ArrayList<>();
        Cursor cursor = DB.selectAllRequests();
        if (cursor.getCount() >= 1){
            do {
                requestList.add(selectRequestHelper(cursor));
            } while (cursor.moveToNext());
            return requestList;
        }else{
            Log.e(TAG, "AUserHelper / selectAllRequests / No results!");
            return new ArrayList<>();
        }
    }
    public ARequest selectRequestByUser(AUser user) {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        Cursor cursor = DB.selectRequestByUser(user);
        if (cursor.getCount() >= 1){
            return selectRequestHelper(cursor);
        }
        Log.e(TAG, "AUserHelper / selectAllRequests / No results!");
        return null;
    }
    public ARequest selectRequestFromUser(AUser user) {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        Cursor cursor = DB.selectRequestFromUser(user);
        if (cursor.getCount() >= 1){
            return selectRequestHelper(cursor);
        }
        Log.e(TAG, "AUserHelper / selectAllRequests / No results!");
        return null;
    }
    public List<AUser> selectAllUsersWithRequestsList() {
        //Log.i(TAG, "-UH / User id= " + uid + "-");
        List<AUser> userList = new ArrayList<>();
        Cursor cursor = DB.selectAllUSersWithRequests();
        if (cursor.getCount() >= 1){
            do {
                userList.add(selectUserHelper(cursor));
            } while (cursor.moveToNext());
            return userList;
        }else{
            Log.e(TAG, "AUserHelper / selectAllUserRequestsList / No results!");
            return new ArrayList<>();
        }
    }

    public void deleteRequest(String rid){
        try{
            long result = DB.deleteRequestByRID(rid);
            Log.i(TAG, "deleteUserUID result = "+result);
        }catch (Exception e){
            Log.v(TAG, "AUserHelper / Delete User Exception!");
        }
    }
    public boolean setUserFriendStatus(AUser user, boolean isFriend){
        try {
            DB.updateFriendStatus(user.getUID(), isFriend);
            return true;
        }catch (Exception e){
            Log.e(TAG, "AUserHelper / Error in setting Friend Status");
        }
        return false;
    }

    public void setUserLastSeen(String uid){
        try {
            long time = System.currentTimeMillis();
            DB.updateLastSeen(uid, time);
        }catch (Exception e){
            Log.e(TAG, "AUserHelper / Error in setting Last Seen");
        }
    }

    public void setUserPicture(String UID, boolean isSet){
        try{
            DB.updateUserPictureIsSet(UID, isSet);
        } catch(Exception e){
            Log.e(TAG, "AUserHelper / Error setting picture");
        }

    }

    public String getSelfGCMID(){
        String gcm;
        SharedPreferences prefs = mContext.getSharedPreferences(MY_PREFS, mContext.MODE_PRIVATE);
        gcm = prefs.getString(mContext.getString(R.string.MY_PREFS_GCM_Registration_ID),"");

        if(gcm==null){
            return "";
        }
        if(gcm.equals("")){
            Log.e(TAG, "*** AUserHelper / Self GCM ID is null!! ***");
        }
        return gcm;
    }

    public String getSelfID(){
        String uid;
        SharedPreferences prefs = mContext.getSharedPreferences(MY_PREFS, mContext.MODE_PRIVATE);
        uid = prefs.getString(mContext.getString(R.string.MY_PREFS_MyUUID),"");
        //if(uid!=null){
        if(uid==null || uid.equals(""))
            uid = getSelfIDFromDB();
        if(uid==null || uid.equals("")){
            Log.e(TAG, "*** AUserHelper / Self ID is null!! ***");
            return "";
        }
        return uid;
    }

    public String getSelfIDFromDB(){
        try {
            Cursor cursor = DB.selectUserBySelf();
            return cursor.getString(cursor.getColumnIndex(MyDBHelper.UID));
        }catch(Exception e){
            Log.e(TAG, "AUserHelper / getSelfIDFromDB Error!");
            return "";
        }

    }

    public void setMyUUIDPref(String uuid){
        SharedPreferences.Editor editor = mContext.getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE).edit();
        editor.putString(mContext.getString(R.string.MY_PREFS_MyUUID), uuid);
        editor.putBoolean(mContext.getString(R.string.MY_PREFS_FirstRun), false);
        editor.apply();

    }

    public void deleteUserByUID(String UID){
        try{
            long result = DB.deleteUserByUID(UID);
            Log.i(TAG, "deleteUserUID result = "+result);
        }catch (Exception e){
            Log.v(TAG, "AUserHelper / Delete User Exception!");
        }
    }
    public void deleteUserByEmail(String email){
        try{
            long result = DB.deleteUserByEmail(email);
            Log.i(TAG, "deleteUserEmail result = "+result);
        }catch (Exception e){
            Log.v(TAG, "AUserHelper / Delete User Exception!");
        }
    }
}
