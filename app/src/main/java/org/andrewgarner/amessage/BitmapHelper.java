package org.andrewgarner.amessage;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Helper class to handle bitmaps for user profile pictures and message photos
 * Created by Andrew on 4/30/2015.
 */
public class BitmapHelper implements ServerHelper.ServerHelperCallback{
    Context mContext;
    private String TAG;
    private BitmapHelperCallback mCallback;

    /**
     * Class to help load and manage bitmaps
     * @param c context of the activity, used for loading bitmaps
     */
    public BitmapHelper(Context c){
        mContext = c;
        TAG = mContext.getString(R.string.TAG);
        mCallback = (BitmapHelperCallback) mContext;
    }
    public interface BitmapHelperCallback{
        void bitmapDownloadPicFinished();
    }

    /**
     * Callback for ServerHelper class when pictures are done loading and saving to device
     */
    @Override
    public void onDownloadPictureFinished(){
        mCallback.bitmapDownloadPicFinished();
    }

    /**
     * Callback for Serverhelper when a search is completed
     * @param list list of users returned from search
     */
    @Override
    public void onSearchFinished(List<AUser> list){}

    /**
     * returns bitmap of a particular user
     * @param UID UID of the user
     * @return profile picture bitmap
     */
    public Bitmap getUserBitmap(String UID){
        try {
            return circleBM(loadBitmap(UID));
        }catch(Exception e){
            return circleAccount();
        }
    }

    /**
     * Downloads all pictures needed for friends (unused and not recommended)
     */
    /*public void downloadAllPictures(){
        AUserHelper AU = new AUserHelper(mContext);
        try {
            List<AUser> userList = AU.selectAllUsersNoPictureList();
            ServerHelper SH = new ServerHelper(mContext);

            for (AUser user : userList) {
                Log.i(TAG, "BitmapHelper / Downloading Picture...");
                Log.v(TAG, "hi");

                SH.downloadUserPicture(user,true);
            }
            Log.i(TAG, "Main / End No Picture--");
        } catch (Exception e){
            Log.v(TAG, "BitmapH / No Photos to DL");
        }

    }*/

    /**
     * Downloads a user's picture and then sets it in an imageview, uses ServerHelper
     * @param user user to download profile picture
     * @param iv imageview to display picture
     * @param bar progress bar to display while loading
     * @param download whether or not to save the picture
     */
    public void downloadPictureSetIV(AUser user, ImageView iv, ProgressBar bar, boolean download){
        try{
            ServerHelper SH = new ServerHelper(mContext);
            SH.downloadUserPicture(user, iv, bar, download);

        }catch(Exception e){
            Log.e(TAG, "BitmapHelper / downloadPicSetIV / Something went wrong\n"+e);

        }
    }

    /**
     * Creates a circular bitmap for the default google account picture
     * @return google account default picture
     */
    public Bitmap circleAccount() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.gaccount);

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
        }

    /**
     * Creates a circular bitmap from a bitmap passed in
     * @param bitmap to turn into a circle
     * @return circularized (is that a word?) bitmap from the original
     */
    public Bitmap circleBM(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * Loads a bitmap from device storage
     * @param picName name of the picture stored in app files. Usually the UID of the user is the name
     * @return user account bitmap
     */
    private Bitmap loadBitmap(String picName){
        Bitmap b;
        FileInputStream fis;
        try {
            fis = mContext.openFileInput(picName);
            b = BitmapFactory.decodeStream(fis);
            fis.close();
            return b;

        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "BitmapHelper / file not found");
        }
        catch (IOException e) {
            Log.e(TAG, "BitmapHelper / io exception");
        }
        //If we don't have a profile picture, use the default google picture
        Resources r = mContext.getResources();
        //We want the picture to be 100dp, as that's what the others are loaded as
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
        return decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.gaccount, px, px);
    }


    /**
     * This function loads a resource drawable file with particular dimensions
     * @param res resources for app
     * @param resId id of the drawable
     * @param reqWidth width to make the drawable
     * @param reqHeight Height to make the drawable
     * @return bitmap from resources of particular dimensions
     */
    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Used for the decodeSampleBitmapFromResources() function, find sampling size for bitmap
     * @param options bitmapfactoy options
     * @param reqWidth required width of bitmap
     * @param reqHeight required height of bitmap
     * @return sampling size to use when loading bitmap
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
