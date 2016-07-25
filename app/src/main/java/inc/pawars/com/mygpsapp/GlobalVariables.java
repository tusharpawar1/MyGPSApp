package inc.pawars.com.mygpsapp;

import android.location.LocationManager;
import android.media.Ringtone;
import android.os.Handler;
import android.os.Vibrator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by TUSHAR on 7/20/2016.
 */
public  class GlobalVariables {

    public static double dblLat = 0;
    public static double dblLong = 0;
    public static LocationManager locationManager;
    public static String provider;
    public static Vibrator vibrator;
    public static Timer timer = null;
    public static TimerTask timerTask;
    public static final Handler handler = new Handler();
    public static LatLng searchLocation = null;
    public static LatLng finalDestination = null;
    public static MarkerOptions destinationMarkerOpts = null;
    public static Marker destinationMarker = null;
    public static Circle currentCircleMarker = null;
    public static Marker currentMarker = null;
    public static LatLng previousLocation = null;
    public static LatLng currentLocation = null;
    public static float currentDistMeters = 0;
    public static long currentDistMinutes = 0;
    public static long previousTimeStamp = 0;
    public static long currentTimeStamp = 0;
    public static MainActivity main = null;
    public static GoogleMap map = null;
    public static Ringtone r = null;

}
