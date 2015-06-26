package org.andrewgarner.amessage;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;


/**
 * Fragment to display a list of recent conversations with friends
 */
public class Fragment_List extends Fragment implements AUserAdapter.AUserAdapterListener {
    public static String TAG;

    public ListConvoListener mListener;
    protected AUserAdapter mAdapter;
    protected List<AMessage> messageList;
    protected AMessageHelper messageHelper;
    protected AUserHelper AUH;
    private AUser userSelected;


    public static Fragment_List newInstance() {
        return new Fragment_List();
    }

    public Fragment_List() {
        // Required empty public constructor
    }

    public interface ListConvoListener {
        void openConversation(AUser user);
        void openProfile(AUser user);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TAG = getActivity().getString(R.string.TAG);
        Log.wtf(TAG,"Fragment_List / onCreate");


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ListConvoListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        //if rotated, get the last selected user and save it
        Bundle b = getArguments();
        userSelected = b.getParcelable(getString(R.string.bundle_message_user));
        int position = b.getInt(getString(R.string.bundle_list_position),-1);
        if(userSelected!=null)
            Log.e(TAG, "ListConvo / user: "+userSelected.getEmail()+" pos: "+position);
        createConversations();
    }

    /**
     * Refresh list of conversations
     */
    public void refreshMe(){
        try {
            List<AMessage> tempList = messageHelper.selectRecentMessages();
            if (tempList.get(0).getMID().equals(messageList.get(0).getMID()) && !messageList.isEmpty()) {
                Log.e(TAG, "ListConvo / refreshMe same list");
            } else {
                Log.e(TAG, "ListConvo / refreshMe different list");
                createConversations();
            }
        }catch(Exception e){
            createConversations();
        }
    }

    /**
     * Make a user selected and display the gray bar
     * @param user user to be set as selected
     */
    public void conversationSelected(AUser user){
        userSelected = user;
        mAdapter.conversationSelected(user);
    }

    /**
     * Open up the conversation page for a user. Send to MainActivity
     * @param user to open conversation page for
     */
    @Override
    public void openConversation(AUser user){
        Log.e(TAG, "Open Convo!");
        mListener.openConversation(user);
    }

    /**
     * Unused in this class. Inherited from AUserHelper
     */
    @Override
    public void openRequest(ARequest request, int position){}
    /**
     * Open the user profile on longpress. Inherited from AUserHelper
     */
    @Override
    public void openProfile(AUser user){
        mListener.openProfile(user);
    }

    /**
     * Find recent conversations and display them in the listview with an adapter
     */
    protected void createConversations(){
        Log.d(TAG, "List / createConversations()");
        try {
            messageHelper = new AMessageHelper(getActivity());
            AUH = new AUserHelper(getActivity());
            messageList = messageHelper.selectRecentMessages();
            if(messageList.isEmpty())
                Log.d(TAG,"list is Empty! ");

            View v = getView();
            if (v != null) {
                //set up listview
                v.findViewById(R.id.listview).setVisibility(View.VISIBLE);

                //set listview adapter and click listener
                mAdapter = new AUserAdapter(getActivity(),  messageList,userSelected);
                ListView lv = (ListView) v.findViewById(R.id.listview);
                lv.setAdapter(mAdapter);
                mAdapter.setViewClickListener(this);

                if(messageList.isEmpty()){
                    Log.d(TAG,"list is Empty!");
                    v.findViewById(R.id.noConvoLayout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.listview).setVisibility(View.GONE);
                }else{
                    v.findViewById(R.id.listview).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.noConvoLayout).setVisibility(View.GONE);
                }
            }

        } catch(Exception e){
            //if list is null, display the no conversation layouts
            Log.e(TAG, " ListConvo / "+e+"");
            View v = getView();
            if (v != null) {
                v.findViewById(R.id.noConvoLayout).setVisibility(View.VISIBLE);
                v.findViewById(R.id.listview).setVisibility(View.GONE);
            }
        }
    }

    /**
     * get position of the conversation listview in case of rotation
     * @return
     */
    public int getListPosition(){
        View v = getView();
        if (v != null) {
            ListView lv = (ListView) v.findViewById(R.id.listview);
            return lv.getFirstVisiblePosition();
        }
        return -1;
    }

    /**
     * if just rotated, keep same position in the list
     * @param position
     */
    public void setListPosition(int position){
        Log.d(TAG, "ListConvo / setListPosition: "+position);
        View v = getView();
        if(v != null){
            ListView lv = (ListView) v.findViewById(R.id.listview);
            if(lv.getCount() > position)
                lv.setSelectionFromTop(position, 0);
            else
                lv.setSelectionFromTop(0, 0);
            }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }




}
