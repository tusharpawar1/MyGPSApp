package inc.pawars.com.mygpsapp;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
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

import static inc.pawars.com.mygpsapp.GlobalVariables.*;

public class LocationCheckService extends IntentService implements LocationListener,OnMapReadyCallback, GeoTask.Geo {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public LocationCheckService(String name) {
        super(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

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
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + str_from + "&destinations=" + str_to + "&mode=driving&language=fr-FR&avoid=tolls&key=AIzaSyBnLYofF9CaNVJeYgr9GcBi4EFu8txpmAA";
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
    public void setDouble(String min) {

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
}
