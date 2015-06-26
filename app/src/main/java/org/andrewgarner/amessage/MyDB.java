package org.andrewgarner.amessage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Andrew on 3/15/2015.
 */
public class MyDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "amessagedb2";
    private static final int DATABASE_VERSION = 1;

    public static final String USER_TABLE = "user";
    public static final String MESSAGE_TABLE = "message";
    public static final String REQUEST_TABLE = "request";


    public static final String MID = "mid";
    public static final String UID = "uid";

    public static final String MESSAGE_TEXT = "message_text";
    public static final String MESSAGE_PHOTO = "message_number";
    public static final String MESSAGE_DATE = "message_date";

    public static final String NAME = "name";
    public static final String PHOTO_ID = "photo_id";
    public static final String PHOTO_ID_DEVICE = "photo_id_device";
    public static final String EMAIL = "email";
    public static final String SELF = "self";
    public static final String LAST_SEEN = "last_seen";
    public static final String FRIEND = "friend";


    public static final String RID = "rid";
    public static final String REQUEST_MESSAGE = "request_message";
    public static final String REQUEST_DATE = "request_date";


    public static final String[] USER_FIELDS = {UID,NAME,EMAIL,PHOTO_ID,PHOTO_ID_DEVICE,SELF,LAST_SEEN,FRIEND};
    public static final String[] MESSAGE_FIELDS = {MID,UID,SELF,MESSAGE_TEXT,MESSAGE_PHOTO,MESSAGE_DATE};
    public static final String[] REQUEST_FIELDS = {RID,UID,REQUEST_MESSAGE,SELF, REQUEST_DATE};


    private static final String CREATE_USER_TABLE = "CREATE TABLE "
            + USER_TABLE + "("
            + UID + " TEXT PRIMARY KEY NOT NULL, "
            + NAME + " TEXT NOT NULL, "
            + EMAIL + " TEXT UNIQUE NOT NULL,"
            + PHOTO_ID + " TEXT,"
            + PHOTO_ID_DEVICE + " INTEGER NOT NULL CHECK ("+ SELF +" IN (0,1)),"
            + SELF + " INTEGER NOT NULL CHECK ("+ SELF +" IN (0,1)),"
            + FRIEND+ " INTEGER NOT NULL CHECK ("+ FRIEND +" IN (0,1)),"
            + LAST_SEEN + " INTEGER"
            + ")";

    private static final String CREATE_MESSAGE_TABLE = "CREATE TABLE "
            + MESSAGE_TABLE + "("
            + MID + " TEXT PRIMARY KEY NOT NULL, "
            + UID + " TEXT NOT NULL,"
            + SELF + " INTEGER NOT NULL CHECK ("+ SELF +" IN (0,1)),"
            + MESSAGE_TEXT + " TEXT, "
            + MESSAGE_PHOTO + " TEXT, "
            + MESSAGE_DATE + " INTEGER NOT NULL, "
            + "FOREIGN KEY(" + UID + ") REFERENCES "+ USER_TABLE + "(" + UID + ") ON DELETE CASCADE"
            + ")";

    private static final String CREATE_REQUEST_TABLE = "CREATE TABLE "
            + REQUEST_TABLE + "("
            + RID + " TEXT PRIMARY KEY NOT NULL, "
            + UID + " TEXT NOT NULL,"
            + REQUEST_MESSAGE + " TEXT, "
            + SELF + " INTEGER NOT NULL CHECK ("+ SELF +" IN (0,1)),"
            + REQUEST_DATE + " INTEGER NOT NULL, "
            + "FOREIGN KEY(" + UID + ") REFERENCES "+ USER_TABLE + "(" + UID + ") ON DELETE CASCADE"
            + ")";


    private static MyDB instance;

    public static synchronized MyDB getHelper(Context context) {
        if (instance == null)
            instance = new MyDB(context);
        return instance;
    }

    private MyDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_MESSAGE_TABLE);
        db.execSQL(CREATE_REQUEST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MyDB.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");

        /*db.execSQL("DROP TABLE IF EXISTS " + MESSAGE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+REQUEST_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+USER_TABLE);*/

        db.delete(MESSAGE_TABLE, null, null);
        db.delete(REQUEST_TABLE, null, null);
        db.delete(USER_TABLE, null, null);

        //onCreate(db);

    }

}