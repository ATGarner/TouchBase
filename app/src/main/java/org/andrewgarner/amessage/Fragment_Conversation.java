package org.andrewgarner.amessage;

import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment for displaying a conversation with a friend
 */
public class Fragment_Conversation extends Fragment {

    //User object of the person we are talking to
    private AUser mUser;
    //UID of the other user in the conversation
    private String mUID;
    //Text written into the send bar, saved in case of rotation or reload
    private String mText;
    private final static String TAG = "andrewgarner";

    //Position in the list to display messages, used in case of rotation to find our last spot
    private int message_start_position;

    private ConversationFragmentListener mListener;
    private AMessageAdapter AMA;

    AMessageHelper AMHelper;
    AUserHelper AUHelper;
    List<AMessage> messageList;


    /**
     * Creates a new instance of the conversation and sets bundle parameters
     * @param bundle info to be passed in
     * @return fragment displaying a particular conversation
     */
    public static Fragment_Conversation newInstance(Bundle bundle) {
        Log.i(TAG, "--New Conversation--");
        Fragment_Conversation fragment = new Fragment_Conversation();
        fragment.setArguments(bundle);
        return fragment;
    }
    public Fragment_Conversation() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG,"Fragment_Convo / onCreate");
        if (getArguments() != null) {
            //get information of the user to display
            mUser = getArguments().getParcelable(getString(R.string.bundle_message_user));
            //Where to start in the message list
            message_start_position = getArguments().getInt(getString(R.string.bundle_message_position), -1);
            //Text written in the sendbar (if rotated)
            mText = getArguments().getString(getString(R.string.bundle_message_sendbar_text),"");
            //get the ID of the user
            mUID = mUser.getUID();
            AUHelper = new AUserHelper(getActivity());


            Log.v(TAG, "Convo / Email: "+mUser.getEmail()+" pos: "+message_start_position);

        } else {
            Log.i(TAG, "No Arguments Set");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //start off with keyboard hidden
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //remove notification from notification drawer
        clearNotification();
        //set user we are talking to
        setUser();
        //set a reciever for incoming messages
        setBroadcastReceiver();
        //put messages to and from this user into a list and display it
        createMessages();
        //set up bottom send bar to type in and send with
        configSendBar();
    }

    private void setUser() {
        AUHelper = new AUserHelper(getActivity());
        AUHelper.setUserLastSeen(mUID);
    }

    /**
     * get messages for the user and set up an adapter to display them
     */
    private void createMessages() {
        Log.v(TAG, "ConvoFrag / createMessages()");
        try {
            AMHelper = new AMessageHelper(getActivity());
            messageList = AMHelper.selectMessagesByUID(mUID);
        }catch(Exception e) {
            messageList = new ArrayList<AMessage>();
            Log.e(TAG, "ConvoFrag / createMessages / " + e);
        }

        View v = getView();
        if (v != null) {
            v.findViewById(R.id.root).setVisibility(View.VISIBLE);
            ListView lv = (ListView) v.findViewById(R.id.listview);
            //set up adapter for the list of messages for the listview
            AMA = new AMessageAdapter(getActivity(), messageList);
            lv.setAdapter(AMA);
            if(message_start_position>=0)
                //set message position if needed from rotation
                setListPosition(message_start_position);
        }

    }

    /**
     * Clears any notification relevant to this user when we are looking at the profile
     */
    public void clearNotification() {
        try {
            NotificationManager notifMgr = (NotificationManager)
                    getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            int id = Integer.parseInt(mUID.substring(0, 8).replaceAll("[\\D]", ""));
            notifMgr.cancel(id);
        } catch (Exception e) {
            Log.e(TAG, "Notification clear error:\n" + e + "");
            e.printStackTrace();
        }

    }

    /**
     * refresh view and show new user
     * @param user user to display
     */
    public void refreshMe(AUser user) {
        message_start_position=-1;
        mUser = user;

        if (user.getUID().equals(mUID)) {
            List<AMessage> newList = AMHelper.selectMessagesByUID(user.getUID());
            if(newList.size() == messageList.size()){
                return;
            }

        } else {
            mUID = user.getUID();
            setUser();
        }
        createMessages();
    }

    /**
     * @return Text in the sendbar editText
     */
    public String getText(){
        final View view = getView();
        if (view != null) {
            final EditText edittxt = (EditText) view.findViewById(R.id.messageBox);
            return edittxt.getText().toString();
        }
        return "";
    }

    /**
     * @return current user being viewed
     */
    public AUser getUser(){
        return mUser;
    }

    /**
     * get position of the listview for rotation purposes
     * Since we can only set position of the top of the visible
     * part of the listview, if we are near the bottom just return the bottom
     * so that the bottom messages can be seen
     * @return position of listview message that is seen
     */
    public int getListPosition(){
        View v = getView();
        if (v != null) {
            ListView lv = (ListView) v.findViewById(R.id.listview);
            int first = lv.getFirstVisiblePosition();
            int last = lv.getLastVisiblePosition();
            int count = lv.getCount();
            Log.d(TAG, "First:"+first+" Last:"+last+" Count: "+count);
            if(last+3>=count) //if we are near the bottom, show all of bottom message
                return last;
            return first;
            //return lv.getLastVisiblePosition();
        }
        return 0;
    }

    /**
     * set position of the listview, used for rotation to keep the right place
     * @param position
     */
    public void setListPosition(int position){
        View v = getView();
        if(v != null){
            ListView lv = (ListView) v.findViewById(R.id.listview);
            if(lv.getCount() > position)
                lv.setSelectionFromTop(position, 0);
            else {
                lv.setSelectionFromTop(0, 0);
            }
        }
    }

    /**
     * set reciever for incoming message broadcasts
     */
    private void setBroadcastReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "ConvoFrag | Broadcast onReceive");
                AMessage amsg = intent.getExtras().getParcelable("message");
                if(amsg!=null) {
                    //make sure it is for the right conversation
                    if(amsg.getUID().equals(mUID))
                        broadcastHelper(amsg);
                }

            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(receiver, new IntentFilter("NewMessageIntent"));


    }
    private void broadcastHelper(AMessage amsg){
        AMA.addMessage(amsg);
        try {
            if (mListener.getCurrentPageForConvoFrag() == 2 && mListener.isActivityActive()) {
                clearNotification();
            }
        }catch(Exception e){
            Log.e(TAG,"Frag_Convo / broadcastHelper / error "+e);
        }

    }


    /**
     * Configure Bottom Send Bar to accept input
     * Also set up the ListView
     */
    private void configSendBar() {
        final View view = getView();
        if (view != null) {

            //setup edittext to have previous text if rotated
            final EditText edittxt = (EditText) view.findViewById(R.id.messageBox);
            if(!mText.equals("")){
                edittxt.setText(mText);
                edittxt.requestFocus();
            }

            //Set up listview to hide keyboard on scroll
            ListView lv = (ListView) view.findViewById(R.id.listview);
            lv.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState != 0){

                        InputMethodManager imm = (InputMethodManager) view.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        edittxt.clearFocus();

                    }
                }
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                }
            });
            //set up send button
            ImageButton imgbtn = (ImageButton) view.findViewById(R.id.sendButton);
            imgbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendClicked(edittxt);
                }
            });
            //Set up edittext to have a sendbutton
            edittxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendClicked(edittxt);
                        return true;
                    }
                    return false;
                }
            });

        }
    }

    /**
     * User sends message, get text and then send it to the Server
     * @param et Edittext from sendBar
     */
    private void sendClicked(EditText et) {
        String messageText = et.getText().toString();

        et.setText("");
        if (!messageText.equals("")) { //check that message isn't empty

            AMessage newMessage = AMHelper.createMessageReturnAMessage(mUID, messageText, null, true);
            Log.i(TAG, "New message isSelf()= " + newMessage.isSelf());

            if (isConnected()) { //check for internet connection
                Log.e(TAG, "Sending Message!");

                //send to server
                ServerHelper SH = new ServerHelper(getActivity());
                SH.sendMessage(newMessage);
                mListener.refreshConvoList();

                //add message to list and refresh
                messageList.add(newMessage);
                AMA.notifyDataSetChanged();

            } else {
                Log.e(TAG, "SigninActivity / callServer / Not Connected");
                Toast.makeText(getActivity(), "No Connection!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * checks for internet connection
     * @return true if connected
     */
    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ConversationFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Interface for calling mainactivity to display new message in the list
     */
    public interface ConversationFragmentListener {
        int getCurrentPageForConvoFrag();
        boolean isActivityActive();
        void refreshConvoList();
    }
}
