package inc.pawars.com.mygpsapp;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.RingtoneManager.TYPE_RINGTONE;
import static android.media.RingtoneManager.getDefaultUri;
import static inc.pawars.com.mygpsapp.GlobalVariables.*;

public class LocationCheckService extends IntentService implements LocationListener,OnMapReadyCallback, GeoTask.Geo {
    Messenger mActivityMessenger = null;

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        LocationCheckService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationCheckService.this;
        }
    }
    LocationCheckService(){

        super("LocationCheckService");
    }

    public LocationCheckService(String name) {
        super(name);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        mActivityMessenger = (Messenger) extras.get("ACTIVITY_HANDLE");
       Message ms = new Message(); ms.arg1 =100;
        try {
            mActivityMessenger.send(ms);
        } catch (RemoteException e) {
            e.printStackTrace();
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
        locationManager.removeUpdates(this);
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


        }
        String str_from = currentLocation.latitude+"," + currentLocation.longitude ;
        String str_to = finalDestination.latitude + ","+ finalDestination.longitude ;
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + str_from + "&destinations=" + str_to + "&mode=transit&traffic_model=best_guess&key=AIzaSyBnLYofF9CaNVJeYgr9GcBi4EFu8txpmAA";
        new GeoTask(main).execute(url);


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
    public void setDouble(String result) {
        String res[]=result.split(",");
        Double min=Double.parseDouble(res[0])/60;
        long lngMinutes = Math.round(min);
        currentDistMinutes = lngMinutes;
        int dist=Integer.parseInt(res[1])/1000;
        /*EditText edTxtLongitude = (EditText) findViewById(R.id.editTextLongitude);
        edTxtLongitude.setText(edTxtLongitude.getText().toString()+ ","+ lngMinutes );*/
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

    private void vibrateOnce( ){

        long pattern[]={10,250,150,350,100,500,100,350,200,200,150};

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(pattern,0, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    public void requestGPSUpdate(){
        if (ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(main, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this,null );
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,0,this);

        }catch(Exception ex){
            Log.d("AppLog", "run: "+ ex);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
    }

    private class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            String str = (String)msg.obj;
            Toast.makeText(getApplicationContext(),
                    "From Activity -> " + str, Toast.LENGTH_LONG).show();
            Message lMsg = new Message();
            lMsg.obj="Hello Activity";
            try {
                mActivityMessenger.send(lMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    public void removeUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        if(locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
