package org.andrewgarner.amessage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter for listviews. Handles 4 different cases:
 * 1. List of conversations
 * 2. List of friends
 * 3. List of friend requests
 * 4. Search results
 * Created by andrewgarner on 5/5/15.
 */
public class AUserAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private List<AMessage> messageList;
    private List<AUser> userList;
    private List<ARequest> requestList;
    private AMessageHelper AMH;
    private AUserHelper AUH;
    private final String TAG = "andrewgarner";
    private BitmapHelper BH;

    private boolean isMessageList=false;
    private boolean isAllList=false;
    private boolean isRequestList=false;

    private boolean save;
    private int lastClicked=-1;
    private View lastGrayBar;
    private AUser userSelected=null;

    /**
     * List Adapter for displaying the list of conversations
     * @param context
     * @param messageList
     * @param userSelected
     */
    public AUserAdapter(Context context, List<AMessage> messageList, AUser userSelected) {
        AMH = new AMessageHelper(context);
        BH = new BitmapHelper(context);
        AUH = new AUserHelper(context);

        this.context = context;
        this.messageList = messageList;
        this.save=true;

        isMessageList=true;
        this.userSelected = userSelected;
    }

    /**
     * List Adapter for displaying list of users, either friends or search results
     * @param context
     * @param userList
     * @param save
     */
    public AUserAdapter(Context context, List<AUser> userList, boolean save) {
        AMH = new AMessageHelper(context);
        BH = new BitmapHelper(context);
        AUH = new AUserHelper(context);

        this.context = context;
        this.userList = userList;
        this.save = save;

        isAllList=true;

    }

    /**
     * List Adapter for displaying list of user requests
     * @param context
     * @param requestList
     */
    public AUserAdapter(Context context, List<ARequest> requestList) {
        AMH = new AMessageHelper(context);
        BH = new BitmapHelper(context);
        AUH = new AUserHelper(context);

        this.context = context;
        this.requestList = requestList;

        isRequestList=true;

    }

    private AUserAdapterListener mAdapterListener;

    public interface AUserAdapterListener {
        void openConversation(AUser user);
        void openRequest(ARequest request, int position);
        void openProfile(AUser user);
    }

    public void setViewClickListener (AUserAdapterListener listener) {
        mAdapterListener = listener;
    }

    @Override
    public int getCount() {
        if(isMessageList)
            return messageList.size();
        if(isAllList)
            return userList.size();
        return requestList.size();
    }

    public void conversationSelected(AUser userSelected){
        Log.v(TAG,"UserAdapter / conversationSelected: "+userSelected.getEmail());
        this.userSelected=userSelected;
        lastClicked=-1;
        if(lastGrayBar!=null)
            lastGrayBar.setVisibility(View.GONE);
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int location) {
        return messageList.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //Log.e(TAG, "AUserAdapter / getView="+position);
        final ViewHolder holder;
        if (inflater == null)
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.fragment_list_person, parent, false);

            holder = new ViewHolder();
            holder.nameTV = (TextView) convertView.findViewById(R.id.name);
            holder.picIV = (ImageView) convertView.findViewById(R.id.picture);
            holder.subtextTV = (TextView) convertView.findViewById(R.id.subtext);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.person_progressBar);
            holder.star = (ImageView) convertView.findViewById(R.id.star);
            holder.grayBar = convertView.findViewById(R.id.grayBar);

            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        String UID;
        String name;
        String text;
        boolean notify;
        final AUser user;

        if(isMessageList){ //If the list is for recent messages
            AMessage msg = messageList.get(position);
            user = AUH.selectUserByUID(msg.getUID());

            UID = user.getUID();
            name = user.getName();
            text = msg.getText();
            if(msg.isSelf())
                text = "You: "+text;
            notify= user.getLastSeen() < msg.getDate() && !msg.isSelf();

            //onclick listener to open conversation
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAdapterListener != null) {
                        Log.v(TAG, "LIST / addConvo / onClick user="+user.getEmail());
                        if(lastGrayBar!=null){
                            lastGrayBar.setVisibility(View.GONE);
                        }
                        mAdapterListener.openConversation(user);
                        //Below sets up the gray bar next to the conversation, showing that it is open

                        lastClicked=position;
                        lastGrayBar=holder.grayBar;
                        holder.grayBar.setVisibility(View.VISIBLE);
                        holder.nameTV.setTypeface(null, Typeface.NORMAL);
                        holder.subtextTV.setTypeface(null, Typeface.NORMAL);
                        AUH.setUserLastSeen(user.getUID());


                    }
                }
            });
            //Long press listener that opens profile page for that user
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    //Toast.makeText(context, user.getEmail(), Toast.LENGTH_LONG).show();
                    mAdapterListener.openProfile(user);
                    return true;
                }

            });

            //if the item is refreshed, make sure it keeps it's gray bar if it had it before
            if(lastClicked==position){
                holder.grayBar.setVisibility(View.VISIBLE);
                lastGrayBar=holder.grayBar;
            }else{
                holder.grayBar.setVisibility(View.GONE);

            }
            //check if the last selected user is this user
            if(userSelected!=null){
                if(user.getUID().equals(userSelected.getUID())) {
                    //if they are the same user, set up gray bar
                    lastClicked = position;

                    holder.grayBar.setVisibility(View.VISIBLE);
                    lastGrayBar = holder.grayBar;
                }
            }
        }else if(isAllList){ //if the list is for all users (or search results)
            user = userList.get(position);

            UID = user.getUID();
            name = user.getName();
            text = user.getEmail();
            notify=false;

            //check if the user has a request or is not a friend
            if(!AUH.hasRequestFrom(user) || AUH.isFriend(user)) {
                //if no request or is a friend, open normally

                if (AUH.isFriend(user)) { //a star denotes a friend
                    holder.star.setVisibility(View.VISIBLE);
                } else {
                    holder.star.setVisibility(View.GONE);
                }

                //open conversation for this user on click
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mAdapterListener != null) {
                            Log.v(TAG, "LIST / addConvo / onClick user=" + user.getEmail());
                            //Open conversation (or friend request if not a friend)
                            mAdapterListener.openConversation(user);
                            AUH.setUserLastSeen(user.getUID());
                        }
                    }
                });

                //open profile page for this user on long press
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        mAdapterListener.openProfile(user);
                        return true;
                    }

                });
            }else{ //the user is not a friend and may have a friend request
                holder.star.setVisibility(View.GONE);
                final ARequest request = AUH.selectRequestFromUser(user);

                if(!request.isSelf()) {
                    text = request.getRequest_message();
                }

                //open the friend request
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mAdapterListener != null) {
                            Log.v(TAG, "LIST / addConvo / onClick user="+user.getEmail());
                            mAdapterListener.openRequest(request, position);
                        }
                    }
                });

            }
        }else{ //if list is for requests
            final ARequest request = requestList.get(position);
            user = request.getUser();

            //if a friend ended up in the request list, remove the person and refresh the list
            if(user.isFriend()){
                requestList.remove(position);
                notifyDataSetChanged();
                return convertView;
            }

            UID = user.getUID();
            name = user.getName();
            text = request.getRequest_message();

            notify=false;

            //open the friend request
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAdapterListener != null) {
                        Log.v(TAG, "LIST / addConvo / onClick user="+user.getEmail());

                        mAdapterListener.openRequest(request, position);
                    }
                }
            });
        }

        text = text.replace("\\n","\n");
        text = text.replace("\\'","'");
        text = text.replace("\\\"", "\"");


        holder.nameTV.setText(name);
        holder.subtextTV.setText(text);

        if(notify) {
            holder.nameTV.setTypeface(null, Typeface.BOLD);
            holder.subtextTV.setTypeface(null, Typeface.BOLD);
        }
        holder.picIV.setTag(UID);
        holder.picIV.setVisibility(View.INVISIBLE);

        if(user.photoNeedsDownload()){
            holder.picIV.setVisibility(View.INVISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);
            BH.downloadPictureSetIV(user, holder.picIV, holder.progressBar, save);
        } else {
            new loadImageTask(holder.picIV, user).execute();
        }

        /*convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapterListener != null) {
                    Log.v(TAG, "LIST / addConvo / onClick user="+user.getEmail());
                    mAdapterListener.openConversation(user);
                }
            }
        });*/
        return convertView;
    }


    static class ViewHolder {
        TextView nameTV;
        TextView subtextTV;
        ImageView picIV;
        ProgressBar progressBar;

        ImageView star;
        View grayBar;
    }

    /**
     * AsyncTask to load profile images
     */
    private class loadImageTask extends AsyncTask<Void, Void, Bitmap>{
        ImageView iv;
        String uid;
        AUser user;

        public loadImageTask(ImageView iv, AUser user){
            this.iv = iv;
            this.uid = user.getUID();
        }

        @Override
        protected Bitmap doInBackground(Void... Params){
            return BH.getUserBitmap(uid);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            if(!uid.equals(iv.getTag())){ //check imageview tag to make sure this is the current image it wants
                return;
            }
            if(bitmap!=null){
                iv.setImageBitmap(bitmap);
                iv.setVisibility(View.VISIBLE);

            }
        }
    }


}

