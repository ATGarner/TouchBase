package org.andrewgarner.amessage;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Object that holds data for a particular user
 * Created by andrewgarner on 3/27/15.
 */
public class AUser implements Parcelable{
    //User ID
    private String UID;
    private String Name;
    private String Email;
    //Path to profile picture
    private String Photo_ID;
    //if the photo is currently stored in memory
    private boolean Photo_Is_On_Device;
    //if it is the logged-in user
    private boolean self;
    //if the user is a friend
    private boolean friend;
    //date when the other user last viewed the conversation (to be implemented)
    private long lastSeen;
    //if this is a new user, choose whether or not to send a notification
    private boolean notify;

    private String TAG = "andrewgarner";


    public AUser (String uid, String name, String email, String photo_id, boolean self, boolean friend){
        UID=uid;
        Name=name;
        Email=email;
        Photo_ID=photo_id;
        this.self=self;
        this.friend=friend;
    }

    public AUser (String uid, String name, String email, String photo_id, String self, String friend){
        UID=uid;
        Name=name;
        Email=email;
        Photo_ID=photo_id;

        try{
            this.self = stringToBool(self);
        }catch(Exception e){
            this.self = false;
        }

        try{
            this.friend = stringToBool(friend);
        }catch(Exception e){
            this.friend = false;
        }
    }

    public void showData(){
            Log.i(TAG, "-------User---------");
            Log.v(TAG, "UID: " + UID);
            Log.v(TAG, "Name: "+Name);
            Log.v(TAG, "email: "+Email);
            Log.v(TAG, "self: "+self);
            Log.v(TAG, "photo_id: "+Photo_ID);
            Log.v(TAG, "photo_on_dev: "+Photo_Is_On_Device);
            Log.v(TAG, "lastseen: "+lastSeen);
            Log.v(TAG, "friend: "+friend);
            Log.i(TAG, "--------------------");
    }

    //Used to create a parcelable object
    public AUser(Parcel in){
        String[] data = new String[8];

        in.readStringArray(data);
        this.UID = data[0];
        this.Name = data[1];
        this.Email = data[2];
        this.Photo_ID = data[3];
        this.Photo_Is_On_Device = stringToBool(data[4]);
        this.self = stringToBool(data[5]);
        this.friend = stringToBool(data[6]);
        this.lastSeen = Long.parseLong(data[7]);

    }
    //Used to create a parcelable object
    @Override
    public int describeContents(){
        return 0;
    }

    //Used to create a parcelable object
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{
                this.UID,
                this.Name,
                this.Email,
                this.Photo_ID,
                booleanToString(this.Photo_Is_On_Device),
                booleanToString(this.self),
                booleanToString(this.friend),
                longToString(this.lastSeen)
        });
    }

    //Used to create a parcelable object
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AUser createFromParcel(Parcel in) {
            return new AUser(in);
        }

        public AUser[] newArray(int size) {
            return new AUser[size];
        }
    };
    private String booleanToString(boolean bool){
        if(bool)
            return "1";
        return "0";
    }
    private boolean stringToBool(String string){
        return string.equals("1");
    }
    private String longToString(long value){
        return String.valueOf(value);

    }

    //check if we need to download the user's photo
    public boolean photoNeedsDownload(){
        return !Photo_ID.equals("") && Photo_ID!=null && !Photo_Is_On_Device;
    }

    public String getUID() {
        return UID;
    }

    public String getName() {
        return Name;
    }

    public String getEmail() {
        return Email;
    }

    public String getPhoto_ID() {
        return Photo_ID;
    }

    public void setPhoto_ID(String photo_ID) {
        Photo_ID = photo_ID;
    }

    public void setPhoto_Is_On_Device(boolean isSet) {
        Photo_Is_On_Device = isSet;
    }

    public boolean getPhoto_Is_On_Device() {
        return Photo_Is_On_Device;
    }

    public boolean isSelf() {
        return self;
    }
    public boolean isFriend() {
        return friend;
    }

    public void setFriend(boolean isFriend){
        friend = isFriend;
    }
    public void setFriend(String isFriend){
        try{
            this.friend = isFriend.equals("1");
        }catch(Exception e){
            this.friend = false;
        }
    }

    public void setLastSeen(long time){
        lastSeen=time;
    }
    public long getLastSeen(){
        return lastSeen;
    }

    public void setNotify(boolean notify){
        this.notify = notify;
    }
    public void setNotify(String notify){
        try{
            this.notify = notify.equals("1");
        }catch(Exception e){
            this.notify = false;
        }
    }
    public boolean shouldNotify(){
        return notify;
    }

/*    public String getRID(){
        return RID;
    }
    public String getRequestMessage(){
        return requestMessage;
    }*/
}
