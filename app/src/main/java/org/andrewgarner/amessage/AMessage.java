package org.andrewgarner.amessage;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


/***
 * Object for messages between users.
 */
public class AMessage implements Parcelable {
    //ID for message itself
    private String MID;
    //ID of the user we are communicating with
    private String UID;
    //Text contents of the message
    private String Text;
    //Path to picture, not fully implemented yet
    private String Photo;
    //if the message originated from the logged-in user
    private boolean Self;
    //if the message has been seen by the other user, to be implemented
    private boolean Seen;
    //if the message has been recieved by the server, to be implemented
    private boolean Sent;
    //date (in epoch time) that the message was sent
    private long date;
    //A check to ensure the incoming message was sent to the right person
    private String uidCheck;
    //Tag for Logging
    private String TAG = "andrewgarner";

    public AMessage(String mid, String touid, boolean self){
        MID = mid;
        UID = touid;
        this.Self =self;
    }
    public AMessage(String mid, String touid, String self){
        MID = mid;
        UID = touid;
        this.Self =self.equals("1");
    }

    public AMessage(String mid, String touid, boolean self, String text, String photo, String date, String uidCheck){
        MID = mid;
        UID = touid;
        Self =self;
        Text = text;
        Photo = photo;

        this.date = Long.parseLong(date);
        this.uidCheck = uidCheck;
    }
    public AMessage(String mid, String touid, String self, String text, String photo, String date, String uidCheck){
        MID = mid;
        UID = touid;
        Self =self.equals("1");

        Text = text;
        Photo = photo;

        this.date = Long.parseLong(date);
        this.uidCheck = uidCheck;
    }

    public void showData(){
        Log.i(TAG, "------Message--------");
        Log.v(TAG, "mid: "+MID);
        Log.v(TAG, "uid: "+UID);
        Log.v(TAG, "text: "+ Text);
        Log.v(TAG, "photo: "+Photo);
        Log.v(TAG, "self: "+Self);
        Log.v(TAG, "date: "+date);
        Log.v(TAG, "uidCheck: "+uidCheck);
        Log.i(TAG, "---------------------");
    }

    //Used to create a parcelable object
    public AMessage(Parcel in){
        String[] data = new String[7];

        in.readStringArray(data);
        this.MID = data[0];
        this.UID = data[1];
        this.Self = data[2].equals("1");
        this.Text = data[3];
        this.Photo = data[4];
        this.date = Long.parseLong(data[5]);
        this.uidCheck = data[6];

    }
    //Used to create a parcelable object
    @Override
    public int describeContents(){
        return 0;
    }
    //Used to create a parcelable object
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String selfString;
        if(Self){
            selfString="1";
        }else{
            selfString="0";
        }
        dest.writeStringArray(new String[] {
                this.MID,
                this.UID,
                selfString,
                this.Text,
                this.Photo,
                this.date+"",
                this.uidCheck
                });
    }
    //Used to create a parcelable object
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AMessage createFromParcel(Parcel in) {
            return new AMessage(in);
        }

        public AMessage[] newArray(int size) {
            return new AMessage[size];
        }
    };



    public String getMID() {
        return MID;
    }

    public String getUID() {
        return UID;
    }


    public String getText() {
        return Text;
    }

    public void setText(String message_Text) {
        Text = message_Text;
    }

    public String getPhoto() {
        return Photo;
    }

    public void setPhoto(String message_Photo) {
        Photo = message_Photo;
    }

    public boolean isSelf() {
        return Self;
    }

    public void setSelf(boolean self) {
        this.Self = self;
    }

    public boolean isSeen() {
        return Seen;
    }

    public void setSeen(boolean seen) {
        this.Seen = seen;
    }

    public boolean isSent() {
        return Sent;
    }

    public void setSent(boolean sent) {
        this.Sent = sent;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setDate(String date) {
        this.date = Long.parseLong(date);
    }

    public String getUidCheck(){
        return uidCheck;
    }

    public boolean checkUid(String uid){
        return uidCheck.equals(uid);
    }

}
