package pl.pamieciprzyszlosc.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Debug;
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
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.io.ByteStreams;


import org.apache.commons.net.ftp.*;

public class GalleryActivity extends Activity {

    HashMap<String, LatLng> coordinates = new HashMap<String, LatLng>();
    HashMap<String, Bitmap> bitmaps = new HashMap<String, Bitmap>();
    HashMap<Integer, LatLng> locationsData = new HashMap<Integer, LatLng>();
    int selectedID = -1;
    private FTPClient ftpClient;
    private ImageView diplayImage;
    private LinearLayout myGallery;
    private Resources res;
    private String ftpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gellery);
        // Show the Up button in the action bar.
        setupActionBar();
        //debugText =  (TextView) findViewById(R.id.debug_text);
        diplayImage = (ImageView) findViewById(R.id.displayImage);
        myGallery = (LinearLayout) findViewById(R.id.mygallery);
        res = getResources();
        FTPBackgroundTask backgroundTask = new FTPBackgroundTask();
        diplayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedID != -1) {
                    Intent resultIntent = new Intent();
                    LatLng coordinate = locationsData.get(selectedID);
                    resultIntent.putExtra(res.getString(R.string.extras_latitude), coordinate.latitude);
                    resultIntent.putExtra(res.getString(R.string.extras_longitude), coordinate.longitude);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }

            }
        });
        SharedPreferences settings;
        settings = getSharedPreferences("PREFS", Context.MODE_PRIVATE);

        ftpAddress = settings.getString("ftp_address",getString(R.string.ftp_address));


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
        //getMenuInflater().inflate(R.menu.main, menu);
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

                Intent resultIntent = new Intent();

                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // class that connect to ftp in background
    private class FTPBackgroundTask extends AsyncTask<Void, Integer, Void> {
        ProgressDialog barProgressDialog = new ProgressDialog(GalleryActivity.this);
        int numberOfFiles=1;
        int downloadedFiles=0;
        int max=100;
        @Override
        protected void onPreExecute() {

            barProgressDialog.setTitle(getString(R.string.download_in_progress));
            barProgressDialog.setMessage(getString(R.string.connecting));
            barProgressDialog.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
            barProgressDialog.setProgress(0);
            barProgressDialog.setMax(max);
            barProgressDialog.setCanceledOnTouchOutside(false);
            barProgressDialog.setCancelable(false);
            barProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            if(values[0].intValue()!=1){
                barProgressDialog.setMessage(getString(R.string.downloading));
            }
            else{
                downloadedFiles++;
                barProgressDialog.setProgress((max/numberOfFiles)*downloadedFiles);


            }
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... voids) {



           // android.os.Debug.waitForDebugger();
            try {
                //creating ftp client

                ftpClient = new FTPClient();
                ftpClient.connect(ftpAddress);

                ftpClient.login(getString(R.string.ftp_login), getString(R.string.ftp_password));

                ftpClient.enterLocalPassiveMode();
                FTPFile[] fileList = ftpClient.listFiles();
                numberOfFiles = fileList.length;
                publishProgress(0);
                //iterating file by file on server
                for (FTPFile file : fileList) {
                    String fileName = file.getName();

                    if (fileName.endsWith("jpg") || fileName.endsWith("txt")) {
                        //if file is jpg or txt file we read its data
                        FileOutputStream fileOutput = openFileOutput(fileName, MODE_PRIVATE);
                        InputStream inputStream = ftpClient.retrieveFileStream(fileName);
                        byte[] bytesArray = new byte[4096];
                        int bytesRead = -1;
                        while ((bytesRead = inputStream.read(bytesArray)) != -1) {
                            fileOutput.write(bytesArray, 0, bytesRead);

                        }
                        boolean success = ftpClient.completePendingCommand();
                        publishProgress(1);
                        FileInputStream inFile = openFileInput(fileName);
                        //if it is file wiht coordinates we are saving it to Hashmap of coordinates
                        if (fileName.equals(getString(R.string.coordinates_file))) {
                            byte readedBytes[] = (ByteStreams.toByteArray(inFile));
                            String readedFile = new String(readedBytes);
                            String[] coord = readedFile.split("\n");
                            for (String data : coord) {
                                String row[] = data.split(" ");
                                coordinates.put(row[0], new LatLng(Double.parseDouble(row[1]), Double.parseDouble(row[2])));
                            }
                            continue;

                        }

                        Bitmap bitmap = BitmapFactory.decodeStream(inFile);
                        bitmaps.put(fileName, bitmap);
                    }

                }
                ftpClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //android.os.Debug.waitForDebugger();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            barProgressDialog.setMessage(getString(R.string.finish));
            super.onPostExecute(result);
           // Debug.waitForDebugger();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            int width;

            if (android.os.Build.VERSION.SDK_INT > 12) {
                display.getSize(size);
                width = size.x;
            } else {
                width = display.getWidth();
            }

            Set<String> bitmapNames = coordinates.keySet();
            int ID = 0;
            for (String bitmapName : bitmapNames) {
                LatLng coordinate = coordinates.get(bitmapName);
                Bitmap bitmap = bitmaps.get(bitmapName);
                ImageView imageView = new ImageView(getApplicationContext());
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(width / 4, width / 4);
                imageView.setLayoutParams(params);
                imageView.setId(ID);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(bitmap);
                imageView.getDrawingCache();
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImageView imageView1 = (ImageView) view;
                        Drawable drawable = imageView1.getDrawable();
                        BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
                        Bitmap bitmap = bitmapDrawable.getBitmap();
                        selectedID = imageView1.getId();
                        diplayImage.setImageBitmap(bitmap);

                    }
                });
                locationsData.put(ID, coordinate);
                ImageView temp =  new ImageView(getApplicationContext());
                temp.setLayoutParams(new ViewGroup.LayoutParams(10, width / 4));
                myGallery.addView(imageView);
                myGallery.addView(temp);
                ID++;


            }
            bitmaps.clear();
            coordinates.clear();
            barProgressDialog.dismiss();

        }
    }

}
