package inc.pawars.com.mygpsapp;
import static inc.pawars.com.mygpsapp.GlobalVariables.*;
import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GeoTask.Geo {
    private LocationCheckService locService = null;
    boolean mBound = false;
    public static Handler messageHandler = new ActivityIncomingHandler();

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationCheckService.LocalBinder binder = (LocationCheckService.LocalBinder) service;
            locService= binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
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
        ((MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment)).getMapAsync(this);
        Intent intent = new Intent(this, LocationCheckService.class);
        intent.putExtra("ACTIVITY_HANDLE",new Messenger(messageHandler));
        startService(intent);


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
                /*Intent intentService = new Intent(this,LocationCheckService.class);
                Messenger actMessenger = new Messenger(new ActivityIncomingHandler());
                intentService.putExtra("ACTIVITY_HANDLE",actMessenger);
                startService(intentService);*/
                locService.requestGPSUpdate();
//                requestGPSUpdate();



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

//                requestGPSUpdate();

            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonCancel){
                Log.d("Clicker", "onClick: on Cancel" );

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
//                locationManager.removeUpdates(main);
//                locService.
                if (timer != null) timer.cancel();
                if (currentMarker!= null) currentMarker.remove();
                if (destinationMarker != null) destinationMarker.remove();
            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonSnooze){

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
//                locationManager.removeUpdates(LocationCheckService.class);

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
    public static class ActivityIncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            String str = (String)msg.obj;
            /*Toast.makeText(this,
                    "From Service -> " + str, Toast.LENGTH_LONG).show();*/
        }
    }

}
