package inc.pawars.com.mygpsapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LocationCheckService extends Service {
    public LocationCheckService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
