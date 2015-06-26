package org.andrewgarner.amessage;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying messages of a particular conversation
 * Created by Andrew on 4/20/2015.
 */
public class AMessageAdapter extends BaseAdapter {
    private Context context; //context from the parent fragment
    private LayoutInflater inflater;
    private List<AMessage> messageList; //list of all messages of a conversation
    private String TAG;

    public AMessageAdapter(Context c, List<AMessage> messageList) {
        this.messageList = messageList;
        context = c;
        TAG = context.getString(R.string.TAG);
        Log.e(TAG, "AMAdapter / new");
    }

    public void addMessage(AMessage msg){
        this.messageList.add(msg);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public Object getItem(int location) {
        return messageList.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    //Since messages load so quickly, I have opted out of using the viewholder,
    //But Have left the code in for it. Since messages in and out use separate XML files,
    //Using a viewholder doesn't work well. But if the two files are merged then it would work.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //ViewHolder holder;

        if (inflater == null)
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

       // if (convertView == null) {
        if (!messageList.get(position).isSelf()) {
            convertView = inflater.inflate(R.layout.fragment_conversation_in, parent, false);
        } else {
            convertView = inflater.inflate(R.layout.fragment_conversation_out, parent, false);
        }
        /*holder = new ViewHolder();

            holder.textTV = (TextView) convertView.findViewById(R.id.name);
            holder.subTextTV = (TextView) convertView.findViewById(R.id.subtext);
            convertView.setTag(holder);

        }else{
            holder = (ViewHolder) convertView.getTag();
        }*/
        AMessage msg = messageList.get(position);

        String text = msg.getText();
        text = text.replace("\\n", "\n");
        text = text.replace("\\'", "'");

        long timestamp = msg.getDate();
        if (timestamp < 14000000000L)
            timestamp *= 1000;
        Calendar calendurr = Calendar.getInstance(Locale.ENGLISH);
        calendurr.setTimeInMillis(timestamp);
        String date = DateFormat.format(context.getString(R.string.dateFormat), calendurr).toString();

        TextView textTV = (TextView) convertView.findViewById(R.id.name);
        TextView subTextTV = (TextView) convertView.findViewById(R.id.subtext);

        textTV.setText(text);
        subTextTV.setText(date);


        return convertView;
    }
    static class ViewHolder{
        TextView textTV;
        TextView subTextTV;
    }


}
