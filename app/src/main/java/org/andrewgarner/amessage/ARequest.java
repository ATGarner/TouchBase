package org.andrewgarner.amessage;

/**
 * Object for friend requests. The app stores requests that are to and from the user that
 * is logged in.
 * Created by Andrew on 5/25/2015.
 */
public class ARequest {
    //Request ID
    private String RID;
    //User ID of the other user
    private String UID;
    //if it is incoming or outgoing
    private boolean self;
    //message that accompanies the request
    private String request_message;
    //User object for the other user
    private AUser user;
    //Epoch time of when the request was sent
    private int dateSent;


    public ARequest(String RID, AUser user, String selfString, String request_message, String dateSent){
        this.RID = RID;
        this.UID = user.getUID();
        this.self = selfString.equals("1");
        this.request_message=request_message;
        this.user = user;
        this.dateSent = Integer.getInteger(dateSent,0);
    }

    public String getRID() {
        return RID;
    }

    public String getUID() {
        return UID;
    }

    public boolean isSelf() {
        return self;
    }

    public String getRequest_message() {
        return request_message;
    }

    public AUser getUser() {
        return user;
    }

    public int getDate(){
        return dateSent;
    }

}
