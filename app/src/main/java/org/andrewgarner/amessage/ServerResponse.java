package org.andrewgarner.amessage;

import java.util.List;

/**
 * Created by Andrew on 5/31/2015.
 */
public class ServerResponse {

    private List<AMessage> messageList;
    private List<AUser> userList;
    private List<ARequest> requestList;

    private boolean messageIsSet=false;
    private boolean userIsSet=false;
    private boolean requestIsSet=false;

    private boolean notify=false;
    public ServerResponse(){

    }
    public void setMessageList(List<AMessage> messageList){
        this.messageList = messageList;
        if(!messageList.isEmpty())
            messageIsSet=true;

    }
    public void setUserList(List<AUser> userList){
        this.userList = userList;
        if(!userList.isEmpty())
            userIsSet=true;

    }
    public void setRequestList(List<ARequest> requestList){
        this.requestList = requestList;
        if(!requestList.isEmpty())
            requestIsSet=true;

    }

    public AMessage getFirstMessage(){
        if (messageList!=null && !messageList.isEmpty() && messageIsSet){
            return messageList.get(0);
        }
        return null;
    }
    public AUser getFirstUser(){
        if (userList!=null && !userList.isEmpty() && userIsSet){
            return userList.get(0);
        }
        return null;
    }
    public ARequest getFirstRequest(){
        if (requestList!=null && !requestList.isEmpty() && requestIsSet){
            return requestList.get(0);
        }
        return null;
    }

    public boolean hasMessage(){
        return messageIsSet;
    }
    public boolean hasUser(){
        return userIsSet;
    }
    public boolean hasRequest(){
        return requestIsSet;
    }

    public List<AMessage> getMessageList(){
        return messageList;
    }
    public List<AUser> getUserList(){
        return userList;
    }
    public List<ARequest> getRequestList(){
        return requestList;
    }

}
