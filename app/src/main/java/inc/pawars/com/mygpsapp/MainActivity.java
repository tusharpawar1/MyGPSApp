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
import android.location.Geocoder;
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
import java.util.List;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.inmobi.ads.*;
import com.inmobi.sdk.*;

import static android.media.RingtoneManager.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {
    private LocationCheckService locService = null;
    boolean mBound = false;
    public static Handler messageHandler = new ActivityIncomingHandler();

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,    IBinder service) {
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
        bindService(intent,mConnection,Context.BIND_AUTO_CREATE);

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
                locService.requestGPSUpdate();

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
                locService.requestGPSUpdate();

            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonCancel){
                Log.d("Clicker", "onClick: on Cancel" );

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
                locService.removeUpdates();

                if (timer != null) timer.cancel();
                if (currentMarker!= null) currentMarker.remove();
                if (destinationMarker != null) destinationMarker.remove();
            }
            else if(v instanceof Button && ((Button)v).getId() == R.id.buttonSnooze){

                if(vibrator != null) vibrator.cancel();
                if (null != r ) r.stop();
                locService.removeUpdates();

            }

        } catch (Exception ex) {
            Log.d("Exception on Click", "onClick: "+ex);
            Toast.makeText(getApplicationContext(), "Exception" +ex, Toast.LENGTH_SHORT).show();


        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
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
