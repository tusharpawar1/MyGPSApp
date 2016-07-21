package inc.pawars.com.mygpsapp;
import static inc.pawars.com.mygpsapp.GlobalVariables.*;
import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.inmobi.ads.*;
import com.inmobi.sdk.*;

import static android.media.RingtoneManager.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener,OnMapReadyCallback, GeoTask.Geo {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InMobiSdk.init(this, "b13dfbdf78a34cb985bd7fc6a9fe222d"); //'this' is used specify context
        InMobiBanner banner = (InMobiBanner)findViewById(R.id.banner);
        banner.setRefreshInterval(30);
        banner.setEnableAutoRefresh(true);
        banner.load();
        findViewById(R.id.buttonSearch).setOnClickListener(this);
        findViewById(R.id.buttonGPS).setOnClickListener(this);
        findViewById(R.id.buttonConfirm).setOnClickListener(this);
        findViewById(R.id.buttonCancel).setOnClickListener(this);
        findViewById(R.id.buttonSnooze).setOnClickListener(this);
        Toast.makeText(getApplicationContext(), R.string.Welcome, Toast.LENGTH_SHORT).show();
        main = this;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getApplicationContext(), R.string.GPSPermissionNotAvail, Toast.LENGTH_SHORT).show();
            return;
        }

        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(finalDestination == null){
                            return;
                        }
                        requestGPSUpdate();
                    }
                });

            }
        };

        timer.schedule(timerTask,60000,60000);
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
                searchLocation = new LatLng(dblLat,dblLong);

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(searchLocation,16);
                map.animateCamera(update);
                if(destinationMarker != null) destinationMarker.remove();
                destinationMarkerOpts = new MarkerOptions().position(searchLocation);
                destinationMarker = map.addMarker(destinationMarkerOpts);

            }
            else if (v instanceof Button && ((Button) v).getId() == R.id.buttonConfirm) {

                if(dblLat == 0 && dblLong == 0){
                    Toast.makeText(getApplicationContext(), R.string.DestinationNotEntered, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), R.string.SleepMessage, Toast.LENGTH_SHORT).show();
                finalDestination = new LatLng(dblLat,dblLong);
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

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
                locationManager.removeUpdates(main);
                if (timer != null) timer.cancel();
                if (currentMarker!= null) currentMarker.remove();
                if (destinationMarker != null) destinationMarker.remove();
            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonSnooze){

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
                locationManager.removeUpdates(main);
            }

        } catch (Exception ex) {
            Log.d("Exception on Click", "onClick: "+ex);
            Toast.makeText(getApplicationContext(), "Exception" +ex, Toast.LENGTH_SHORT).show();


        }
    }
    private void vibrateOnce( ){

        long pattern[]={10,250,150,350,100,500,100,350,200,200,150};

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(pattern,0, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());

    }
    private void requestGPSUpdate(){
        if (ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, main,null );
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,0,this);

        }catch(Exception ex){
            Log.d("AppLog", "run: "+ ex);
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        if(finalDestination == null ){
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.removeUpdates(main);
        if(currentLocation != null){
            previousLocation = currentLocation;
            previousTimeStamp = currentTimeStamp;
        }
        currentTimeStamp = System.currentTimeMillis();

        currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
        float[] results = new float[1];
        Location.distanceBetween(finalDestination.latitude, finalDestination.longitude,
                currentLocation.latitude, currentLocation.longitude, results);
        currentDistMeters = results[0];
        if(currentMarker != null){
            currentMarker.remove();
        }

        currentMarker = map.addMarker(new MarkerOptions().position(currentLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        if(searchLocation != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(searchLocation);
            builder.include(currentLocation);
            LatLngBounds bounds = builder.build();
            int mapPadding = 150;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,mapPadding);
            map.animateCamera(cu);


        }
//        double time = Math.round(results[0]/400);
//        Toast.makeText(getApplicationContext(), getString(R.string.YouAreApprx) +time+ getString(R.string.MinAway), Toast.LENGTH_SHORT).show();
        if(null != previousLocation && null != currentLocation &&
                !previousLocation.equals(currentLocation) && previousTimeStamp != 0){
            Location.distanceBetween(previousLocation.latitude, previousLocation.longitude,
                    currentLocation.latitude, currentLocation.longitude, results);
            double timeTraveled = currentTimeStamp - previousTimeStamp;
            timeTraveled = timeTraveled/(3600);
            double velo = results[0]/(timeTraveled);
            DecimalFormat df = new DecimalFormat("###.####");
            Toast.makeText(getApplicationContext(), getString(R.string.Speed1) +df.format(velo)+ getString(R.string.Speed2), Toast.LENGTH_SHORT).show();
            EditText edTxtLatitude = (EditText) findViewById(R.id.editTextLatitude);
            edTxtLatitude.setText(edTxtLatitude.getText().toString()+ ","+ df.format(velo));

        }
        String str_from = currentLocation.latitude+"," + currentLocation.longitude ;
        String str_to = finalDestination.latitude + ","+ finalDestination.longitude ;
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + str_from + "&destinations=" + str_to + "&mode=driving&language=fr-FR&avoid=tolls&key=AIzaSyBnLYofF9CaNVJeYgr9GcBi4EFu8txpmAA";
        new GeoTask(MainActivity.this).execute(url);


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
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void setDouble(String result) {
        String res[]=result.split(",");
        Double min=Double.parseDouble(res[0])/60;
        long lngMinutes = Math.round(min);
        currentDistMinutes = lngMinutes;
        int dist=Integer.parseInt(res[1])/1000;
        EditText edTxtLongitude = (EditText) findViewById(R.id.editTextLongitude);
        edTxtLongitude.setText(edTxtLongitude.getText().toString()+ ","+ lngMinutes );
        Toast.makeText(getApplicationContext(), getString(R.string.YouAreApprx)  + lngMinutes  +  getString(R.string.MinAway), Toast.LENGTH_SHORT).show();

        if(lngMinutes < 6 || currentDistMeters < 1000){
            vibrateOnce();
            playSound();
            sendNotification();
        }
    }
    private void sendNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_wmu_notif)
                        .setContentTitle("Wake Me Up Notification!")
                        .setContentText("You are almost at your destination ")
                        .setAutoCancel(true)
                        .setTicker("This is Ticker")
                        .setWhen(System.currentTimeMillis())
                        .setColor(Color.YELLOW)
                        .setLights(Color.CYAN,300,200);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(resultIntent);
        /*PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );*/
        PendingIntent pdgIntent = PendingIntent.getActivity(this,0,resultIntent,PendingIntent.FLAG_NO_CREATE);
        mBuilder.setContentIntent(pdgIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(10, mBuilder.build());
    }


    private void playSound() {
        Uri notif = getDefaultUri(TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notif);
        r.play();

    }


}
