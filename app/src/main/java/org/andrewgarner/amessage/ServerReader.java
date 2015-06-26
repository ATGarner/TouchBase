package org.andrewgarner.amessage;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads XML response from server and then handles it and adds to database
 * Created by Andrew on 4/18/2015.
 */
public class ServerReader {

    private static final String TAG = "andrewgarner";
    private String MyUUID="";
    private Context mContext;

    private List<AUser> aUserList = new ArrayList<>();
    private List<ARequest> aRequestList = new ArrayList<>();
    private List<AMessage> aMessageList = new ArrayList<>();

    private AUserHelper AUH;

    public ServerReader(Context c){
        mContext = c;

        AUH = new AUserHelper(mContext);
        try {
            MyUUID = AUH.getSelfID();
        }catch(Exception e){
            Log.e(TAG, "ServerReader / Self ID error");
        }
    }

    /**
     * Read the xml and put it into the databse
     * @param xml XML data from server
     * @return server response with lists
     */
    public ServerResponse readMessageAddToDB(String xml){
        try {
            //Log.d(TAG, xml);
            //parse the xml data
            parse(null, xml);
        }catch(Exception e){
            Log.e(TAG,"ServerReader  / parse() error\n"+e);
        }

        //Return the lists
        return addToDB();

    }

    /**
     * Read search result and return the list it generates
     * @param xml XML data from server
     * @return user list
     */
    public List<AUser> readSearchResult(String xml){
        try {
            parse(null, xml);
        }catch(Exception e){
            Log.e(TAG,"ServerReader / parse() error\n"+e);
        }
        return aUserList;
    }

    /**
     * Add lists to database and then return serverresponse object
     * @return ServerResponse containing lists of users, messages, and/or requests
     */
    private ServerResponse addToDB(){
        Log.i(TAG,"SR / Populating DB...");

        AMessageHelper MH = new AMessageHelper(mContext);

        //The following three lists are the ones returned in the ServerResponse
        List<AUser> userSendList = new ArrayList<>();
        List<AMessage> messageSendList = new ArrayList<>();
        List<ARequest> requestSendList = new ArrayList<>();

        try{
            Log.e(TAG, "SR / " + aUserList.size() + " " + aMessageList.size());
        }catch(Exception e){
            e.printStackTrace();
        }

        //Go through each user, check if self, check if is friend or new user
        for (AUser myUser : aUserList) {
            //myUser.showData();
            //Log.v(TAG, "SR / myUser / "+myUser.getEmail()+" isFriend:"+myUser.isFriend());
            try {
                if (!AUH.isUser(myUser)) {
                    //if it is a new user coming in
                    if (myUser.isSelf()) {
                        if (MyUUID.equals("")) {
                            //set self into database
                            Log.v(TAG, "SR / myUser / " + myUser.getEmail() + " isSelf:" + myUser.isSelf());
                            AUH.createUser(myUser);
                            //set self preferences
                            AUH.setMyUUIDPref(myUser.getUID());
                            MyUUID = myUser.getUID();
                            Log.wtf(TAG, "SR / myUser / MY UUID = " + MyUUID);
                            myUser.showData();
                        }
                    } else {
                        //simply create the user
                        AUH.createUser(myUser);
                    }
                } else {
                    if(!myUser.isSelf()) {
                        if (myUser.isFriend()) {
                            //change user to status to friend
                            if (AUH.setUserFriendStatus(myUser, myUser.isFriend())) {
                                //if change is successful, add to user list to return
                                userSendList.add(myUser);
                            }
                        } else {
                            AUH.createUser(myUser);
                            userSendList.add(myUser);
                        }
                    }
                }
            }catch(Exception e){
                Log.e(TAG, "ServerReader / AddToDB / For loop User error "+e);
            }
        }

        //check received messages
        for (AMessage myMsg : aMessageList) {
            try {
                //check to make sure the message went to the right person
                if (myMsg.checkUid(MyUUID)) {
                    //if insertion is successful, add to send list
                    if (MH.createMessage(myMsg)) {
                        messageSendList.add(myMsg);
                    }
                } else {
                    Log.e(TAG, "ServerReader / CheckUID failed");
                }
            }catch(Exception e){
                Log.e(TAG,"ServerReader / AddToDB / For loop Message error "+e);
            }
        }

        //check received request
        for (ARequest request : aRequestList){
            try {
                //Log.i("TAG", "ServerReader / Request from "+request.getUser().getName());
                //request.getUser().showData();
                if(!request.getUser().isSelf()) {
                    AUH.createUser(request.getUser());
                    //if creation of request is successful, add to list
                    if (AUH.createRequest(request)) {
                        requestSendList.add(request);
                    }
                }
            }catch(Exception e){
                Log.e(TAG, "ServerReader / AddToDB / For loop Request error "+e);
            }
        }

        //create response object and add lists to it
        ServerResponse SR = new ServerResponse();
        SR.setMessageList(messageSendList);
        SR.setUserList(userSendList);
        SR.setRequestList(requestSendList);

        return SR;
    }

    public String getMyUUID(){
        return MyUUID;
    }

    //set namespace to null
    private static final String ns = null;

    /**
     * Parse the XML data
     * @param in
     * @param input XML input
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void parse(InputStream in, String input) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(input));
            parser.nextTag();
            readFeed(parser);
        } finally {
            in.close();
        }
    }

    /**
     * Reads the XML data and determines which list to add it to
     * @param parser
     */
    private void readFeed(XmlPullParser parser){

        try{
            parser.require(XmlPullParser.START_TAG, ns, "xml");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                //Determine which list to add to based off of root name
                switch(name){
                    case "user":
                        aUserList.add(readUser(parser)); break;
                    case "message":
                        aMessageList.add(readMessage(parser));break;
                    case "request":
                        aRequestList.add(readRequest(parser));break;

                    default:
                        skip(parser);
                }
            }
        } catch(Exception e){
            Log.e(TAG,"readFeed error");
            e.printStackTrace();
        }
        //return userList;
    }

    /** Parses the contents of a user entry and add to user list
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private AUser readUser(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "user");

        String uid = "";
        String uname = "";
        String email = "";
        String photo = "";
        String self = "";
        String friend ="";
        String notify="";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            switch(name){
                case "uid":
                    uid = readNormal(parser, name); break;
                case "name":
                    uname = readNormal(parser, name); break;
                case "email":
                    email = readNormal(parser, name); break;
                case "photo":
                    photo = readNormal(parser, name); break;
                case "self":
                    self = readNormal(parser, name); break;
                case "notify":
                    notify = readNormal(parser, name); break;
                case "friend":
                    friend = readNormal(parser, name); break;
                default:
                    skip(parser);
            }

        }
        Log.d(TAG, uid+" "+uname+" "+email+" "+photo+" "+self+" "+friend+" "+notify);
        if(uid.equals(MyUUID))
            return null;
        //make sure we have enough required information to create a user
        if(!uid.equals("") && !uname.equals("") && !email.equals("")) {
            AUser user =  new AUser(uid, uname, email, photo, self,friend);
            user.setNotify(notify);
            return user;
        }
        return null;
    }

    /**
     * Read contents of a request and add to request list
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private ARequest readRequest(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "request");

        String uid = "";
        String uname = "";
        String email = "";
        String photo = "";
        String self = "";
        String rid = "";
        String message = "";
        String friend = "";
        String requestSelf = "";
        String dateSent ="";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            switch(name){
                case "uid":
                    uid = readNormal(parser, name); break;
                case "name":
                    uname = readNormal(parser, name); break;
                case "email":
                    email = readNormal(parser, name); break;
                case "photo":
                    photo = readNormal(parser, name); break;
                case "self":
                    self = readNormal(parser, name); break;
                case "friend":
                    friend = readNormal(parser, name); break;
                case "rid":
                    rid = readNormal(parser, name); break;
                case "message":
                    message = readNormal(parser, name); break;
                case "requestself":
                    requestSelf = readNormal(parser, name); break;
                case "datesent":
                    dateSent = readNormal(parser, name);break;
                default:
                    skip(parser);

            }

        }
        //Make sure we aren't sending a request to ourselves
        if(uid.equals(MyUUID))
            return null;
        //Make sure we have the necessary information to create a user and request
        if(!uid.equals("") && !uname.equals("") && !email.equals("")){
            AUser user = new AUser(uid, uname, email, photo, self,friend);
            return new ARequest(rid, user, requestSelf,message, dateSent);
        }
        return null;
    }


    /**
     * Read contents of a message entry and add to message list
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private AMessage readMessage(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "message");

        String mid = "";
        String to_uid="";
        String self = "";
        String text = "";
        String photo = "";
        String date = "";
        String uidCheck = "";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

           String name = parser.getName();
           switch(name){
               case "mid":
                   mid = readNormal(parser, name); break;
               case "to_uid":
                   to_uid = readNormal(parser, name); break;
               case "self":
                   self = readNormal(parser, name); break;
               case "text":
                   text = readNormal(parser, name); break;
               case "photo":
                   photo = readNormal(parser, name); break;
               case "date":
                   date = readNormal(parser, name); break;
               case "uidCheck":
                   uidCheck = readNormal(parser, name); break;
               default:
                   skip(parser);
           }
        }

        //Make sure there is enough required information to create a message
        if(!mid.equals("") && !to_uid.equals(""))
            return new AMessage(mid, to_uid, self, text, photo, date, uidCheck);
        return null;
    }

    /**
     * Use this to read any XML element of the form:
     * <tag> something </tag>
     * @param parser
     * @param tag
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readNormal(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return title;
    }


    /**
     * Read the contents of a tag
     * @param parser
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Skip over a tag if not needed
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
