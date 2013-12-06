package pl.pamieciprzyszlosc.app;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.app.ActionBar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Context;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.Fragment;

public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {


    private GoogleMap mMap;
    private LocationClient mLocationClient;
    private LatLng seekingLocation=null;
    private boolean gameBegins=true;
    private float previousDistance = 0;
    private PendingIntent proximityIntent;
    private boolean playing = false;
    double latitude;
    double longitude;
    private String ftpAddress;

    private TextView locationLabel;

    private LocationManager mLocationManager;
    private LocationListener mMyLocationListener;
    Resources res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        locationLabel = (TextView) findViewById(R.id.locationLabel);


        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mMyLocationListener = new MyLocationListener();
        setUpRequests();
        res = getResources();

        mLocationClient = new LocationClient(this, this, this);
        SharedPreferences settings;
        settings = getSharedPreferences("PREFS", Context.MODE_PRIVATE);

        ftpAddress = settings.getString("ftp_address",getString(R.string.ftp_address));
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ftp_address",ftpAddress);
        editor.commit();
        setUpMapIfNeeded();
    }
    private void setUpRequests(){
        mLocationManager.requestLocationUpdates(mLocationManager.GPS_PROVIDER, 150, (float) 25, mMyLocationListener);
    }


    private void addProximityAlert(double latitude, double longitude) {

        Intent intent = new Intent(getString(R.string.proximity_alert_intent));
        proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        mLocationManager.addProximityAlert(
                latitude,
                longitude,
                6,
                -1,
                proximityIntent
        );

        IntentFilter filter = new IntentFilter(getString(R.string.proximity_alert_intent));
        registerReceiver(new ProximityIntentReceiver(), filter);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == res.getInteger(R.integer.gallery_request_code)) {
            if (resultCode == Activity.RESULT_OK){
                latitude = data.getDoubleExtra(res.getString(R.string.extras_latitude),0.0);
                longitude = data.getDoubleExtra(res.getString(R.string.extras_longitude),0.0);
                gameBegins=true;
                playing = true;
                seekingLocation = new LatLng(latitude,longitude);
                addProximityAlert(latitude,longitude);
            }
        }

    }

    private void setUpMapIfNeeded() {

        if (mMap == null) {

            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.clear();
        if (mLocationClient.isConnected()) {
            Location location = mLocationClient.getLastLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Lokalizacja"));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        CharSequence text = "";


        switch (item.getItemId()) {
            case R.id.action_gallery:
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                this.startActivityForResult(intent, res.getInteger(R.integer.gallery_request_code));
                break;
            case R.id.action_about:
                text = "Info!";
                break;
            case R.id.action_settings:

                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(getString(R.string.ftp));
                alert.setMessage(getString(R.string.set_up_ftp));

                final EditText input = new EditText(this);
                input.setText(ftpAddress);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        ftpAddress = value;
                        SharedPreferences settings;
                        settings = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("ftp_address",ftpAddress);
                        editor.commit();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();


                break;
        }


        //Toast toast = Toast.makeText(context, text, duration);
        //toast.show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        setUpRequests();
        mLocationClient.connect();
        if(playing){
            addProximityAlert(latitude,longitude);
        }


    }

    @Override
    protected void onStop() {
        // Disconnect the client.
        if(playing){
            mLocationManager.removeProximityAlert(proximityIntent);
        }
        mLocationManager.removeUpdates(mMyLocationListener);
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Display the error code on failure
        Toast.makeText(this, getString(R.string.connection_fail) +
                connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }



    private class GetAddressTask extends AsyncTask<Location, Void, String> {
        Context mContext;
        Location loc;

        public GetAddressTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected void onPostExecute(String address) {
            // Display the current address map
            LatLng newLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            MarkerOptions options =new MarkerOptions().position(newLatLng).title(address);
            mMap.addMarker(options);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 16.0f));

        }

        @Override
        protected String doInBackground(Location... params) {
            Geocoder geocoder =
                    new Geocoder(mContext, Locale.getDefault());
            loc = params[0];

            List<Address> addressesList = null;
            try {
                addressesList = geocoder.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
            } catch (IOException e1) {
                return (getString(R.string.io_exception));
            } catch (IllegalArgumentException e2) {
                String errorString = getString(R.string.illegal_arguments) +
                        Double.toString(loc.getLatitude()) +
                        " , " +
                        Double.toString(loc.getLongitude()) +
                        getString(R.string.passed);
                e2.printStackTrace();
                return errorString;
            }
            if (addressesList != null && addressesList.size() > 0) {
                // Get the first address
                Address address = addressesList.get(0);
                String addressText = String.format(
                        getString(R.string.address_format),
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",
                        address.getLocality(),
                        address.getCountryName());
                return addressText;
            } else {
                return getString(R.string.no_address);
            }
        }
    }// AsyncTask class

    private String getBearing(Location location){
        /*
        double R = 6371;
        double dLat = Math.toRadians(seekingLocation.latitude-location.getLatitude());
        double dLon = Math.toRadians(seekingLocation.longitude-location.getLongitude());
        double lat1 = Math.toRadians(location.getLatitude());
        double lat2 = Math.toRadians(seekingLocation.latitude);
        double y = Math.sin(dLon)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) -
                Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));

        brng = (brng+360)%360;
        //*/
        Location loc = new Location("Test");
        loc.setLatitude(seekingLocation.latitude);
        loc.setLongitude(seekingLocation.longitude);
        float brng = location.bearingTo(loc);
        if(brng>45 &&brng<=135)
            return "E";
        else if(brng>135 && brng<=225)
            return "S";
        else if(brng>225 && brng<=315)
            return "W";
        else
            return "N";
    }

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if(playing){
                GetAddressTask task = new GetAddressTask(getApplicationContext());
                task.execute(location);
                String toView;
                float[] results = new float[3];
                Location.distanceBetween(location.getLatitude(),location.getLongitude(),seekingLocation.latitude,seekingLocation.longitude,results);
                float distance = results[0];

                toView = distance+ getString(R.string.left_to_finish);

                if(gameBegins){
                    gameBegins=false;
                    previousDistance = distance;
                }else{
                    if(previousDistance>distance){
                        locationLabel.setBackgroundColor(Color.GREEN);
                    }
                    else
                        locationLabel.setBackgroundColor(Color.RED);
                }
                String currentDirection = getBearing(location);
                toView+="\n" + getString(R.string.your_direction)+currentDirection;
                locationLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                locationLabel.setText(toView);
            }

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            //dummy function unused so far
        }

        @Override
        public void onProviderEnabled(String s) {
            Toast.makeText(getApplicationContext(), getString(R.string.provider_enabled), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String s) {
            Toast.makeText(getApplicationContext(), getString(R.string.provider_disabled), Toast.LENGTH_SHORT).show();
        }
    }//location listener class


    private class ProximityIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String key = LocationManager.KEY_PROXIMITY_ENTERING;

            Boolean entering = intent.getBooleanExtra(key, false);

            if (entering){
                locationLabel.setBackgroundColor(Color.GREEN);
                locationLabel.setText(getString(R.string.win));
                playing = false;
                mLocationManager.removeProximityAlert(proximityIntent);
                unregisterReceiver(this);


            }
        }
    }//Broadcast receiver class

}
