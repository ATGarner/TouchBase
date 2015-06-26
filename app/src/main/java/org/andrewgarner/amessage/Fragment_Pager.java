package org.andrewgarner.amessage;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

/**
 * Created by Andrew on 4/29/2015.
 */
public class Fragment_Pager extends Fragment {
    ListPagerAdapter mAdapter;

    ViewPager mPager;

    private static String TAG;
    static String MyUUID;
    private static AUser mUser=null;
    AUserHelper AUH;

    private OnFragmentInteractionListener mListener;

    public Fragment_Pager() {
        // Required empty public constructor
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public interface OnFragmentInteractionListener {
        void changeMenu(int position, int finder_position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        TAG = getString(R.string.TAG);
        Bundle bundle = this.getArguments();
        if(bundle!=null){
            Log.v(TAG, "Fragment_Pager / onCreateView / Bundle not null");
            bundleHandler(bundle);
        }else{
            Log.v(TAG, "Fragment_Pager / onCreateView / Bundle null");
            mUser = null;
        }

        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    private int pager_start_position=1;
    private int message_start_position=-1;
    private int finder_page_position=0;
    private int list_start_position;
    private int list_clicked_position;
    private String message_text="";
    private String finder_search_string;

    /**
     * if we just came off
     * @param b bundle for the saved items from rotation
     */
    private void bundleHandler(Bundle b){
        mUser = b.getParcelable(getString(R.string.bundle_message_user));

        //Get important variables from the bundle

        pager_start_position = b.getInt(getString(R.string.bundle_pager_position), 1);
        message_start_position = b.getInt(getString(R.string.bundle_message_position), -1);
        finder_page_position = b.getInt(getString(R.string.bundle_finder_page),0);
        finder_search_string = b.getString(getString(R.string.bundle_finder_search_string), null);
        list_start_position = b.getInt(getString(R.string.bundle_list_position), -1);
        list_clicked_position = b.getInt(getString(R.string.bundle_list_clicked),-1);
        message_text = b.getString(getString(R.string.bundle_message_sendbar_text), "");

        /*Log.v(TAG, "Fragment_Pager / bundleHandler / Pager Start Pos: " + pager_start_position);
        Log.v(TAG, "Fragment_Pager / bundleHandler / Message Start Pos: " + message_start_position);
        Log.wtf(TAG, "Fragment_Pager / bundleHandler / finder page pos: " + finder_page_position);
        Log.v(TAG, "Fragment_Pager / bundleHandler / List Start Pos: " + list_start_position);*/

        if(mUser==null){
            message_start_position=-1;
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.v(TAG, "Fragment_Pager / onActivityCreated");
        AUH = new AUserHelper(getActivity());
        setup();
    }

    public void rotateHandler(Bundle b){
        bundleHandler(b);
        rotateSetup();
    }
    public void rotateSetup(){
        View v = getView();
        if(v==null){
            Log.wtf(TAG,"View is null...");
        }else{
            Log.wtf(TAG,"View is not null!!!");
        }

        mAdapter = new ListPagerAdapter(getActivity().getFragmentManager());

        mPager = (ViewPager)getActivity().findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pager_start_position);

        if(pager_start_position==0){ //go to previously opened page if rotated
            getActivity().setTitle("Friends");
        }else if(pager_start_position==1){
            getActivity().setTitle("TouchBase");
        }
        mPager.setOffscreenPageLimit(2); //allows all 3 pages to be open in memory at once


        if(list_start_position>0){ //see if we were previously scrolled down somewher on the
            setListPosition(list_start_position);
        }

        if(mUser!=null){
            //if we got a user conversation from the rotation, open it again. scroll if we were on that page
            Log.v(TAG, "Fragment_Pager / onActivityCreated / mUser is" + mUser.getEmail());
            openConversation(mUser,pager_start_position==2);
        }
    }

    public void setup(){
        setRetainInstance(false);
        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.MY_PREFS), getActivity().MODE_PRIVATE);
        MyUUID = prefs.getString(getString(R.string.MY_PREFS_MyUUID), "");

        mAdapter = new ListPagerAdapter(getActivity().getFragmentManager());

        mPager = (ViewPager)getActivity().findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(pager_start_position);

        if(pager_start_position==0){ //go to previously opened page if rotated
            getActivity().setTitle("Friends");
        }else if(pager_start_position==1){
            getActivity().setTitle("TouchBase");
        }
        mPager.setOffscreenPageLimit(2); //allows all 3 pages to be open in memory at once

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            /**
             * Handle different pages being selected
             * First, set softInput mode to handle keyboard opening
             * We don't want the screen to adjust on the finder page
             * But we do want it to for the conversation page.
             * Next, change the title in the actionbar to reflect which page we are on
             */
            @Override
            public void onPageSelected(int position) {
                mListener.changeMenu(position, getFinderPage());
                if (mPager.getCurrentItem() == 0) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    getActivity().setTitle("Friends");
                } else if (mPager.getCurrentItem() == 1) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    getActivity().setTitle("TouchBase");
                } else if (mPager.getCurrentItem() == 2) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    getActivity().setTitle(mUser.getName());
                    AUH.setUserLastSeen(mUser.getUID());
                    mAdapter.clearConvoNotification();

                }
            }

            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
            }

            /**
             * If the pager is scrolled horizontally, dismiss the keyboard if open
             * @param state
             */
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) { // Hide the keyboard
                    ((InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mPager.getWindowToken(), 0);
                }

            }
        });

        if(list_start_position>0){ //see if we were previously scrolled down somewher on the
            setListPosition(list_start_position);
        }

        if(mUser!=null){
            //if we got a user conversation from the rotation, open it again. scroll if we were on that page
            Log.v(TAG, "Fragment_Pager / onActivityCreated / mUser is" + mUser.getEmail());
            openConversation(mUser,pager_start_position==2);
        }
    }


    public void openConversation(AUser user, boolean scroll){
        Log.v(TAG, "Fragment_Pager / openConvo ");
        //mUser = user;
        if(AUH.isFriend(user)) {
            mUser = user;

            if (mAdapter != null) {
                if (mAdapter.getCount() < 3) {
                    mAdapter.setCount(3);
                    mAdapter.notifyDataSetChanged();
                } else {
                    mAdapter.changeConvo(user);
                }
                if (scroll) {
                    mPager.setCurrentItem(2, true);
                    getActivity().setTitle(user.getName());
                }
                mAdapter.setListUserSelected(user);
                AUH.setUserLastSeen(user.getUID());
                //mAdapter.refreshConvoList();
            } else {
                Log.e(TAG, "Fragment_Pager / openConvo / Adapter is null");
            }
        }
    }
    public void openFriendList(){
        finder_page_position=0;
        mAdapter.openFriendList();
        mPager.setCurrentItem(0, true);
    }
    public void openFriendRequestList(){
        finder_page_position=1;
        mAdapter.openFriendRequestList();
        mPager.setCurrentItem(0, true);
    }
    public void showRequests(boolean requests){
        if(requests)
            finder_page_position=1;
        else
            finder_page_position=0;
        if(mPager.getCurrentItem() == 0){
            mAdapter.showRequests(requests);
        }
    }
    public void showFinderAtPage(int page){
        finder_page_position=page;
        if(mPager.getCurrentItem() == 0){
            if(page==0){
                openFriendList();
            }else if(page==1){
                openFriendRequestList();
            }else if(page==2){

            }
        }
    }


    /**
     * Handle if a user is unfriended while the app is open
     * @param user user to remove from views
     */
    public void unfriend(AUser user){
        //if we are currently in the conversation for the unfriended user, tear down the views to
        //  properly handle the unfriending.
        if(mUser!=null) {
            if (mUser.getUID().equals(user.getUID()) || mAdapter.getConvoUser().getUID().equals(user.getUID())) {
                Log.wtf(TAG,"************ RECREATE ******");
                getActivity().recreate();
                //mAdapter.setCount(2);
                mAdapter.refreshConvoList();
                mAdapter.notifyDataSetChanged();
                //mUser = null;
                pager_start_position = 1;

                //((InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE))
                //        .hideSoftInputFromWindow(mPager.getWindowToken(), 0);

                //getActivity().recreate();
            }
        }
        //if we weren't on the unfriended user's conversation, refresh convo list and finder
        mAdapter.forceRefreshConvoList();
        mAdapter.finderHandleDeleteUser(user);
    }


    /**
     * @return Position of Conversations List
     */
    public int getListPosition(){
        try{
            return mAdapter.getListConvoPosition();
        }catch(Exception e){
            Log.e(TAG, "Fragment_Pager / getListConvoPosition / error");
            return -1;
        }
    }

    /**
     * Set which position to go to in the Conversation List
     * @param position
     */
    public void setListPosition(int position){
        mAdapter.setListConvoPosition(position);
    }

    /**
     * @return position of Messages in the open conversation
     */
    public int getConvoPosition(){
        try{
            return mAdapter.getConvoPosition();
        }catch(Exception e){
            Log.e(TAG, "Fragment_Pager / getConvoPosition / error");
            return -1;
        }
    }

    /**
     * Finds the text in the conversation editText for rotation
     * @return text in the editText
     */
    public String getConvoText(){
        try {
            return mAdapter.getConvoText();
        }catch(Exception e){
            return "";
        }
    }

    /**
     * @return if page is search, all friends, or requests
     */
    public int getFinderPage(){
        finder_page_position = mAdapter.getFinderPage();
        return finder_page_position;
    }
    public void setFinderPage(int page){
        finder_page_position= page;
    }
    public String getFinderSearch(){
        try {
            return mAdapter.getFinderSearch();
        }catch(Exception e){
            return null;
        }
    }

    /**
     * Jumps to the main page
     */
    public void goHome() {
        mPager.setCurrentItem(1, true);
    }

    /**
     * @return User that the conversation is open for
     */
    public AUser getConvoUser(){
        return mUser;
    }

    /**
     * @return position of the Fragment Pager:
     * 0 = Finder, 1 = List of Conversations, 2 = Conversation
     */
    public int getPagerPosition(){
        return mPager.getCurrentItem();
    }
    public void serverResponse(List<AUser> list) {
        mAdapter.serverResponse(list);
    }
    public void refreshConvoList(){
        mAdapter.refreshConvoList();
    }

    /**
     * Send the user requesting in to the Finder Fragment
     * @param user that is requesting
     */
    public void handleFinderRequest(AUser user){
        mAdapter.finderHandleRequest(user);
    }
    /**
     * the finder needs a little help to decide which page to display
     * when a user comes in. We need to decide whether or not
     * to refresh a list or search
     * @param user that is requesting
     */
    public void handleFinderNewUser(AUser user){
        Log.wtf(TAG, "pager / handleFinderNewUser");
        mAdapter.finderHandleNewUser(user);
        mAdapter.refreshConvoList();
        if(!user.isFriend()) {
            Log.wtf(TAG,"pager / handlefindernewuser / is not friend");
            unfriend(user);
        }
    }

    @Override
    public void onDestroy(){
        Log.wtf(TAG, "Pager / onDestroy()");
        super.onDestroy();
    }



    /**
     * Adapter for displaying the 3 panels of fragments
     */
    public class ListPagerAdapter extends PagerAdapter {
        FragmentManager fragmentManager;
        Fragment[] fragments;
        //start off with only 2 pages, finder and listConvo
        private int count=2;

        public ListPagerAdapter(FragmentManager fm){
            fragmentManager = fm;
            //make it of size 3 so that we can add the conversation fragment later
            fragments = new Fragment[3];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //assert(0 <= position && position < fragments.length);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.remove(fragments[position]);
            trans.commit();
            fragments[position] = null;
        }

        public void destroyChildren(){
            Log.wtf(TAG, "FragmentPager / ListAdapter / Destroying Children!");
            try {
                FragmentTransaction trans = fragmentManager.beginTransaction();
                trans.remove(fragments[0]);
                trans.commit();
                fragments[0] = null;

                FragmentTransaction trans2 = fragmentManager.beginTransaction();
                trans2.remove(fragments[1]);
                trans2.commit();
                fragments[1] = null;

                if(count >2) {
                    FragmentTransaction trans3 = fragmentManager.beginTransaction();
                    trans3.remove(fragments[2]);
                    trans3.commit();
                    fragments[2] = null;
                }

            }catch(Exception e){
                Log.e(TAG, "Fragment_Pager / ListAdapter / DestroyChildren() error: "+e);
            }
        }

        @Override
        public Fragment instantiateItem(ViewGroup container, int position){
            Fragment fragment = getItem(position);
            FragmentTransaction trans = fragmentManager.beginTransaction();
            trans.add(container.getId(), fragment, "fragment:" + position);
            trans.commit();
            return fragment;
        }

        public void changeConvo(AUser user){
            try{
                Fragment_Conversation convoFrag = (Fragment_Conversation) fragments[2];
                convoFrag.refreshMe(user);
                /*Fragment_List listFrag = (Fragment_List) fragments[1];
                listFrag.conversationClicked(user);*/
            }catch(Exception e){
                Log.e(TAG, "Fragment_Pager / changeConvo error\n"+e);
                e.printStackTrace();
            }
        }
        public void refreshConvoList(){
            try{
                Fragment_List listFrag = (Fragment_List) fragments[1];
                listFrag.refreshMe();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshConvoList " + e);
            }
        }
        public void clearConvoNotification(){
            try{
                Fragment_Conversation convoFrag = (Fragment_Conversation) fragments[2];
                convoFrag.clearNotification();
            }catch(Exception e){
                Log.e(TAG, "Fragment_Pager / clearConvoNotification error\n"+e);
                e.printStackTrace();
            }
        }
        public void forceRefreshConvoList(){
            try{
                Fragment_List listFrag = (Fragment_List) fragments[1];
                listFrag.refreshMe();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshConvoList " + e);
            }
        }

        public void finderHandleRequest(AUser user){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.handleRequest(user);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshFinderList " + e);
            }
        }
        public void finderHandleNewUser(AUser user){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.handleUser(user);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshFinderList " + e);
            }
        }
        public void finderHandleDeleteUser(AUser user){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.handleUser(user);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshFinderList " + e);
            }
        }
        public int getFinderPage(){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                return finderFrag.getCurrent_page();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshFinderList " + e);
                return 0;
            }
        }
        public String getFinderSearch(){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                return finderFrag.getSearch();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshFinderList " + e);
                return "";
            }

        }
        /**
         * Send the search result to the Finder
         * @param list list of users returned by search
         */
        public void serverResponse(List<AUser> list){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.onSearchFinished(list);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshConvoList " + e);
            }
        }
        public void showRequests(boolean requests){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                if(requests)
                    finderFrag.showRequests();
                else
                    finderFrag.showFriends();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / refreshConvoList "+e);
            }
        }
        public int getListConvoPosition(){
            try{
                Fragment_List listFrag = (Fragment_List) fragments[1];
                return listFrag.getListPosition();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / getListConvoPosition error:" + e);

            }
            return -1;
        }
        public void setListConvoPosition(int position){
            try{
                Fragment_List listFrag = (Fragment_List) fragments[1];
                listFrag.setListPosition(position);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / setListConvoPosition error:" + e);
            }
        }
        public void setListUserSelected(AUser user){
            try{
                Fragment_List listFrag = (Fragment_List) fragments[1];
                listFrag.conversationSelected(user);
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / setListUserSelected error:" + e);
            }
        }
        public String getConvoText(){
            try{
                Fragment_Conversation convoFrag = (Fragment_Conversation) fragments[2];
                return convoFrag.getText();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / getConvoPosition error:" + e);
            }
            return "";
        }
        public AUser getConvoUser(){
            try{
                Fragment_Conversation convoFrag = (Fragment_Conversation) fragments[2];
                return convoFrag.getUser();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / getConvoPosition error:" + e);
            }
            return null;
        }
        public int getConvoPosition(){
            try{
                Fragment_Conversation convoFrag = (Fragment_Conversation) fragments[2];
                return convoFrag.getListPosition();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / getConvoPosition error:" + e);
            }
            return -1;
        }

        public void openFriendRequestList(){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.showRequests();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / openFriendRequestList " + e);
            }
        }
        public void openFriendList(){
            try{
                Fragment_Finder finderFrag = (Fragment_Finder) fragments[0];
                finderFrag.showFriends();
            } catch(Exception e){
                Log.e(TAG, "Fragment Pager / openFriendRequestList " + e);
            }
        }

        public void setCount(int count){
            this.count=count;
        }
        @Override
        public int getCount() {
            return this.count;
        }

        @Override
        public boolean isViewFromObject(View view, Object fragment) {
            return ((Fragment) fragment).getView() == view;
        }

        /**
         * Get the fragment at the position of the pager
         * Send in arguments via bundle for opening after a rotation
         * or when a user is clicked on to chat
         * @param position which fragment to open
         * @return fragment to display at that position
         */
        public Fragment getItem(int position){
            //assert(0 <= position && position < fragments.length);
            if(fragments[position] == null){
                if(position==0) { //Get Finder Fragent
                    Bundle bundle = new Bundle();
                    bundle.putInt(getString(R.string.bundle_finder_page),finder_page_position);
                    bundle.putString(getString(R.string.bundle_finder_search_string),finder_search_string);
                    fragments[position] = Fragment_Finder.newInstance();
                    fragments[position].setArguments(bundle);
                }else if (position==1){ //Get List Fragment
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(getString(R.string.bundle_message_user), mUser);
                    bundle.putInt(getString(R.string.bundle_list_position), list_start_position);
                    fragments[position] = Fragment_List.newInstance();
                    fragments[position].setArguments(bundle);

                }else if (position==2){ //Get Conversation Fragment
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(getString(R.string.bundle_message_user),mUser);
                    bundle.putInt(getString(R.string.bundle_message_position),message_start_position);
                    bundle.putString(getString(R.string.bundle_message_sendbar_text),message_text);

                    fragments[position] = Fragment_Conversation.newInstance(bundle);

                }
            }
            return fragments[position];
        }

    }

}