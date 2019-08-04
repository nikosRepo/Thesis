package nikos.whatevervalue.com.thesis;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MyService extends Service implements GoogleApiClient.ConnectionCallbacks,
                                                  GoogleApiClient.OnConnectionFailedListener,
                                                  LocationListener,
                                                  SensorEventListener{

    FirebaseDatabase database;
    DatabaseReference myRef;


    private static final String TAG = MyService.class.getSimpleName();
    GoogleApiClient mLocationClient;
    LocationRequest mLocationRequest = new LocationRequest();


    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x,last_y,last_z;
    private static final int SHAKE_THRESHOLD = 100;

    public static final long INTERVAL = 1000 * 30;
    public static final long FASTEST_INTERVAL = 1000 * 5;

    String id;
    String longitude, latitude, strTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Firebase Connection;
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        //Unique ID;
        id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //GPS Location;
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;

        mLocationRequest.setPriority(priority);
        mLocationClient.connect();

        //Accelerometer;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,senAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Location callbacks;
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            Log.d(TAG, "Error onConnected() Permission not granted");

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient,mLocationRequest,this);

        Log.d(TAG,"Connected to Google API");
    }

    @Override
    public void onDestroy() {
        mLocationClient.disconnect();
        sensorManager.unregisterListener(this);

        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null){

            longitude = String.valueOf(location.getLatitude());
            latitude = String.valueOf(location.getLongitude());
            String key  = getSystemTime();

            writeToDatabase(longitude, latitude, key);

        }
    }

    public void writeToDatabase(String lon, String lat, String key) {
        if (lat != null && lon !=null) {

            myRef.child(id).child("Location").child(key).child("Latitude").setValue(lat);
            myRef.child(id).child("Location").child(key).child("Longitude").setValue(lon);


        }
    }

    //Get Time;
    private String getSystemTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        strTime = format.format(calendar.getTime());
        return strTime;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Take accelerometer;
    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100){
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z -last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    String key  = getSystemTime();
                    myRef.child(id).child("Acceleration").child(key).child("X").setValue(x);
                    myRef.child(id).child("Acceleration").child(key).child("Y").setValue(y);
                    myRef.child(id).child("Acceleration").child(key).child("Z").setValue(z);

                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
