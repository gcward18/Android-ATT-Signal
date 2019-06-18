package com.example.findmysignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db;      //Firestone Database instance
    Location userLocation;     //When updated, stores the location of the user
    int cellStrength;          //Stores cell signal strength from 0 - 4 where 0 is very poor and 4 is very strong
    int mbps;                   //Stores the speed of current connection in MBPS
    Map<String, Object> locationData = new HashMap<>();     //HashMap of all the local data that is pushed to Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupLayout();
        setupDatabase();        //Starts the database initialization
        getCellData();      //Gets information from the cellphone like strength, speed and network type
        getLocation();        //Gets information on where the user is located
    }

    @Override
    protected void onResume() {
        super.onResume();
        //loopData();
    }

    public void setupLayout(){

        Button cameraButton = findViewById(R.id.button);
        final Intent cameraIntent = new Intent(this,CameraActivity.class);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCellData();
                getLocation();            }
        });


    }

    public void loopData(){
        while(true){
            try {
                TimeUnit.SECONDS.sleep(10);
                getCellData();
                getLocation();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    public void setupDatabase() { db = FirebaseFirestore.getInstance(); }       //Sets db to the instance located in FireStone

    public void getCellData(){  //Collects cellphone data and places it into the CellStrength value

        TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            SignalStrength signalStrength = telephonyManager.getSignalStrength();
            cellStrength = signalStrength.getLevel() + 1;

        }

        else{ ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},99 ); }
    }

    private void getLocation(){     //Gets last known location for using Permission Manager and Location Manager classes

        //Setting up all managers and begins receiving location data
        PermissionManager permissionManager = new PermissionManager(this,this);
        final MyLocationManager myLocationManager = new MyLocationManager(this,this);
        myLocationManager.startLocationReceiving();
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Gets last known location of users phone
        if(permissionManager.checkLocationPermission()) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {

                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                myLocationManager.positionReceived(location);
                                userLocation = location;
                                writeToDatabase();
                            }
                        }
                    });

        }
    }

    public void writeToDatabase() {     //Writes all relevant information to the Firestone Database

        setWifiAndNetworkData();    //Gets information on if there is a network or wifi connection and its relevant speed
        putAllFoundData();          //Put in all found data (Location and Cell tower strength) into the HashMap
        List<Address> addressList = new List<Address>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(@Nullable Object o) {
                return false;
            }

            @NonNull
            @Override
            public Iterator<Address> iterator() {
                return null;
            }

            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @NonNull
            @Override
            public <T> T[] toArray(@NonNull T[] ts) {
                return null;
            }

            @Override
            public boolean add(Address address) {
                return false;
            }

            @Override
            public boolean remove(@Nullable Object o) {
                return false;
            }

            @Override
            public boolean containsAll(@NonNull Collection<?> collection) {
                return false;
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends Address> collection) {
                return false;
            }

            @Override
            public boolean addAll(int i, Collection<? extends Address> collection) {
                return false;
            }

            @Override
            public boolean removeAll(@NonNull Collection<?> collection) {
                return false;
            }

            @Override
            public boolean retainAll(@NonNull Collection<?> collection) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public Address get(int i) {
                return null;
            }

            @Override
            public Address set(int i, Address address) {
                return null;
            }

            @Override
            public void add(int i, Address address) {

            }

            @Override
            public Address remove(int i) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<Address> listIterator() {
                return null;
            }

            @Override
            public ListIterator<Address> listIterator(int i) {
                return null;
            }

            @Override
            public List<Address> subList(int i, int i1) {
                return null;
            }
        };

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
             addressList = geocoder.getFromLocation(userLocation.getLatitude(), userLocation.getLongitude(), 1);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        Log.i("Address", addressList.get(0).toString());
        // Add a new document with a generated ID
        db.collection("newCollection").document(addressList.get(0).getCountryName()).collection(addressList.get(0).getAdminArea()).document(addressList.get(0).getSubAdminArea()).collection(addressList.get(0).getLocality()).document(addressList.get(0).getPostalCode()).collection(addressList.get(0).getThoroughfare()).add(locationData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {    //Records Success
                        Log.d("Success ", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {                   //Records Failure
                        Log.w("Error", "Error adding document", e);
                    }
                });

    }

    public void setWifiAndNetworkData(){    //Check for WiFi and Cellular and record its speeds

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(!wifiManager.isWifiEnabled()){   //Check to see if wifi is enabled
            Toast.makeText(getApplicationContext(), "Please enbale your wifi",
                    Toast.LENGTH_SHORT).show();
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public void putAllFoundData(){      //Puts location and cell data into the HashMap
        locationData.put("Latitude", userLocation.getLatitude());
        locationData.put("Longitude", userLocation.getLongitude());
        locationData.put("Bars", cellStrength);
    }




}
