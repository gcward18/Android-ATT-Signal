package com.example.findmysignal;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/* Singleton class that tracks the users location */
public class MyLocationManager{

    private Context context = null;
    private Activity activity = null;
    private LocationManager mLocationManager = null;
    PermissionManager permissionManager = null;
    MainActivity mainActivity;
    FirebaseFirestore db;      //Firestone Database instance
    Location userLocation;     //When updated, stores the location of the user
    int cellStrength;          //Stores cell signal strength from 0 - 4 where 0 is very poor and 4 is very strong
    int mbps;                   //Stores the speed of current connection in MBPS
    Map<String, Object> locationData = new HashMap<>();     //HashMap of all the local data that is pushed to Firebase
    DecimalFormat decimalFormat = new DecimalFormat("#.####");
    MyLocationManager myLocationManager;



    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Location currentLocation;

    public Location getLocation() {
            return this.userLocation;
    }

    public void setLocation(Location location) {
        this.userLocation = location;
    }


    public MyLocationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionManager = new PermissionManager(context,activity);
        startLocationReceiving();


    }

    public void getMainActivity(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    private GPSLocationListener gpsLocationListener = new GPSLocationListener();
    private NetworkLocationListener networkLocationListener = new NetworkLocationListener();

    public void positionReceived(Location location){
        this.userLocation = location;
    }


    private class GPSLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            Log.i("Location", "position received: " + location);
            if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
                return;
            }
            positionReceived(location);
            setUpDataLoop();
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }


    }

    private class NetworkLocationListener implements LocationListener {

        public NetworkLocationListener() {
        }

        public void onLocationChanged(Location location) {
            Log.d("Location", "position received: " + location);
            if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
                return;
            }
            positionReceived(location);
            setUpDataLoop();
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    public void startLocationReceiving() {
        if(this.permissionManager.checkLocationPermission()) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100F, gpsLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 100F, networkLocationListener);
        }
    }

    public void stopLocationReceiving() {
        mLocationManager.removeUpdates(gpsLocationListener);
        mLocationManager.removeUpdates(networkLocationListener);
    }

    // Takes two set of coords and returns the amount of miles between those coords
    public int distFrom(double lat1, double lng1) {

            double lng2 = userLocation.getLongitude();
            double lat2 = userLocation.getLatitude();

            double earthRadius = 6371000; //meters
            double dLat = Math.toRadians(lat2 - lat1);
            double dLng = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLng / 2) * Math.sin(dLng / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            float dist = (float) (earthRadius * c);

            return (int) dist / 1609;       //Convert to miles


    }


    public void writeToDatabase() {     //Writes all relevant information to the Firestone Database

        locationData.clear();
        getCellData();
        setWifiAndNetworkData();    //Gets information on if there is a network or wifi connection and its relevant speed
        putAllFoundData();          //Put in all found data (Location and Cell tower strength) into the HashMap
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this.context, Locale.getDefault());
        try {
            addressList = geocoder.getFromLocation(userLocation.getLatitude(), userLocation.getLongitude(), 1);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        Log.i("Address", addressList.get(0).toString());
        // Add a new document with a generated ID
        if(!databaseReference.child("shortPath").child(addressList.get(0).getCountryName()).child(addressList.get(0)
                .getAdminArea()).child(addressList.get(0).getSubAdminArea()).child(addressList.get(0).getLocality())
                .child(addressList.get(0).getPostalCode()).child(addressList.get(0).getThoroughfare()).child( decimalFormat.format(userLocation.getLatitude())
                        .replace('.','_') +  decimalFormat.format(userLocation.getLongitude()).replace('.', '_')).equals( decimalFormat.format(userLocation.getLatitude())
                        .replace('.','_') +',' + decimalFormat.format(userLocation.getLongitude()).replace('.', '_'))){


            databaseReference.child("shortPath").child(addressList.get(0).getCountryName()).child(addressList.get(0).
                    getAdminArea()).child(addressList.get(0).getSubAdminArea()).child(addressList.get(0).getLocality()).
                    child(addressList.get(0).getPostalCode()).child(addressList.get(0).getThoroughfare()).child( decimalFormat.format(userLocation.getLatitude()).
                    replace('.','_') + ',' + decimalFormat.format(userLocation.getLongitude()).replace('.', '_')).setValue(locationData)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("What Happened", e.toString());
                        }
                    });

            Log.i("Accepted", "Adding");
            Log.i("Added", decimalFormat.format(userLocation.getLatitude()).
                    replace('.',' ') + decimalFormat.format(userLocation.getLongitude()).replace('.', ' '));
        }

        else{
            Toast.makeText(this.context,"Location Already Exists!",Toast.LENGTH_LONG);
            Log.i("Rejected", "Adding");
        }


    }
    public void setUpDataLoop(){
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                writeToDatabase();
            }
        }, 0, 5000);
    }

    public void setWifiAndNetworkData(){    //Check for WiFi and Cellular and record its speeds

        WifiManager wifiManager = (WifiManager) this.context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(!wifiManager.isWifiEnabled()){   //Check to see if wifi is enabled
            Toast.makeText(this.context.getApplicationContext(), "Please enbale your wifi",
                    Toast.LENGTH_SHORT).show();
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ConnectivityManager connectivityManager = (ConnectivityManager) this.context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

        if(networkCapabilities != null){    //If there is a network connected
            if(wifiManager.isWifiEnabled()){   //If there is a wifi, it will always be used over data so record it
                mbps = wifiInfo.getLinkSpeed();
                locationData.put("Network Speed", -1);      //Network may be enabled however it is not being recorded to we display a -1
                locationData.put("WiFi Speed", mbps);
            }
            else {  //Else if it is still on a connection with data, record that
                mbps = networkCapabilities.getLinkDownstreamBandwidthKbps() / 1000;
                locationData.put("Network Speed", mbps);
                locationData.put("WiFi Speed", -1);     //From the previous if, we know that WFIF isnt enabled so set to -1
            }
        }

    }

    public void getCellData(){  //Collects cellphone data and places it into the CellStrength value

        TelephonyManager telephonyManager = (TelephonyManager)this.context.getSystemService(Context.TELEPHONY_SERVICE);

        if(ContextCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SignalStrength signalStrength = telephonyManager.getSignalStrength();
            cellStrength = signalStrength.getLevel() + 1;

        }
        else{ ActivityCompat.requestPermissions(this.activity, new String[]{Manifest.permission.READ_PHONE_STATE},99 ); }
    }

    public void putAllFoundData(){      //Puts location and cell data into the HashMap
        locationData.put("Latitude",Double.valueOf(decimalFormat.format(userLocation.getLatitude())));
        locationData.put("Longitude",Double.valueOf(decimalFormat.format(userLocation.getLongitude())));
        locationData.put("Bars", cellStrength);
    }

}
