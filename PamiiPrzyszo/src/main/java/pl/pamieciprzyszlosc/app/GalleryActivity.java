package pl.pamieciprzyszlosc.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.*;

public class GalleryActivity extends Activity {

    private TextView debugText;
    private FTPClient ftpClient;
    private String fileNames = "";


// class that connect to ftp in background
    private class FTPBackgroundTask extends AsyncTask<Void,Void,String> {
        @Override
        protected String doInBackground(Void... voids) {
            String mFileNames = "";
            try {

                ftpClient = new FTPClient();
                ftpClient.connect("ftp.strefa.pl");

                ftpClient.login("admin+ftpforproject.strefa.pl","studia12");

                ftpClient.enterLocalPassiveMode();
                FTPFile[] fileList = ftpClient.listFiles();

                for(FTPFile file : fileList){
                    mFileNames+= file.getName() +"\n";
                }

            }catch (Exception e) {
                e.printStackTrace();
            }
            return mFileNames;
        }

        protected void onPostExecute(String result){
            fileNames = result;
            debugText.setText(fileNames);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gellery);
        // Show the Up button in the action bar.
        setupActionBar();
        debugText =  (TextView) findViewById(R.id.debug_text);
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
