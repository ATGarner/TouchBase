package org.andrewgarner.amessage;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class for Signin in with a google+ account into the app.
 * By using the googleAPIclient, the user can pick their google account to sign in with.
 * Once selected, the app sends Email, Name, and picture to the server.
 * If the user has logged in before, it gets their information from the server
 * Once logged in, the activity finishes and the mainactivity takes over
 *
 * Created with the help of developer.android.com
 * https://developer.android.com/training/sign-in/index.html
 *
 * Their code was fairly messy, and I did my best to clean it up and work it properly
 */
public class SigninActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private boolean mIntentInProgress;

    private String TAG;
    private String MY_PREFS;

    private boolean signin=false;

    /**
     * This is your Google API sender ID. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    private String SENDER_ID;//TODO: Change this in your strings.xml
    private String mSecret; //TODO: Change this in your string.xml
    private String signinURL; //TODO: Change this in your string.xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SENDER_ID = getString(R.string.google_sender_id);
        mSecret = getString(R.string.secret);
        signinURL = getString(R.string.url_signin);
        Log.wtf(TAG,"Signin / URL: "+signinURL);

        setContentView(R.layout.activity_signin);
        TAG = getString(R.string.TAG);
        MY_PREFS = getString(R.string.MY_PREFS);

        MyDBHelper DBH = new MyDBHelper(this);
        DBH.clearDB();
        SharedPreferences settings = getSharedPreferences(getString(R.string.MY_PREFS), Context.MODE_PRIVATE);
        settings.edit().clear().apply();

        buttonsPls();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope("profile")) //"profile"
                        //.addScope(Plus.SCOPE_PLUS_LOGIN)
                        //.addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            Toast.makeText(this, "isConnected", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_signin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void buttonsPls(){
        final TransitionDrawable td =
                new TransitionDrawable(new Drawable[] {
                        new ColorDrawable(Color.TRANSPARENT),
                        getResources().getDrawable(R.drawable.arrow)

                });

        ImageView imageView = (ImageView) findViewById(R.id.arrow);
        imageView.setImageDrawable(td);
        td.startTransition(1500);



    }
    public void signin(View v){
        if(isConnected()) {
            if (!signin) {
                //findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                signin = true;
                mGoogleApiClient.connect();
            }
        }else{
            Toast.makeText(this, "No network connection...", Toast.LENGTH_LONG).show();
        }

    }

    /*************************************************/
    @Override
    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.v(TAG, "Connection Failed");
        if (!mIntentInProgress && result.hasResolution() && signin) {
            signin=false;
            try {
                mIntentInProgress = true;
                result.startResolutionForResult(this, RC_SIGN_IN);
                Log.v(TAG, "Connection Failed / no exception");
            } catch (IntentSender.SendIntentException e) {
                Log.v(TAG, "Connection Failed / exception");
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnected()) {
                Log.v(TAG, "onActivity / not connected");
                mGoogleApiClient.reconnect();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "onConnectionSuspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.
        Log.e(TAG, "Setting up GCM");
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        setUpGCM();

    }

    private void setUpGCM(){
        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            Log.e(TAG, "RegistrationID=\n"+regid);

            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                callServer();
            }
        } else {
            Log.v(TAG, "No valid Google Play Services APK found.");
        }

    }



    /************************* SERVER ********************************/

    private String mEmail;
    private String mName;
    private String mId;
    private String mUUID;
    private Person.Image mPersonPhoto;
    private String mPhotoURL;

    private void callServer(){
        final Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        //final String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
        //Log.e(TAG, "#onConnected - GoogleApiClient accountName=" + accountName);

        mUUID = UUID.randomUUID().toString();
        mEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
        mName = currentPerson.getDisplayName();
        mPersonPhoto = currentPerson.getImage();
        mId = currentPerson.getId();
        mPhotoURL = mPersonPhoto.getUrl();

        Log.v(TAG, mUUID+" "+mEmail+" "+mName+" "+mId+ " "+mPhotoURL + " regid="+regid);


        if(isConnected()){
            Log.e(TAG, "Starting Login!");
            new SigninTask().execute(signinURL);
        }else{
            Log.e(TAG, "SigninActivity / callServer / Not Connected");
        }
    }


    private class SigninTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return sendSigninToServer(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "--- Response --");
             //Log.v(TAG, result);
            Log.v(TAG, "---------------");
            Log.v(TAG, "Signin / Result acquired");

            // ServerLoginReader slr = new ServerLoginReader(getApplicationContext());
            ServerReader sr = new ServerReader(getApplicationContext());
            sr.readMessageAddToDB(result);
            String uid = sr.getMyUUID();
            Log.e(TAG, "Login / UID2 = "+uid);
            Log.e(TAG, "ServerLoginReader");
            //uid = slr.readLoginAddToDB(result, true);
            if (!uid.equals("")){
                mUUID = uid;
            }
           /* ServerLoginReader slr = new ServerLoginReader();
            slr.readLogin();*/
            finishWithResult();
        }
    }
    private String sendSigninToServer(String myurl) throws IOException {
        // Create a new HttpClient and Post Header

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(myurl);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("secret", mSecret));
            nameValuePairs.add(new BasicNameValuePair("uid", mUUID));
            nameValuePairs.add(new BasicNameValuePair("email", mEmail));
            nameValuePairs.add(new BasicNameValuePair("name", mName));
            nameValuePairs.add(new BasicNameValuePair("photo", mPhotoURL));
            nameValuePairs.add(new BasicNameValuePair("regid", regid));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                return readFully(instream);
            }

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        return "";
    }

    public String readFully(InputStream entityResponse) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = entityResponse.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString();
    }
    /********************************** /SERVER ******************************************/


    private void finishWithResult()
    {
        Bundle conData = new Bundle();
        conData.putString(getString(R.string.UID), mUUID);
        conData.putString(getString(R.string.NAME), mName);
        conData.putString(getString(R.string.EMAIL), mEmail);
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        Log.e(TAG, "finishWithResult!");
        finish();
    }

    /* --------------------- GCM ----------------------------------*/
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    GoogleCloudMessaging gcm;
    Context context;

    private String regid;

    /* **********************GCM*********************/

    /**
     *
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.v(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.v(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the registration ID in your app is up to you.
        return getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE);
    }
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new registerTask().execute();
    }
    public class registerTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String msg;
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regid;

                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                sendRegistrationIdToBackend();

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                // Persist the registration ID - no need to register again.
                storeRegistrationId(context, regid);
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            //mDisplay.append(msg + "\n");
        }
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
        Log.v(TAG, "GCMRegID= " + regid);
        callServer();
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.v(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }


    //********************************************//
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Log.v(TAG, "checkPlayServices / error dialog");
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.v(TAG, "This device is not supported.");
                Toast.makeText(this,"This Device does not support Google Play", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }
    private boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();

    }

}
