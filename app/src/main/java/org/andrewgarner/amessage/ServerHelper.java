package org.andrewgarner.amessage;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class to send data to the server or downloading pictures.
 * Includes functions for:
 * Downloading photos, Sending messages, sending photos, searching, friend requests, unfriending
 * Created by Andrew on 4/30/2015.
 */
public class ServerHelper {
    private Context mContext;
    private String TAG = "andrewgarner";
    private ServerHelperCallback mCallback;

    //TODO: Change these in String.xml to urls from your own site
    private String SendMessageURL;// = "https://www.yoururl.com/sendmessage.php";
    private String SearchURL;// = "https://www.yoururl.com/search.php";
    private String RequestURL;// = "https://www.yoururl.com/sendrequest.php";
    private String RequestResponseURL;// = "https://www.yoururl.com/respondrequest.php";
    private String UnfriendURL;// = "https://www.yoururl.com/unfriend.php";
    private String PhotoURL;// = "https://www.yoururl.com/sendphoto.php";


    private String mSecretString; //TODO: Change this in String.xml to your own secret
    //The below items are POST variable names to use when interacting with the server
    //If they are changed here, they must be changed on the server files or else
    //it will not run properly.
    private String mSecret="secret";
    private String mMID ="mid";
    private String mUID="uid";
    private String mRID="rid";
    private String mTo_UID = "to_uid";
    private String mFrom_UID="from_uid";
    private String mFriend_UID="friend_uid";
    private String mText = "text";
    private String mGCM = "gcm";
    private String mPhoto="photo";
    private String mSearch="search";
    private String mMessage="message";
    private String mResponse="response";
    private String mAccept="ACCEPT";
    private String mReject="REJECT";


    public ServerHelper(Context c){
        mContext = c;
        mCallback = (ServerHelperCallback) mContext;

        setStrings();
    }

    private void setStrings(){
        mSecretString = mContext.getString(R.string.secret);

        SendMessageURL = mContext.getString(R.string.url_sendmessage);
        SearchURL = mContext.getString(R.string.url_search);
        RequestURL = mContext.getString(R.string.url_sendrequest);
        RequestResponseURL= mContext.getString(R.string.url_respondrequest);
        UnfriendURL = mContext.getString(R.string.url_unfriend);
        PhotoURL = mContext.getString(R.string.url_sendphoto);
    }

    public interface ServerHelperCallback{
        void onDownloadPictureFinished();
        void onSearchFinished(List<AUser> list);
    }

    /**
     * Reads XML tags and their contents
     * @param entityResponse
     * @return
     * @throws IOException
     */
    public String readFully(InputStream entityResponse) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = entityResponse.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString();
    }

    /*****************************   DOWNLOAD PICTURE  *********************************************/
    public void downloadUserPicture(AUser user, boolean save){

        new DownloadPictureTask(save).execute(user);

    }
    public void downloadUserPicture(AUser user, ImageView iv, ProgressBar bar, boolean save){

        new DownloadPictureTask(save,iv, bar).execute(user);

    }

    private class DownloadPictureTask extends AsyncTask<AUser, Void, Bitmap> {
        AUser user;
        //ImageView to set the downloaded picture to
        ImageView iv;
        //Progress bar spinner to show loading
        ProgressBar bar;
        boolean save;

        public DownloadPictureTask(boolean save, ImageView iv, ProgressBar bar){
            this.iv = iv;
            this.bar = bar;
            this.save=save;

        }
        public DownloadPictureTask(boolean save){
            iv=null;
            bar=null;
            this.save=save;
        }
        @Override
        protected Bitmap doInBackground(AUser... users) {
            try {
                user = users[0];
                String photo_url = user.getPhoto_ID();

                //find what 100dp is in pixels for this device
                Resources r = mContext.getResources();
                int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
                if(px<=0 || px >=500){
                    px = 200;
                }

                //Google pictures defauly to size 50, change that to 100dp
                photo_url = photo_url.replace("sz=50", "sz="+px);

                if(iv==null){
                    Log.e(TAG, "ServerHelper / downloadPicture / null! ");
                    return null;
                }
                /*if(iv==null || !user.getUID().equals(iv.getTag())) {
                    Log.e(TAG, "ServerHelper / downloadPicture / null! ");
                    return null;
                }*/

                Bitmap bitmapResult =  getBitmapFromURL(photo_url);
                if(save) { //if we want to save the bitmap to device storage
                    FileOutputStream fos;
                    try {
                        //Put the bitmap in private storage with the name of the user ID
                        fos = mContext.openFileOutput(user.getUID(), Context.MODE_PRIVATE);
                        bitmapResult.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "file not found");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.d(TAG, "io exception");
                        e.printStackTrace();
                    }
                    //Log.e(TAG, "ServerHelper / new bitmapfile for: " + user.getName());
                    AUserHelper AU = new AUserHelper(mContext.getApplicationContext());
                    AU.setUserPicture(user.getUID(), true);

                    mCallback.onDownloadPictureFinished();
                }
                return bitmapResult;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(Bitmap bitmapResult) {
            if(bitmapResult!=null) {

                BitmapHelper BH = new BitmapHelper(mContext);
                if(iv!=null){ //Make sure Imageview isn't null
                    //Check the tag on the imageview, if we the user has scrolled it may
                    //want another picture instead, so in that case don't set it
                    if(user.getUID().equals(iv.getTag())) {
                        //make the bitmap a circle and set it to the imageview
                        iv.setImageBitmap(BH.circleBM(bitmapResult));
                        iv.setVisibility(View.VISIBLE);
                    }
                }

            }else{
                Log.e(TAG, "ServerHelper / onPostExecute / bitmap file error for: " + user.getName());
            }
            if(bar!=null)
                bar.setVisibility(View.GONE);

        }

        /**
         * Opens up a connection with a URL, get the input, convert to bitmap
         * @param src URL of the picture
         * @return Bitmap of the user profile picture
         * @throws IOException
         */
        private Bitmap getBitmapFromURL(String src) throws IOException{

            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        }

        //End of Async
    }

    /********************************   SEND MESSAGE   ******************************************/
    /**
     * Sends a message to the server
     * @param msg Object that holds all the necessary information to send
     */
    public void sendMessage(AMessage msg){

        new SendMessageTask().execute(msg);
    }

    private class SendMessageTask extends AsyncTask<AMessage, Void, String> {

        public SendMessageTask(){

        }
        @Override
        protected String doInBackground(AMessage... msgs) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendMessageToServer(msgs[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Response --");
            Log.v(TAG, result);
            Log.v(TAG, "---------------");
            //ServerLoginReader slr = new ServerLoginReader();

        }
    }

    private String sendMessageToServer(AMessage msg) throws IOException {
        // Create a new HttpClient and Post Header
        //Log.e(TAG, "ServerHelper / SendMessage / sendMessageToServer()");

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(SendMessageURL);

        try {

            AUserHelper UH = new AUserHelper(mContext);
            String myUUID = UH.getSelfID();
            String myGCM = UH.getSelfGCMID();

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(mSecret, mSecretString));
            nameValuePairs.add(new BasicNameValuePair(mMID, msg.getMID()));
            nameValuePairs.add(new BasicNameValuePair(mTo_UID, msg.getUID()));
            nameValuePairs.add(new BasicNameValuePair(mFrom_UID, myUUID));
            nameValuePairs.add(new BasicNameValuePair(mText, msg.getText()));
            nameValuePairs.add(new BasicNameValuePair(mGCM, myGCM));
            //TODO: message photo
            nameValuePairs.add(new BasicNameValuePair(mPhoto, ""));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }
        return "";
    }



    /********************************** SEND PHOTO *******************************************/

    //TODO: This isn't fully implemented yet

    /**
     * Send a photo to the server,
     * needs to be implemented as a message, just in test phase right now
     * @param uid send the profile picture of a user by their uid
     */
    public void sendPhoto(String uid){
        BitmapHelper bh = new BitmapHelper(mContext);
        Bitmap b = bh.getUserBitmap(uid);
        File newFile = persistImage(b,uid);
        new sendPhotoTask(uid, b).execute(newFile);


    }

    private class sendPhotoTask extends AsyncTask<File, Void, String> {
        private String uid;
        Bitmap b;
        public sendPhotoTask(String uid, Bitmap b) {
            this.uid = uid;
            this.b = b;
        }

        @Override
        protected String doInBackground(File... params) {
            HttpURLConnection connection;
            DataOutputStream outputStream;

            String dir = mContext.getFilesDir().getAbsolutePath();


            Log.e(TAG, "FilePath="+dir);

            String pathToOurFile = dir+"/"+uid;
            String urlServer = PhotoURL;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024*1024;

            try
            {
                FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );

                URL url = new URL(urlServer);
                connection = (HttpURLConnection) url.openConnection();

                // Allow Inputs &amp; Outputs.
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                // Set HTTP method to POST.
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

                outputStream = new DataOutputStream( connection.getOutputStream() );
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.v(TAG, "Response code: "+serverResponseCode);
                Log.v(TAG, "Response msg: \n"+serverResponseMessage);

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
            }
            catch (Exception ex)
            {
                Log.e(TAG, ex+"");
                //Exception handling
                ex.printStackTrace();
            }
            return null;
        }

    }

    /**
     * Save the image as a file object
     * @param bitmap to make into file
     * @param name name of the file
     * @return
     */
    private File persistImage(Bitmap bitmap, String name) {
        File filesDir = mContext.getFilesDir();
        File imageFile = new File(filesDir, name + ".jpg");

        OutputStream os;
        try {
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
            return imageFile;
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
        }
        return null;
    }

    /***************************   SEARCH   *********************************************/
    /**
     * Searches the server for a name or email
     * @param searchString String to search on
     */
    public void search(String searchString){

        new SearchTask(searchString).execute();
    }

    private class SearchTask extends AsyncTask<Void, Void, String> {
        String searchString;

        public SearchTask(String searchString){
            this.searchString = searchString;
        }
        @Override
        protected String doInBackground(Void... Params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendSearchToServer(searchString);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Search Response ---");
            Log.v(TAG, result);
            Log.v(TAG, "------------------------");

            ServerReader SR = new ServerReader(mContext);
            List<AUser> userlist = SR.readSearchResult(result);

            mCallback.onSearchFinished(userlist);

        }
    }

    private String sendSearchToServer(String searchString) throws IOException {
        // Create a new HttpClient and Post Header

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(SearchURL);

        try {

            //Get self ID so that we don't get ourselves in a search
            AUserHelper UH = new AUserHelper(mContext);
            String myUUID = UH.getSelfID();

            // Add data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(mSecret, mSecretString));
            nameValuePairs.add(new BasicNameValuePair(mSearch,searchString));
            nameValuePairs.add(new BasicNameValuePair(mUID, myUUID));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }
        return "";
    }

    /***************************   SEND REQUEST   *********************************************/
    /**
     * Send a friend request to a user
     * We've already checked if the user is a friend or has a request
     * But if that somehow fails, the user will still not see it.
     * @param uid UID of the person we are sending it to
     * @param requestString Message that accompanies the request
     */
    public void request(String uid, String requestString){

        new RequestTask(uid, requestString).execute();
    }

    private class RequestTask extends AsyncTask<Void, Void, String> {
        String searchString;
        String uid;

        public RequestTask(String uid, String searchString){
            this.uid=uid;
            this.searchString = searchString;
        }
        @Override
        protected String doInBackground(Void... Params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendRequestToServer(uid, searchString);
            } catch (IOException e) {
                return "Unable to retrieve request page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Request Response ---");
            Log.v(TAG, result);
            Log.v(TAG, "------------------------");

            //ServerReader SR = new ServerReader(mContext);
            //List<AUser> userlist = SR.readSearchResult(result);

            //mCallback.onSearchFinished(userlist);

        }
    }

    private String sendRequestToServer(String uid, String messageString) throws IOException {
        // Create a new HttpClient and Post Header

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(RequestURL);

        try {

            AUserHelper UH = new AUserHelper(mContext);
            String myUUID = UH.getSelfID();
            String RID = UUID.randomUUID().toString();

            Log.i(TAG, "ServerHelper / sendRequest / "+RID+" "+uid+" "+myUUID+" "+messageString);

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(mSecret, mSecretString));
            nameValuePairs.add(new BasicNameValuePair(mRID, RID));
            nameValuePairs.add(new BasicNameValuePair(mUID, myUUID));
            nameValuePairs.add(new BasicNameValuePair(mFriend_UID, uid));
            nameValuePairs.add(new BasicNameValuePair(mMessage,messageString));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }
        return "";
    }
    /***************************   REQUEST RESPONSE   *********************************************/
    /**
     * Respond to a friend request
     * @param request object of the request responding to
     * @param response boolean yes or no to accept it
     */
    public void requestResponse(ARequest request, boolean response){

        new RequestResponseTask(request,response).execute();
    }

    private class RequestResponseTask extends AsyncTask<Void, Void, String> {
        ARequest request;
        boolean response;

        public RequestResponseTask(ARequest request,boolean response){
            this.request = request;
            this.response = response;
        }
        @Override
        protected String doInBackground(Void... Params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendRequestResponseToServer(request, response);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Request Response Response ---");
            Log.v(TAG, result);
            Log.v(TAG, "------------------------");

            //ServerReader SR = new ServerReader(mContext);
            //List<AUser> userlist = SR.readSearchResult(result);

            //mCallback.onSearchFinished(userlist);

        }
    }

    private String sendRequestResponseToServer(ARequest request, boolean requestResponse) throws IOException {
        // Create a new HttpClient and Post Header

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(RequestResponseURL);

        try {

            AUserHelper UH = new AUserHelper(mContext);
            String myUUID = UH.getSelfID();

            Log.i(TAG, "ServerHelper / sendRequestResponse / to "+request.getUser().getName()+ " response: "+requestResponse);

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(mSecret, mSecretString));
            nameValuePairs.add(new BasicNameValuePair(mRID, request.getRID()));
            nameValuePairs.add(new BasicNameValuePair(mUID, myUUID));
            nameValuePairs.add(new BasicNameValuePair(mFriend_UID, request.getUser().getUID()));

            if(requestResponse){
                nameValuePairs.add(new BasicNameValuePair(mResponse, mAccept));
            }else{
                nameValuePairs.add(new BasicNameValuePair(mResponse, mReject));
            }

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }
        return "";
    }
    /***************************   UNFRIEND   *********************************************/
    /**
     * Unfriends a particular user
     * @param user Who to unfriend
     */
    public void unfriend(AUser user){

        new unfriendTask(user).execute();
    }

    private class unfriendTask extends AsyncTask<Void, Void, String> {
        AUser user;

        public unfriendTask(AUser user){
            this.user=user;
        }
        @Override
        protected String doInBackground(Void... Params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendUnfriendToServer(user);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Request Response Response ---");
            Log.v(TAG, result);
            Log.v(TAG, "------------------------");

            //ServerReader SR = new ServerReader(mContext);
            //List<AUser> userlist = SR.readSearchResult(result);

            //mCallback.onSearchFinished(userlist);

        }
    }

    private String sendUnfriendToServer(AUser user) throws IOException {
        // Create a new HttpClient and Post Header

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(UnfriendURL);

        try {

            AUserHelper UH = new AUserHelper(mContext);
            String myUUID = UH.getSelfID();

            Log.i(TAG, "ServerHelper / unfriend / "+user.getEmail());

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair(mSecret, mSecretString));
            nameValuePairs.add(new BasicNameValuePair(mUID, myUUID));
            nameValuePairs.add(new BasicNameValuePair(mFriend_UID, user.getUID()));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }
        return "";
    }
    /**************    *******************************************/


}
