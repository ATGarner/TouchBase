package org.andrewgarner.amessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import java.util.List;


public class MainActivity extends ActionBarActivity implements
                    Fragment_Conversation.ConversationFragmentListener,
                    Fragment_List.ListConvoListener,
                    Fragment_Pager.OnFragmentInteractionListener,
                    BitmapHelper.BitmapHelperCallback,
                    ServerHelper.ServerHelperCallback,
                    Fragment_Finder.FinderFragmentListener,
                    GoogleApiClient.ConnectionCallbacks

{
    //TODO: Change variables in String.xml, including...
    //TODO: google_sender_id, secret, and the URLs
    //TODO: Also check the manifest and put in your own info
    //TODO: Put these in your gradle dependencies
    //TODO:    compile 'com.android.support:appcompat-v7:21.0.3'
    //TODO:    compile 'com.google.android.gms:play-services:7.0.0'

    //Tag for logging purposes. The tag is in String.xml
    private String TAG;
    //Name of preferences. This is in String.xml
    private String MY_PREFS;
    //Self UID for debugging
    private String MyUUID;
    //Tells if the app just started running to avoid duplicate functions on create
    private boolean justStarted;
    //Position of the fragment that is currently onscreen.
    //0 is the leftmost finder page, 1 is the middle all conversation page, 2 is the conversation page
    //we change the activity's menu based off of which page we are on
    private int menuPosition=1;
    //reference to the fragmentpager that holds the 3 main fragments
    private Fragment_Pager pagerFragment;

    //Which page the finder fragment is on. 0 is all friends, 1 is requests, 2 is search
    private int finder_page=0;
    //Return code for onActivity result for signin page
    private static final int SIGN_IN = 1;
    //Return code for onActivity result for profile page
    private static final int PROFILE = 2;
    //Client to allow for logging out
    private GoogleApiClient mGoogleApiClient;
    //false when activity has been paused, true if it has been resumed.
    //this is used for the conversation fragment to ensure proper dismissal of notifications
    private boolean activityActive=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        justStarted=true;
        activityActive=true;

        TAG = getString(R.string.TAG);
        MY_PREFS = getString(R.string.MY_PREFS);

        Log.e(TAG, "------------Start------------");
        setTitle("TouchBase");
        setBroadcastReceiver();
        firstRunSignin();

        setContentView(R.layout.activity_main);
        //make the actionbar flat with no drop shadow
        getSupportActionBar().setElevation(0);

        setupPager(savedInstanceState);

        if(!checkPlayServices()){
            Log.e(TAG, "Play Services is out of date");
        }

        //testFunction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Main / onResume");
        if(!justStarted) {
            //make sure the conversations are current
            refreshConvoList();
        }
        activityActive=true;
        justStarted=false;
        checkPlayServices();
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "Main / onPause");
    }
    @Override
    protected void onStop(){
        activityActive=false;
        super.onStop();
        Log.d(TAG, "Main / onStop");

    }
    @Override
    protected void onDestroy(){
        Log.d(TAG, "Main / onDestroy");
        super.onDestroy();
        //pagerFragment.destroyChildren();


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SIGN_IN) { //result from signin activity
            if (resultCode == RESULT_OK) {// Make sure the signin was successful
                Log.e(TAG, "Sign in RESULT_OK!");
                Bundle res = data.getExtras();
                String uid = res.getString(getString(R.string.UID));
                String name = res.getString(getString(R.string.NAME));
                String email = res.getString(getString(R.string.EMAIL));
                Log.e(TAG, "Main / Signin User: " + uid + " " + name + " " + email);

                MyUUID = uid;
                recreate(); //restart app to complete signin
            } else {
                Log.e(TAG, "SIGN_IN ERROR! Exiting...");
                finish();
            }
        } else if (requestCode == PROFILE){ //result from profile page
            if (resultCode == RESULT_OK) {
                Log.wtf(TAG, "Main / onResult / PROFILE OK");
                Bundle response = data.getExtras();
                //check if the user signed out
                boolean signout = response.getBoolean(getString(R.string.bundle_profile_signout),false);
                if(signout){ //user signed out
                    signout();
                }
                //check if the user unfriended someone
                boolean unfriend = response.getBoolean(getString(R.string.bundle_profile_unfriend),false);
                if(unfriend){ //user unfriended a friend
                    AUser user = response.getParcelable(getString(R.string.bundle_profile_unfriend_user));
                    ServerHelper SH = new ServerHelper(this);
                    SH.unfriend(user);
                    pagerFragment.unfriend(user);
                }
                //refreshConvoList();
            }
        }
    }

    /**
     * Catches if new intents are recieved
     * This will be from GCMIntentService or when app is rotated
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent){ //intent from GCMIntentService while app running
        Log.wtf(TAG, "Main / onNewIntent()");
        if(intent.hasExtra(getString(R.string.bundle_message_user))) {
            //new message came in, open that user's conversation. Also for when a user accepts request
            AUser user = intent.getParcelableExtra(getString(R.string.bundle_message_user));
            Log.wtf(TAG, "Intent User: " + user.getEmail());
            openConversation(user);
        }else if (intent.hasExtra(getString(R.string.bundle_finder_page))){//request came in
            int page = intent.getExtras().getInt((getString(R.string.bundle_finder_page)));
            Log.wtf(TAG, "Main / onNewIntent / finder_pos: "+page);
            if(page==0) { //open all friends page
                pagerFragment.openFriendList();
                //showRequestMenuOption =true;
            }else if(page==1) { //open request page
                pagerFragment.openFriendRequestList();
                //showRequestMenuOption =false;
            }

        }
    }

    /**
     * Handle Rotation
     * @param save bundle to bundle everything up into to send back on recreate
     */
    @Override
    public void onSaveInstanceState(Bundle save){
        Log.e(TAG, "--------Save Instance---------");
        save.putInt(getString(R.string.bundle_pager_position), pagerFragment.getPagerPosition());
        save.putInt(getString(R.string.bundle_message_position), pagerFragment.getConvoPosition());
        save.putInt(getString(R.string.bundle_list_position), pagerFragment.getListPosition());

        save.putInt(getString(R.string.bundle_finder_page), finder_page);//pagerFragment.getFinderPage());
        //Log.v(TAG, "Main / SaveInstance / finder_page=" + finder_page);
        save.putString(getString(R.string.bundle_finder_search_string), pagerFragment.getFinderSearch());
        //Log.v(TAG, "Main / SaveInstance / finder_search=" + pagerFragment.getFinderSearch());
        save.putString(getString(R.string.bundle_message_sendbar_text), pagerFragment.getConvoText());

        save.putParcelable(getString(R.string.bundle_message_user), pagerFragment.getConvoUser());

        Log.e(TAG, "-------- End Save Instance---------");
        //super.onSaveInstanceState(save);

    }

    /**
     * Takes app arguments on startup, whether there was an intent from a notification
     * or if the app was rotated. Then sets up the Pager Fragment
     * @param savedInstanceState if the app was rotated, includes values for setup
     */
    private void setupPager(Bundle savedInstanceState) {
        Bundle bundle = new Bundle();

        if (savedInstanceState != null) { //if rotated, paused, etc.
            pagerFragment = new Fragment_Pager();

            menuPosition = savedInstanceState.getInt(getString(R.string.bundle_pager_position), 1);
            finder_page = savedInstanceState.getInt(getString(R.string.bundle_finder_page), 0);
            Log.v(TAG, "Main / SetupPager / finder_page=" + finder_page);
            pagerFragment.setArguments(savedInstanceState);
            getFragmentManager().beginTransaction().replace(R.id.container, pagerFragment, "Pager").commit();
        } else {
            pagerFragment = new Fragment_Pager();
            if (getIntent() != null) { //Get intent from GCMIntentService, from notification, so app wasn't running
                if (getIntent().hasExtra(getString(R.string.bundle_message_user))) { //if a user is added or modified

                    AUser user = getIntent().getParcelableExtra(getString(R.string.bundle_message_user));
                    Log.wtf(TAG, "Intent User: " + user.getEmail());
                    bundle.putParcelable(getString(R.string.bundle_message_user), user); //bundle in user
                    bundle.putInt(getString(R.string.bundle_pager_position), 2);
                    pagerFragment.setArguments(bundle);

                } else if (getIntent().hasExtra(getString(R.string.bundle_finder_page))) { //a request has come in

                    menuPosition = 0;
                    bundle.putInt(getString(R.string.bundle_pager_position), 0);
                    int finder_page = getIntent().getIntExtra(getString(R.string.bundle_finder_page), 0);
                    //if (finder_page == 1)
                    //    showRequestMenuOption = false;
                    bundle.putInt(getString(R.string.bundle_finder_page), finder_page);
                    pagerFragment.setArguments(bundle);
                }
            }
            getFragmentManager().beginTransaction().replace(R.id.container, pagerFragment, "Pager").commit();
        }

    }

    /**
     * Run when app first starts
     * if this is the first time the app has been run, open the signin page
     */
    private void firstRunSignin(){

        SharedPreferences prefs = getSharedPreferences(MY_PREFS, MODE_PRIVATE);
        MyUUID = prefs.getString(getString(R.string.MY_PREFS_MyUUID),"");
        boolean firstRun = prefs.getBoolean(getString(R.string.MY_PREFS_FirstRun),true);

        if(firstRun){
            //if this is the first time the app has run, show the login page
            Intent i = new Intent(getBaseContext(),SigninActivity.class);
            startActivityForResult(i, SIGN_IN);
        } else{
            //Output all users, friends, and requests for debugging
            AUserHelper AUH = new AUserHelper(this);
            AUser myUser = AUH.selectUserBySelf();
            if(myUser==null){
                Log.wtf(TAG, "Main / I am NULL!");
            }else{
                Log.wtf(TAG, "Main / I am "+myUser.getEmail());
            }

            try {

                List<AUser> alist = AUH.selectAllUsersList();
                Log.i(TAG, "--- ALL USERS ---");
                for (AUser user : alist) {
                    Log.i(TAG, user.getEmail());
                }
                Log.i(TAG, "-----------------");
            }catch(Exception e){}

            try {
                List<AUser> flist = AUH.selectAllFriendsList();
                Log.i(TAG, "--- ALL FRIENDS ---");
                for (AUser user : flist) {
                    Log.i(TAG, user.getEmail());
                }
                Log.i(TAG, "-----------------");
            }catch(Exception e){}

            try {
                List<ARequest> rlist = AUH.selectAllRequests();
                if (rlist != null) {
                    Log.i(TAG, "--- ALL REQUESTS ---");
                    for (ARequest request : rlist) {
                        Log.i(TAG, request.getUser().getEmail());
                    }
                    Log.i(TAG, "-----------------");
                }
            }catch(Exception e){}

        }
    }

    /**
     * Set up receiver for broadcasts from GCMIntentService
     * Listens for new messages, users, and requests
     * then updates views accordingly
     */
    public void setBroadcastReceiver(){
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    if(intent.hasExtra("message")){
                        Log.i(TAG, "Main / Broadcast Receiver / message");
                        refreshConvoList();
                    }
                    if(intent.hasExtra("request")){
                        Log.i(TAG, "Main / Broadcast Receiver / request");
                        AUser user = intent.getParcelableExtra("request");
                        pagerFragment.handleFinderRequest(user);
                    }
                    if(intent.hasExtra("user")){
                        AUser user = intent.getParcelableExtra("user");
                        Log.i(TAG, "Main / Broadcast Receiver / user");
                        pagerFragment.handleFinderNewUser(user);
                    }
                }catch(Exception e){
                    Log.e(TAG, "MainAct / Broadcast / onNavDrawerSelect");
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(receiver, new IntentFilter("NewMessageIntent"));
    }

    /**
     * Sign out of the app,
     * Clears database and clears shared prefs
     * recreates the app to go back to signin page
     */
    private  void signout(){
        MyDBHelper DBH = new MyDBHelper(this);
        DBH.clearDB();
        SharedPreferences settings = getSharedPreferences(getString(R.string.MY_PREFS), Context.MODE_PRIVATE);
        settings.edit().clear().apply();

        mGoogleApiClient  = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Plus.API)
                .addScope(new Scope("profile"))
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnected(Bundle connectionHint)
    {
        //when connected, log out
        Log.e(TAG, "OnConnected!");
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        mGoogleApiClient.disconnect();
        mGoogleApiClient.connect();
        recreate();
    }

    /**
     * Required for the googleAccount connection
     * @param cause code for why the connection was suspended
     */
    @Override
    public void onConnectionSuspended(int cause) {Log.e(TAG, "OnConnectionSuspended: "+cause);}


    /**
     * Refreshes the conversation list page in the pager
     */
    public void refreshConvoList(){
        try {
            pagerFragment.refreshConvoList();
        }catch(Exception e){
            Log.e(TAG, "RefreshConvoList error");
        }
    }

    /**
     * onBackPressed is overridden so that when the back key is pressed, the app switches
     * to the middle conversation home page. If we are home then just exit the app
     */
    @Override
    public void onBackPressed(){
        if(menuPosition==1) {
            super.onBackPressed();
        }else {
            pagerFragment.goHome();
        }
    }

    /**
     * Used to determing which page's menu to show in the actionbar
     * @param menuPosition - which of the three pages we are on
     * @param finder_page which of the pages the finder is on
     */
    @Override
    public void changeMenu(int menuPosition, int finder_page){
        this.menuPosition=menuPosition;
        this.finder_page = finder_page;
        invalidateOptionsMenu();
    }

    /**
     * Open a partiucular conversation, specified by the User
     * @param user  which user to open the conversation for
     */
    @Override
    public void openConversation(AUser user){
        Log.wtf(TAG, "Main / openConversation");

        pagerFragment.openConversation(user, true);
    }

    /**
     * Open the profile page for a user, allows viewing and logout
     * @param user which user to view
     */
    @Override
    public void openProfile(AUser user){
        Intent i = new Intent(getBaseContext(),ProfileActivity.class);
        i.putExtra(getString(R.string.bundle_profile_user), user); //TODO:blah
        //keepChildren = openingProfile =true;
        startActivityForResult(i, PROFILE);
    }

    /**
     * Gets current page number for the conversation fragment
     * to decide whether or not to cancel notification
     * @return current page numnber
     */
    @Override
    public int getCurrentPageForConvoFrag(){
        return menuPosition;
    }

    /**
     * Checks if the activity has been paused. This is for the convo frag
     * If the acivity is in the background, allow notifications even if the
     * conversation page is active
     * @return
     */
    @Override
    public boolean isActivityActive(){
        return activityActive;
    }

    /**
     * Required inherited function from BitmapHelper
     * In this activity we don't need to use it
     */
    @Override
    public void bitmapDownloadPicFinished(){}

    /**
     * Required inherited function from ServerHelper
     * In this activity we don't need to use it
     */
    @Override
    public void onDownloadPictureFinished(){}


    /**
     * Returned from ServerHelper, we send it to the pager fragment
     * to then send to the finder fragment
     * @param list List of users returned from the search
     */
    @Override
    public void onSearchFinished(List<AUser> list){
        Log.d(TAG, "Main / onSearchFinished");
        pagerFragment.serverResponse(list);
    }

    /**
     * Set the menu for the finder page to another position to display different options
     * @param position to go to. 0 = all friends, 1 = requests, 2 = search
     */
    @Override
    public void setFinderListPosition(int position){
        try {
            finder_page = position;
            pagerFragment.setFinderPage(finder_page);
            if (menuPosition == pagerFragment.getPagerPosition()) {
                Log.d(TAG, "searchStarted / position same");
                invalidateOptionsMenu();
            } else {
                Log.d(TAG, "searchStarted / position different");
            }
        }catch(Exception e){Log.e(TAG, "Main / setFinderListPosition error");}
    }

    /**
     * Called when finder frag does a search, change the menu
     * so that it has the option for "All Friends"
     */
    @Override
    public void searchStarted(){
        try {
            finder_page = 2;
            pagerFragment.setFinderPage(finder_page);
            if (menuPosition == pagerFragment.getPagerPosition()) {
                Log.d(TAG, "searchStarted / position same");
                invalidateOptionsMenu();
            } else {
                Log.d(TAG, "searchStarted / position different");
            }
        }catch(Exception e){Log.e(TAG, "Main / searchStarted error");}

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        //If we are looking at the left page, finder, change the menu accordingly
        if(menuPosition==0){

            MenuItem request = menu.findItem(R.id.action_requests);
            request.setVisible(true);
            if(finder_page==0){ //We are on the all friends page, show option for requests
                request.setTitle("Friend Requests");
            }else if(finder_page==1){//we are on the requests page, show option for all friends
                request.setTitle("All Friends");
            }else if(finder_page==2){//we are on the search page, show option for all friends
                request.setTitle("All Friends");
            }


        }else if(menuPosition==1){ //We are in the all conversations page, show accoount option
            MenuItem request = menu.findItem(R.id.action_add);
            request.setVisible(true);

        } else if (menuPosition==2){
            //currently there are no options while in a conversation
        }

        //The below code puts a circular profile picture in the menu
        /*MenuItem img = menu.findItem(R.id.barIcon);
        BitmapHelper BH = new BitmapHelper(this);
        img.setIcon(getPicture(R.drawable.account));*/
        //img.setIcon(circleBM(loadBitmap(MyUUID)));

        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        if(item.getItemId() == R.id.action_requests){ //Open friendRequest page in Finder
            if(finder_page==0){
                item.setTitle("Friend Requests");
                finder_page=1;
            }else if(finder_page==1){
                item.setTitle("All Friends");
                finder_page=0;
            }else if(finder_page==2){
                item.setTitle("All Friends");
                finder_page=0;
            }
            pagerFragment.showFinderAtPage(finder_page);
            //pagerFragment.showRequests(showRequestMenuOption);
            //showRequestMenuOption = !showRequestMenuOption;
            invalidateOptionsMenu();
            return true;
        }
        if(item.getItemId() == R.id.action_add){ //open self profile page
            Intent i = new Intent(getBaseContext(), ProfileActivity.class);
            //keepChildren = openingProfile = true;
            startActivityForResult(i, PROFILE);
            return true;

        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


}
