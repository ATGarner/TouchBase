package org.andrewgarner.amessage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by Andrew on 3/15/2015.
 */
public class MyDBHelper {
    private MyDB DB;

    private SQLiteDatabase database;

    private final String TAG = "andrewgarner";

    public static final String USER_TABLE = MyDB.USER_TABLE;
    public static final String MESSAGE_TABLE = MyDB.MESSAGE_TABLE;
    public static final String REQUEST_TABLE = MyDB.REQUEST_TABLE;

    public static final String MID = MyDB.MID;
    public static final String UID = MyDB.UID;

    public static final String MESSAGE_TEXT = MyDB.MESSAGE_TEXT;
    public static final String MESSAGE_PHOTO = MyDB.MESSAGE_PHOTO;
    public static final String MESSAGE_DATE = MyDB.MESSAGE_DATE;

    public static final String NAME = MyDB.NAME;
    public static final String PHOTO_ID = MyDB.PHOTO_ID;
    public static final String PHOTO_ID_DEVICE = MyDB.PHOTO_ID_DEVICE;
    public static final String EMAIL = MyDB.EMAIL;
    public static final String SELF = MyDB.SELF;
    public static final String LAST_SEEN = MyDB.LAST_SEEN;
    public static final String RID = MyDB.RID;
    public static final String REQUEST_MESSAGE = MyDB.REQUEST_MESSAGE;
    public static final String FRIEND = MyDB.FRIEND;
    public static final String REQUEST_DATE = MyDB.REQUEST_DATE;

    public static final String[] USER_FIELDS = MyDB.USER_FIELDS;
    public static final String[] MESSAGE_FIELDS = MyDB.MESSAGE_FIELDS;
    public static final String[] REQUEST_FIELDS = MyDB.REQUEST_FIELDS;


    /**
     * Start up Database helper,
     * provides functions to simplify database queries
     * @param context
     */
    public MyDBHelper(Context context){
        DB = MyDB.getHelper(context);
        database = DB.getWritableDatabase();
    }

    /**
     * Deletes all tuples from the database, be careful when using!
     */
    public void clearDB(){
        DB.onUpgrade(database, 1, 2);
    }


    public long createUser(AUser user) throws SQLiteConstraintException {
        ContentValues values = new ContentValues();
        values.put(UID, user.getUID());
        values.put(NAME, user.getName());
        values.put(PHOTO_ID, user.getPhoto_ID());
        values.put(EMAIL, user.getEmail());

        if(user.getPhoto_Is_On_Device()) {
            values.put(PHOTO_ID_DEVICE, "1");
        }else{
            values.put(PHOTO_ID_DEVICE, "0");
        }

        if(user.isSelf()) {
            values.put(SELF, "1");
        }else{
            values.put(SELF, "0");
        }
        if(user.isFriend()) {
            values.put(FRIEND, "1");
        }else{
            values.put(FRIEND, "0");
        }

        values.put(LAST_SEEN, "0");


        long line = database.insertWithOnConflict(USER_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if(line != -1){
            return line;
        } else {
            throw new SQLiteConstraintException();
        }
    }

    public long createMessage(String mid, String touid, String text, String messagePhoto, long messageDate, boolean self) throws SQLiteConstraintException{
        ContentValues values = new ContentValues();
        values.put(MID,mid);
        values.put(UID,touid);
        values.put(MESSAGE_TEXT,text);
        values.put(MESSAGE_PHOTO,messagePhoto);
        values.put(MESSAGE_DATE, messageDate);

        if(self) {
            values.put(SELF, "1");
        }else{
            values.put(SELF, "0");
        }

        long line = database.insertWithOnConflict(MESSAGE_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if(line != -1){
            return line;
        }else{
            throw new SQLiteConstraintException();
        }
    }
    public long createMessage(AMessage msg) throws SQLiteConstraintException{
        ContentValues values = new ContentValues();
        values.put(MID,msg.getMID());
        values.put(UID,msg.getUID());
        values.put(MESSAGE_TEXT,msg.getText());
        values.put(MESSAGE_PHOTO,msg.getPhoto());
        values.put(MESSAGE_DATE, msg.getDate());

        if(msg.isSelf()) {
            values.put(SELF, "1");
        }else{
            values.put(SELF, "0");
        }

        long line = database.insertWithOnConflict(MESSAGE_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if(line != -1){
            return line;
        }else{
            throw new SQLiteConstraintException();
        }
    }

    public long createRequest(ARequest request) throws SQLiteConstraintException{
        ContentValues values = new ContentValues();
        values.put(RID,request.getRID());
        values.put(UID,request.getUID());
        values.put(REQUEST_MESSAGE,request.getRequest_message());
        values.put(REQUEST_DATE,request.getDate());

        if(request.isSelf()) {
            values.put(SELF, "1");
        }else{
            values.put(SELF, "0");
        }

        long line = database.insertWithOnConflict(REQUEST_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if(line != -1){
            return line;
        }else{
            throw new SQLiteConstraintException();
        }
    }

    /*--    UPDATE      --*/

    public int updateLastSeen(String uid, long time){
        ContentValues values = new ContentValues();
        values.put(LAST_SEEN, time);
        String whereClause = UID+"=? ";
        String[] whereArgs = {uid};

        int rows = database.updateWithOnConflict(USER_TABLE, values, whereClause, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
        Log.i(TAG, "MyDBHelper / updateLastSeen/ rows="+rows);

        return rows;
    }

    public int updateUserPictureIsSet(String uid, boolean isSet){
        ContentValues values = new ContentValues();
        if(isSet) {
            values.put(PHOTO_ID_DEVICE, "1");
        }else{
            values.put(PHOTO_ID_DEVICE, "0");
        }
        String whereClause = UID+"=? ";
        String[] whereArgs = {uid};

        int rows = database.updateWithOnConflict(USER_TABLE, values, whereClause, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
        //Log.i(TAG, "MyDBHelper / updateLastSeen/ rows="+rows);

        return rows;
    }

    public int updateFriendStatus(String uid, boolean isFriend){
        ContentValues values = new ContentValues();
        values.put(FRIEND, isFriend);
        if(isFriend) {
            values.put(FRIEND, "1");
        }else{
            values.put(FRIEND, "0");
        }
        String whereClause = UID+"=? ";
        String[] whereArgs = {uid};

        int rows = database.updateWithOnConflict(USER_TABLE, values, whereClause, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
        Log.i(TAG, "MyDBHelper / updateFriendStatus/ rows="+rows);

        return rows;
    }
    /*--    DELETE      --*/

    public long deleteUserByEmail(String email){
        String whereClause = EMAIL+"=?";
        String[] whereArgs = {email};
        return database.delete(USER_TABLE, whereClause,whereArgs);
    }
    public long deleteUserByUID(String uid) {
        String whereClause = UID + "=?";
        String[] whereArgs = {uid};
        return database.delete(USER_TABLE, whereClause, whereArgs);
    }

    public long deleteRequestByRID(String rid){
        String whereClause = RID+"=?";
        String[] whereArgs = {rid};
        return database.delete(REQUEST_TABLE, whereClause,whereArgs);
    }
    public long deleteRequestsByUID(String uid){
        String whereClause = UID+"=?";
        String[] whereArgs = {uid};
        return database.delete(REQUEST_TABLE, whereClause,whereArgs);
    }

    /*--     SELECT      --*/

    public Cursor selectAllUsers() {
        String orderBy = NAME + " ASC";
        String[] cols = USER_FIELDS;
        String whereClause= SELF;//+" IS NOT 1";
        Cursor mCursor = database.query(true, USER_TABLE,cols,whereClause
                , null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectAllFriends() {
        String orderBy = NAME + " ASC";
        String[] cols = USER_FIELDS;
        String whereClause= SELF+" IS NOT 1 AND "+FRIEND+" IS 1";
        Cursor mCursor = database.query(true, USER_TABLE,cols,whereClause
                , null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectAllUSersWithRequests() {
        String orderBy = NAME + " ASC";
        String[] cols = USER_FIELDS;
        String whereClause= SELF+" IS NOT 1 AND WHERE "+UID+" IN "+"(SELECT "+UID+" FROM "+REQUEST_TABLE+")";
        Cursor mCursor = database.query(true, USER_TABLE,cols,whereClause
                , null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectAllFriendsOrRequests() {
        String orderBy = NAME + " ASC";
        String[] cols = USER_FIELDS;
        String whereClause= "("+SELF+" IS NOT 1 AND "+FRIEND+"=1) OR "+UID+" IN "+"(SELECT "+UID+" FROM "+REQUEST_TABLE+" WHERE "+SELF+"=0)";
        Cursor mCursor = database.query(true, USER_TABLE,cols,whereClause
                , null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectRequestByUser(AUser user) {
        String whereClause = UID + "=?";// OR " + FROM_UID +"=?";
        String[] whereArgs = {user.getUID()};
        String[] cols = REQUEST_FIELDS;
        Cursor mCursor = database.query(true, REQUEST_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectRequestFromUser(AUser user) {
        String whereClause = UID + "=? AND "+SELF+" =0";// OR " + FROM_UID +"=?";
        String[] whereArgs = {user.getUID()};
        String[] cols = REQUEST_FIELDS;
        Cursor mCursor = database.query(true, REQUEST_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectRequestToUser(String uid) {
        String whereClause = UID + "=? AND "+SELF+"=1";// OR " + FROM_UID +"=?";
        String[] whereArgs = {uid};
        String[] cols = REQUEST_FIELDS;
        Cursor mCursor = database.query(true, REQUEST_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectAllRequests() {
        String orderBy = REQUEST_DATE + " ASC";
        String[] cols = REQUEST_FIELDS;
        String whereClause=SELF+" IS NOT 1";
        Cursor mCursor = database.query(true, REQUEST_TABLE,cols,
                whereClause,null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectAllUsersNoPicture() {
        String orderBy = NAME + " ASC";
        String whereClause = PHOTO_ID + " IS NOT NULL AND " + PHOTO_ID_DEVICE +"='0'";
        String[] cols = USER_FIELDS;
        Cursor mCursor = database.query(true, USER_TABLE, cols, whereClause
                , null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectMessagesByUID(String uid) {
        String whereClause = UID + "=?";// OR " + FROM_UID +"=?";
        String[] whereArgs = {uid};
        String orderBy = MESSAGE_DATE + " ASC";
        String[] cols = MESSAGE_FIELDS;
        Cursor mCursor = database.query(true, MESSAGE_TABLE,cols,
                whereClause, whereArgs, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectMessagesByMID(String mid) {
        String whereClause = MID + "=?";
        String[] whereArgs = {mid};
        String[] cols = MESSAGE_FIELDS;
        Cursor mCursor = database.query(true, MESSAGE_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectRecentMessages() {
        String groupBy = "";//TO_UID;
        String orderBy = MESSAGE_DATE+" DESC";
        String[] cols = MESSAGE_FIELDS;
        Cursor mCursor = database.query(true, MESSAGE_TABLE,cols,
                null, null, groupBy, null, orderBy, "3");
        /*String qry = "SELECT * FROM " + MESSAGE_TABLE + " GROUP BY "+TO_UID+" ORDER BY "+MESSAGE_DATE+" DESC";*/
        //mCursor = database.rawQuery(qry , new String[] {});
        mCursor = database.rawQuery(
                "SELECT t.*"
                        + "FROM " + MESSAGE_TABLE +" t "
                        + "INNER JOIN ("
                        + "    SELECT "+UID+", MAX("+MESSAGE_DATE+") AS maxdate "
                        + "    FROM " + MESSAGE_TABLE
                        + "    GROUP BY "+UID
                        + ") ss ON t."+UID+" = ss."+UID+" AND t."+MESSAGE_DATE+" = ss.maxdate ORDER BY ss.maxdate DESC",
                null
        );
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectRecentMessages2() {
        String groupBy = UID;
        String orderBy = MESSAGE_DATE+" DESC";
        String[] cols = MESSAGE_FIELDS;
        Cursor mCursor = database.query(true, MESSAGE_TABLE,cols,
                null, null, groupBy, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectUserByUID(String uid) {
        String whereClause = UID + "=?";
        String[] whereArgs = {uid};
        String[] cols = USER_FIELDS;
        Cursor mCursor = database.query(true, USER_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }
    public Cursor selectFriendByUID(String uid) {
        String whereClause = UID + "=? AND "+FRIEND+" = 1";
        String[] whereArgs = {uid};
        String[] cols = USER_FIELDS;
        Cursor mCursor = database.query(true, USER_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectUserBySelf() {
        String whereClause = SELF + "=?";
        String[] whereArgs = {"1"};
        String[] cols = USER_FIELDS;
        Cursor mCursor = database.query(true, USER_TABLE,cols,
                whereClause, whereArgs, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }


}
