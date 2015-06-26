package org.andrewgarner.amessage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;


public class ProfileActivity extends ActionBarActivity implements BitmapHelper.BitmapHelperCallback, ServerHelper.ServerHelperCallback{
    AUserHelper AUH;
    BitmapHelper BH;
    ServerHelper SH;
    AUser user;
    boolean self=false;
    String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TAG = getString(R.string.TAG);
        AUH = new AUserHelper(this);
        BH = new BitmapHelper(this);
        SH = new ServerHelper(this);

        //user = AUH.selectUserBySelf();
        //self=true;
        String myUUID = AUH.getSelfID();
        String myUUID2 = AUH.getSelfIDFromDB();

        Log.wtf(TAG,"Profile: myUUID="+myUUID);
        Log.wtf(TAG,"Profile: myUUID2="+myUUID2);


        if(getIntent()!=null){
            user = getIntent().getParcelableExtra(getString(R.string.bundle_profile_user));
            if(user==null){
                user = AUH.selectUserBySelf();
                self=true;
            }else{
                self=false;
            }
        }else{
            user = AUH.selectUserBySelf();
            self=true;
        }
        Log.wtf(TAG,"Profile: "+user.getEmail());
        user.showData();
        Log.wtf(TAG,"Profile: "+AUH.getSelfID());

        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
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

    private void setup(){

        getSupportActionBar().setElevation(0);

        setTitle(user.getName());

        TextView nameTV = (TextView) findViewById(R.id.nameTV);
        TextView emailTV = (TextView) findViewById(R.id.emailTV);

        nameTV.setText(user.getName());
        emailTV.setText(user.getEmail());

        ImageView picture = (ImageView) findViewById(R.id.profileImageView);
        picture.setTag(user.getUID());
        ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar2);
        BH.downloadPictureSetIV(user, picture, null, true);

        if(user.photoNeedsDownload()){
            bar.setVisibility(View.VISIBLE);

            BH.downloadPictureSetIV(user, picture, bar, true);
        } else {
            new loadImageTask(picture, user, bar).execute();
        }

        Button logoutButton = (Button) findViewById(R.id.logoutButton);

        if(self){
            logoutButton.setText("Logout");
        }else if(AUH.isFriend(user)){
            logoutButton.setText("Unfriend");
        } else {
            logoutButton.setVisibility(View.GONE);
            return;
        }

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPress();
            }
        });


    }

    private void buttonPress(){
        if(self){
            openLogout();
        }else{
            openUnfriend(user);
        }

    }
    private void logout(){
        Log.wtf(TAG, "Profile / SIGNOUT!");
        Bundle conData = new Bundle();
        conData.putBoolean(getString(R.string.bundle_profile_signout), true);
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finish();
    }
    private void unfriend(AUser user){
        Log.wtf(TAG, "Profile / SIGNOUT!");
        AUH.setUserFriendStatus(user,false);

        Bundle conData = new Bundle();
        conData.putBoolean(getString(R.string.bundle_profile_unfriend), true);
        conData.putParcelable(getString(R.string.bundle_profile_unfriend_user),user);
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finish();
    }



    private class loadImageTask extends AsyncTask<Void, Void, Bitmap> {
        ImageView iv;
        String uid;
        AUser user;
        ProgressBar bar;

        public loadImageTask(ImageView iv, AUser user,ProgressBar bar){
            this.iv = iv;
            this.uid = user.getUID();
            this.bar=bar;
        }

        @Override
        protected Bitmap doInBackground(Void... Params){
            return BH.getUserBitmap(uid);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            bar.setVisibility(View.GONE);
            if(bitmap==null)
                bitmap = BH.circleAccount();
            iv.setImageBitmap(bitmap);
            iv.setVisibility(View.VISIBLE);
        }
    }

    public void openUnfriend(final AUser user){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unfriend "+user.getName()+"?")
                .setPositiveButton("Unfriend", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        unfriend(user);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
    public void openLogout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout of TouchBase?")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }


    @Override
    public void bitmapDownloadPicFinished(){

    }
    @Override
    public void onDownloadPictureFinished(){

    }
    @Override
    public void onSearchFinished(List<AUser> list){}
}
