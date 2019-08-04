package nikos.whatevervalue.com.thesis;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity{

    final private int REQUEST_CODE = 123;

    FirebaseDatabase database;
    DatabaseReference myRef;

    TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        txtResults = findViewById(R.id.txtResults);

        //Get usersID;
        final String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                txtResults.setText(dataSnapshot.getValue().toString() + "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkGooglePlayService();
    }

    //Check Google Play Services;
    private void checkGooglePlayService(){
        if (isGooglePlayServicesAvailable()){

            checkInternetConnection(null);
        }else {
            Toast.makeText(getApplicationContext(),"Google Play Services are not available.",Toast.LENGTH_LONG).show();
        }
    }

    //Check & Prompt Internet conenction;

    private Boolean checkInternetConnection(DialogInterface dialog){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()){
            promptInterntConnect();
            return false;
        }

        if (dialog != null){
            dialog.dismiss();
        }

        //If internet connection is active;
        if (checkPermissions()){
            //startService();
        }else {
            requestPermission();
        }
        return true;
    }

    //Show dialog to refresh internet connection;

    private void promptInterntConnect(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Internet Connection: ");
        builder.setMessage("Enable your Internet connection");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (checkInternetConnection(dialog)){
                    if (checkPermissions()){
                        //startService();
                    }else if (!checkPermissions()){
                        requestPermission();
                    }
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Return availability of GooglePlayServices;
    public boolean isGooglePlayServicesAvailable(){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS){
            if (googleApiAvailability.isUserResolvableError(status)){
                googleApiAvailability.getErrorDialog(this,status,2404).show();
            }
            return false;
        }
        return true;
    }

    //Return current state of permissions;

    private boolean checkPermissions(){
        int permissionState1 = ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionState2 = ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED;
    }

    //Ask for Permissions;
    private void requestPermission(){
        if (Build.VERSION.SDK_INT >=23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                                                                                    ,Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                //startService();
            }else{
                requestPermission();
            }
        }
    }

    //Start Service;
    private void startService(){
        startService(new Intent(this, MyService.class));
        Toast.makeText(this,"Service is enable", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {

        //Stop MyService;
        stopService(new Intent(this,MyService.class));
        super.onDestroy();
    }
}
