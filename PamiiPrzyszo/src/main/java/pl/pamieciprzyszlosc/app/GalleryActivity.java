package pl.pamieciprzyszlosc.app;

import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import com.google.common.io.ByteStreams;


import org.apache.commons.net.ftp.*;

public class GalleryActivity extends Activity {

    private FTPClient ftpClient;
    private String fileNames = "";
    private ImageView diplayImage;
    private LinearLayout myGallery;
    private TextView textView;




// class that connect to ftp in background
    private class FTPBackgroundTask extends AsyncTask<Void,Void,Bitmap[]> {


    @Override
        protected Bitmap[] doInBackground(Void... voids) {
            ArrayList<Bitmap> bitmapVector = new ArrayList<Bitmap>();
            //android.os.Debug.waitForDebugger();
            try {


                ftpClient = new FTPClient();
                ftpClient.connect("ftp.strefa.pl");

                ftpClient.login("admin+ftpforproject.strefa.pl","studia12");

                ftpClient.enterLocalPassiveMode();
                FTPFile[] fileList = ftpClient.listFiles();

                for(FTPFile file : fileList){
                    String fileName =file.getName();
                    if (!fileName.endsWith("jpg"))
                        continue;
                    FileOutputStream fileOutput = openFileOutput(fileName,MODE_PRIVATE);
                    InputStream inputStream = ftpClient.retrieveFileStream(fileName);
                    byte[] bytesArray = new byte[4096];
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(bytesArray)) != -1){
                        fileOutput.write(bytesArray,0,bytesRead);

                    }
                    boolean success = ftpClient.completePendingCommand();
                    FileInputStream inFile = openFileInput (fileName);
                    bytesRead = -1;
                    //bytesArray = (ByteStreams.toByteArray(inFile));
                    final Bitmap bitmap = BitmapFactory.decodeStream(inFile);
                    bitmapVector.add(bitmap);



                }

            }catch (Exception e) {
                e.printStackTrace();
            }
            return bitmapVector.toArray(new Bitmap[bitmapVector.size()]);
        }

        protected void onPostExecute(Bitmap[] result){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            for (Bitmap bitmap : result){
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(width/4, width/4));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
                imageView.getDrawingCache();
                imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageView imageView1 = (ImageView) view;
                    Drawable drawable = imageView1.getDrawable();
                    BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
                    Bitmap bitmap = bitmapDrawable .getBitmap();
                    diplayImage.setImageBitmap(bitmap);

                }
            });

            myGallery.addView(imageView);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gellery);
        // Show the Up button in the action bar.
        setupActionBar();
        //debugText =  (TextView) findViewById(R.id.debug_text);
        diplayImage = (ImageView) findViewById(R.id.displayImage);
        myGallery = (LinearLayout) findViewById(R.id.mygallery);
        textView = (TextView) findViewById(R.id.show_files);
        FTPBackgroundTask backgroundTask = new FTPBackgroundTask();
        backgroundTask.execute();








    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
