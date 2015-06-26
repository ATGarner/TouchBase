package org.andrewgarner.amessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;


/**
 * Fragment for searching and displaying friends and requests
 */
public class Fragment_Finder extends Fragment implements BitmapHelper.BitmapHelperCallback, ServerHelper.ServerHelperCallback, AUserAdapter.AUserAdapterListener {
    public static String TAG = "andrewgarner";

    public FinderFragmentListener mListener;
    protected AUserAdapter mAdapter;
    protected AMessageHelper MH;
    protected AUserHelper UH;
    protected ServerHelper SH;
    //Which page to start on (Friends, search, requests)
    private int start_page;
    //Which page we are currently on (Friends, search, requests)
    private int current_page;
    //List of users returned from a search
    private List<AUser> searchList;
    //String of the search the user last did
    private String searchString;


    public static Fragment_Finder newInstance() {
        return new Fragment_Finder();
    }

    public Fragment_Finder() {
        // Required empty public constructor
    }

    public interface FinderFragmentListener {
        void openConversation(AUser user);

        void openProfile(AUser user);

        void searchStarted();

        void setFinderListPosition(int position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(false);
        super.onCreate(savedInstanceState);
        setRetainInstance(false);

        Log.wtf(TAG, "Fragment_Finder / onCreate");

        if (getArguments() != null) {
            //get information about where we were last at after a rotation
            start_page = getArguments().getInt(getString(R.string.bundle_finder_page), 0);
            current_page = start_page;
            //For the search string, null is used since an empty string opens all friends
            searchString = getArguments().getString(getString(R.string.bundle_finder_search_string), null);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setRetainInstance(false);
        return inflater.inflate(R.layout.activity_finder, container, false);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FinderFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setRetainInstance(false);
        super.onActivityCreated(savedInstanceState);
        setup();
    }

    /**
     * sets up important variables and objects and helpers to begin the fragment
     */
    private void setup() {
        setupSearchBar();

        //Setup Message, User, and Server Helpers
        MH = new AMessageHelper(getActivity());
        UH = new AUserHelper(getActivity());
        SH = new ServerHelper(getActivity());

        if (start_page == 0)
            showFriends();
        else if (start_page == 1)
            showRequests();
        else if (start_page == 2) {
            if (searchString != null) { //if there was a search before, show it again
                if (getView() != null) {
                    EditText et = (EditText) getView().findViewById(R.id.editText);
                    et.setText(searchString);
                    search();
                }
            }else { //if the search was empty, show friends
                start_page = 0;
                showFriends();
            }
        }

    }

    /**
     * Hnadles a new incoming request from the MainActivity
     * @param user that is requesting or being requested to
     */
    public void handleRequest(AUser user) {
        if (current_page == 0) { //if we are looking at friends, refresh page
            showFriends();
        }
        if (current_page == 1) { //if we are looking at requests, refresh page
            showRequests();
            //setup();
        } else if (current_page == 2) { //if the new user is in the search, refresh it
            if (isUserInSearchList(user)) {
                onSearchFinished(searchList);
            } //otherwise don't refresh the search
        }
    }

    /**
     * Handles a new friend or unfriending a current one
     * @param user to add or remove as friend
     */
    public void handleUser(AUser user){
        if (current_page == 0) { //if all friends page is open, refresh
            showFriends();
        } else if (current_page == 2) { //if the new user is in a search, refresh the search
            if (isUserInSearchList(user)) {
                onSearchFinished(searchList);
            }
        }
    }

    /**
     * Get the current page of the finder fragment
     * @return current page. 0 = all friends 1 = all requests 2 = search
     */
    public int getCurrent_page() {
        return current_page;
    }

    /**
     * Get the string of what the user last searched for
     * @return text of last search
     */
    public String getSearch() {
        return searchString;
    }

    /**
     * checks if a user is in the most recent search result
     * @param user to search for
     * @return whether or not the user is in the search
     */
    private boolean isUserInSearchList(AUser user) {
        for (Iterator<AUser> iterator = searchList.iterator(); iterator.hasNext(); ) {
            AUser tmpUser = iterator.next();
            if (tmpUser.getUID().equals(user.getUID())) {
                onSearchFinished(searchList);
                return true;
            }
        }
        return false;
    }


    /**
     * Setup editText to allow for searches
     */
    private void setupSearchBar() {
        View v = getView();
        if (v != null) {
            final EditText editText = (EditText) v.findViewById(R.id.editText);
            //Set IME for search icon on keyboard
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        search();
                        return true;
                    }
                    return false;
                }
            });
        } else {
            Log.e(TAG, "finder / setupSearchBar getView() error");
        }
    }

    /**
     * Display all friends in listview
     */
    public void showFriends() {
        Log.d(TAG, "finder / showFriends");
        current_page = 0;
        searchString = null;
        //tell the main activity that we are on the friend page
        mListener.setFinderListPosition(current_page);
        View v = getView();
        if (v != null) {

            hideKeyboard();
            //select all friends and friend requests to display
            List<AUser> userList = UH.selectAllFriendsOrRequestsList();
            //put some extra space in the box if the list is empty and check if null
            if(userList!=null) {
                if (!userList.isEmpty()) {
                    v.findViewById(R.id.finder_space).setVisibility(View.GONE);
                } else {
                    v.findViewById(R.id.finder_space).setVisibility(View.VISIBLE);
                }
            }

            //hide the "no results" textview
            v.findViewById(R.id.noResults).setVisibility(View.GONE);
            //set the edittext hint and remove input text
            EditText editText = (EditText) v.findViewById(R.id.editText);
            editText.setHint("All Friends");
            editText.setText("");
            //make sure the search box is displayed
            v.findViewById(R.id.editText).setVisibility(View.VISIBLE);
            v.findViewById(R.id.requests).setVisibility(View.GONE);
            v.findViewById(R.id.progress).setVisibility(View.INVISIBLE);

            //Set up the adapter and listview
            mAdapter = new AUserAdapter(getActivity(), userList, true);
            ListView lv = (ListView) v.findViewById(R.id.listView);
            lv.setVisibility(View.VISIBLE);
            lv.setAdapter(mAdapter);
            mAdapter.setViewClickListener(this);
        } else {
            Log.e(TAG, "finder / showFriends v error");
        }
    }

    /**
     * Display all incoming friend requests in the listview
     */
    public void showRequests() {
        Log.d(TAG, "finder / showRequests");
        current_page = 1;
        searchString = null;
        //tell the main activity that we are on the request page
        mListener.setFinderListPosition(current_page);
        View v = getView();
        if (v != null) {
            hideKeyboard();
            //select all user friend requests
            List<ARequest> requestList = UH.selectAllRequests();
            //if the list is null or empty, tell the user so
            if(requestList==null){
                TextView requestsTV = (TextView) v.findViewById(R.id.requests);
                requestsTV.setText("No Friend Requests");
            }else {
                if (!requestList.isEmpty()) {
                    v.findViewById(R.id.finder_space).setVisibility(View.GONE);
                    TextView requestsTV = (TextView) v.findViewById(R.id.requests);
                    requestsTV.setText("Friend Requests");
                } else {
                    v.findViewById(R.id.finder_space).setVisibility(View.VISIBLE);
                    TextView requestsTV = (TextView) v.findViewById(R.id.requests);
                    requestsTV.setText("No Friend Requests");
                }
            }
            //show requests textview and hide the searchbar and results textview
            v.findViewById(R.id.requests).setVisibility(View.VISIBLE);
            v.findViewById(R.id.editText).setVisibility(View.GONE);
            v.findViewById(R.id.noResults).setVisibility(View.GONE);
            v.findViewById(R.id.progress).setVisibility(View.INVISIBLE);

            //set up listview and adapter
            mAdapter = new AUserAdapter(getActivity(), requestList);
            ListView lv = (ListView) v.findViewById(R.id.listView);
            lv.setVisibility(View.VISIBLE);
            lv.setAdapter(mAdapter);
            mAdapter.setViewClickListener(this);
        } else {
            Log.e(TAG, "finder / showFriends v error");
        }
    }

    public void clearNotification(String uid) {
        try {
            NotificationManager notifMgr = (NotificationManager)
                    getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            int id = Integer.parseInt(uid.substring(0, 8).replaceAll("[\\D]", ""));
            notifMgr.cancel(id);
        } catch (Exception e) {
            Log.e(TAG, "Notification clear error:\n" + e + "");
            e.printStackTrace();
        }

    }

    /**
     * Implements opening requests selected in the listview adapter
     * @param request request object to display
     * @param position position that the request is in on the list
     */
    @Override
    public void openRequest(final ARequest request, final int position) {
        final AUser user = request.getUser();
        final ServerHelper SH = new ServerHelper(getActivity());

        clearNotification(user.getUID());

        //create a linearlayout to display the message of the request
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);

        //make padding of 20dp around the textview
        float scale = getActivity().getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (20 * scale + 0.5f);
        ll.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, 0);

        //get the text and format it properly and set it
        String text = request.getRequest_message();
        text = text.replace("\\n", "\n");
        text = text.replace("\\'", "'");
        final TextView tv = new TextView(getActivity());
        tv.setText(text);

        ll.addView(tv);

        //Build the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(ll)
                .setTitle(user.getName())
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //put the positive button that will accept the request
                        Log.i(TAG, "AUserRequestAdapter / Accept Request from " + request.getUser().getName());
                        //send confirmation to the server
                        SH.requestResponse(request, true);
                        //save acceptance on local databse
                        UH.acceptRequest(request);
                        //refresh the page we are on
                        if (current_page == 0)
                            showFriends();
                        else if (current_page == 1)
                            showRequests();
                        else if (current_page == 2 && isUserInSearchList(user))
                            onSearchFinished(searchList);


                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //add the negative button that deletes the request
                        //send delete message to server
                        SH.requestResponse(request, false);
                        //delete request from local database
                        UH.rejectRequest(request);
                        //refresh the page we are on
                        if (current_page == 0)
                            showFriends();
                        else if (current_page == 1)
                            showRequests();
                        else if (current_page == 2 && isUserInSearchList(user))
                            onSearchFinished(searchList);

                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog, do nothing
                    }
                });
        //Build and display the alert
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Open a user's conversation
     * @param user user that we want to view
     */
    @Override
    public void openConversation(AUser user) {
        if (!UH.isFriend(user) && !UH.hasSentRequestTo(user)) {
            openRequestAlert(user);
        } else if (!UH.isFriend(user) && UH.hasSentRequestTo(user)) {
            openYourPendingRequest(user);
        } else {
            mListener.openConversation(user);
        }
    }

    /**
     * Opens a user's profile page, sends to mainactivity to start a new activity
     * @param user
     */
    @Override
    public void openProfile(AUser user) {
        mListener.openProfile(user);
    }

    /**
     * Hides the soft keyboard
     */
    private void hideKeyboard(){
        View v = getView();
        if (v!=null) {
            InputMethodManager imm = (InputMethodManager) v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * Alert dialog to send a friend request to someone
     * @param user
     */
    public void openRequestAlert(final AUser user) {
        //create a linearlayout to hold edittext
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);

        //set a padding of 20dp for the edittext
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (20 * scale + 0.5f);
        ll.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, 0);

        //create the edittext and set a character limit
        final EditText et = new EditText(getActivity());
        et.setHint("Hi, I'd like to chat!");
        et.setSingleLine(true);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});

        //Make sentences start with capital letters and hide the keyboard on done key
        et.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                    return true;

                }
                return false;
            }
        });

        ll.addView(et);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(ll)
                .setTitle(user.getName())
                .setPositiveButton("Send Request", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Send request to the user
                        //get the message edittext text
                        String message = et.getText().toString();

                        //hide keyboard
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                        //if the message is blank, send the default message
                        if (message.equals("")) {
                            SH.request(user.getUID(), "Hi, I'd like to chat!");
                        } else {
                            SH.request(user.getUID(), message);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog, hide keyboard
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    /**
     * Open an alert dialog saying that a friend request has already been sent
     * @param user to open request from
     */
    public void openYourPendingRequest(final AUser user) {
        //create a new linearlayout to hold the textview
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);

        //make a 20dp padding
        float scale = getActivity().getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (20 * scale + 0.5f);
        ll.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, 0);

        //Set text to display
        String text = "You've already sent a friend request";
        final TextView tv = new TextView(getActivity());
        tv.setText(text);

        ll.addView(tv);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(ll)
                .setTitle(user.getName())
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do nothing, just cancel dialog
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Search the server for users. Gets the input of an edittext and sends it to a ServerHelper
     */
    private void search() {
        View v = getView();
        if (v != null) {
            //get text from the searchbar edittext
            EditText et = (EditText) v.findViewById(R.id.editText);
            et.clearFocus();
            String text = et.getText().toString();
            searchString = text;

            if (!text.equals("")) {
                //tell the mainactivity that we are searching
                mListener.searchStarted();
                //set current page
                current_page = 2;
                et.setText(text);
                hideKeyboard();

                //Send search to server
                SH.search(text);
                //result will come in onSearchFinished()

                v.findViewById(R.id.requests).setVisibility(View.GONE);
                v.findViewById(R.id.progress).setVisibility(View.VISIBLE);
                v.findViewById(R.id.noResults).setVisibility(View.GONE);

            } else {
                //if search is empty, show all friends
                searchString=null;
                et.setHint("All Friends");
                v.findViewById(R.id.progress).setVisibility(View.GONE);
                v.findViewById(R.id.noResults).setVisibility(View.GONE);

                showFriends();
            }
        } else {
            Log.e(TAG, "finder / search v error");
        }
    }

    /**
     * Gets the results of a search and displays it
     * @param list
     */
    @Override
    public void onSearchFinished(List<AUser> list){
        searchList = list;
        View v = getView();
        if (v != null) {
            Log.v(TAG, "Finder / onSearchFinished()");
            v.findViewById(R.id.progress).setVisibility(View.INVISIBLE);

            //make sure we are still on the search page before displaying results
            if(current_page==2) {
                //if search comes up empty, tell the user so
                if (list.size() == 0) {
                    v.findViewById(R.id.noResults).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.finder_space).setVisibility(View.VISIBLE);
                } else {
                    v.findViewById(R.id.noResults).setVisibility(View.GONE);
                    v.findViewById(R.id.finder_space).setVisibility(View.GONE);
                }

                //set up listview and its adapter
                mAdapter = new AUserAdapter(getActivity(), list, false);
                ListView lv = (ListView) v.findViewById(R.id.listView);
                v.findViewById(R.id.requests).setVisibility(View.GONE);
                lv.setVisibility(View.VISIBLE);
                lv.setAdapter(mAdapter);
                lv.setVisibility(View.VISIBLE);
                mAdapter.setViewClickListener(this);
            }
        } else {
            Log.e(TAG, "onSearchFinished");
        }



    }

    //Implemented methods from bitmap and serverhelper, but we don't need or use them
    @Override
    public void bitmapDownloadPicFinished(){}
    @Override
    public void onDownloadPictureFinished(){}

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }




}
