package inc.pawars.com.mygpsapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.Map;
import com.inmobi.ads.*;
import com.inmobi.sdk.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener,OnMapReadyCallback {
    private double dblLat = 0;
    private double dblLong = 0;
    private LocationManager locationManager;
    private String provider;
    private Vibrator vibrator;
    Timer timer = null;
    TimerTask timerTask;
    final Handler handler = new Handler();
    private LatLng Search_location = null;
    private LatLng Final_Destination = null;
    private MarkerOptions destinationMarkerOpts = null;
    private Marker destinationMarker = null;
    private Circle currentCircleMarker = null;
    private Marker currentMarker = null;
    private LatLng previous_location = null;
    private LatLng current_location = null;

    private long previousTimeStamp = 0;
    private long currentTimeStamp = 0;
    private MainActivity main;
    private GoogleMap map;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InMobiSdk.init(this, "b13dfbdf78a34cb985bd7fc6a9fe222d"); //'this' is used specify context
        InMobiBanner banner = (InMobiBanner)findViewById(R.id.banner);
        banner.setRefreshInterval(20);
        banner.setEnableAutoRefresh(true);
        banner.load();
        findViewById(R.id.buttonSearch).setOnClickListener(this);
        findViewById(R.id.buttonGPS).setOnClickListener(this);
        findViewById(R.id.buttonConfirm).setOnClickListener(this);
        findViewById(R.id.buttonCancel).setOnClickListener(this);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getApplicationContext(), R.string.GPSPermissionNotAvail, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), R.string.Welcome, Toast.LENGTH_SHORT).show();

        main = this;

        timer = new Timer();

/*        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(Final_Destination == null){
                            return;
                        }
                        requestGPSUpdate();
                    }
                });

            }
        };*/

//        timer.schedule(timerTask,60000,60000);
        ((MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment)).getMapAsync(this);


    }


    @Override
    public void onClick(View v) {
        try {
            if (v instanceof Button && ((Button) v).getId() == R.id.buttonSearch) {

                EditText edTxt = (EditText) findViewById(R.id.editTextAddress);
                String textEntered = edTxt.getText().toString();
                if(textEntered == null || "".equals(textEntered)){
                    Toast.makeText(getApplicationContext(), R.string.AddrNotEntered, Toast.LENGTH_SHORT).show();
                    return;

                }
                int duration = Toast.LENGTH_LONG;

                View viewKeyB = this.getCurrentFocus();
                if(viewKeyB != null){
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(viewKeyB.getWindowToken(),0);
                }
                Geocoder gc = new Geocoder(getApplicationContext());

                List<Address> lst = gc.getFromLocationName(edTxt.getText().toString(), 1);
                if(lst.size() <=0 ){
                    Toast.makeText(getApplicationContext(), R.string.AddrNotFpund, Toast.LENGTH_SHORT).show();
                    return;
                }
                Address adr = lst.get(0);
                dblLat = adr.getLatitude();
                dblLong = adr.getLongitude();
                Search_location = new LatLng(dblLat,dblLong);

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(Search_location,16);
                map.animateCamera(update);
                if(destinationMarker != null) destinationMarker.remove();
                destinationMarkerOpts = new MarkerOptions().position(Search_location);
                destinationMarker = map.addMarker(destinationMarkerOpts);

            }
            else if (v instanceof Button && ((Button) v).getId() == R.id.buttonConfirm) {

                if(dblLat == 0 && dblLong == 0){
                    Toast.makeText(getApplicationContext(), R.string.DestinationNotEntered, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), R.string.SleepMessage, Toast.LENGTH_SHORT).show();
                Final_Destination = new LatLng(dblLat,dblLong);
                requestGPSUpdate();


            }
            else if (v instanceof Button && ((Button) v).getId() == R.id.buttonGPS) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Toast t = Toast.makeText(getApplicationContext(), R.string.GPSPermissionNotAvail, Toast.LENGTH_SHORT);
                    t.show();
                    return;
                }
                Toast t = Toast.makeText(getApplicationContext(), R.string.RequestedGPSLoc, Toast.LENGTH_SHORT);
                t.show();

                requestGPSUpdate();

            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonCancel){
                Log.d("Clicker", "onClick: on Cancel" );
                vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.cancel();
//                locationManager.removeUpdates(main);

                if (currentMarker!= null) currentMarker.remove();
                if(destinationMarker != null) destinationMarker.remove();
            }


        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Exception" +ex, Toast.LENGTH_SHORT).show();


        }
    }
    private void vibrateOnce( ){

        long pattern[]={10,250,150,350,100,500,100,350,200,200,150};

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
//        vibrator.vibrate(pattern,-1);

        vibrator.vibrate(pattern,0, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());

    }
    private void requestGPSUpdate(){
        if (ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        try {
//            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, main,null );
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,0,this);

        }catch(Exception ex){
            Log.d("AppLog", "run: "+ ex);
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        if(Final_Destination == null ){
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.removeUpdates(main);
        if(current_location != null){
            previous_location = current_location;
            previousTimeStamp = currentTimeStamp;
        }
        currentTimeStamp = System.currentTimeMillis();

        current_location = new LatLng(location.getLatitude(),location.getLongitude());
        float[] results = new float[1];
        Location.distanceBetween(Final_Destination.latitude, Final_Destination.longitude,
                current_location.latitude, current_location.longitude, results);
        if(results[0] <500) {
            vibrateOnce();
        }
        if(currentMarker != null){
            currentMarker.remove();
        }

        currentMarker = map.addMarker(new MarkerOptions().position(current_location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        if(Search_location != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(Search_location);
            builder.include(current_location);
            LatLngBounds bounds = builder.build();
            int mapPadding = 150;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,mapPadding);
            map.animateCamera(cu);


        }
        double time = Math.round(results[0]/400);
        Toast.makeText(getApplicationContext(), getString(R.string.YouAreApprx) +time+ getString(R.string.MinAway), Toast.LENGTH_SHORT).show();
        if(null != previous_location && null != current_location &&
                !previous_location.equals(current_location) && previousTimeStamp != 0){
            Location.distanceBetween(previous_location.latitude, previous_location.longitude,
                    current_location.latitude, current_location.longitude, results);
            double timeTraveled = currentTimeStamp - previousTimeStamp;
            timeTraveled = timeTraveled/(3600);
            double velo = results[0]/(timeTraveled);
            DecimalFormat df = new DecimalFormat("###.####");
            Toast.makeText(getApplicationContext(), getString(R.string.Speed1) +df.format(velo)+ getString(R.string.Speed2), Toast.LENGTH_SHORT).show();
            EditText edTxtLatitude = (EditText) findViewById(R.id.editTextLatitude);
            EditText edTxtLongitude = (EditText) findViewById(R.id.editTextLongitude);

            edTxtLatitude.setText(edTxtLatitude.getText().toString()+ ","+ df.format(velo));
            edTxtLongitude.setText(edTxtLongitude.getText().toString()+ ","+ time);
        }
        AsyncTask at = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                return null;
            }
        };

//        vibrateOnce();

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }
}
