package com.example.findmysignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.database.*;
import com.google.firebase.firestore.SnapshotMetadata;
import com.google.firebase.firestore.model.Document;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.lang.reflect.Array;
import java.security.Provider;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FirebaseFirestore db;
    List<Address> addressList;
    List<LatLng> locationLatLang;
    Location userLocation;
    double [] distances = new double [1000];
    double [] listLat = new double [1000];
    double [] listLon = new double [1000];
    MyLocationManager myLocationManager;
    int cellStrength;
    DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLocationInfo();
        myLocationManager = new MyLocationManager(this,this);
        myLocationManager.startLocationReceiving();
        userLocation = myLocationManager.getLocation();

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupDatabase();

        setupMaps();
        getData();

    }

    @Override
    protected void onResume() {
        super.onResume();



    }

    public void setupMaps(){
        if(mMap != null){
            if(checkSelfPermission(PermissionManager.LOCATION_SERVICE) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

            }
            else{
                requestPermissions(new String [] {PermissionManager.LOCATION_SERVICE},99);
            }

        }
    }

    public void setupDatabase(){ db = FirebaseFirestore.getInstance(); }

    public void getData(){

        final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        db.collection("newCollection").document(addressList.get(0).getCountryName()).collection(addressList.get(0).getAdminArea()).document(addressList.get(0).getSubAdminArea()).collection(addressList.get(0).getLocality()).document(addressList.get(0).getPostalCode()).collection(addressList.get(0).getThoroughfare()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
               int i = 0;
               int currenthigh = 0;
               if(task.isSuccessful()){
                   Map<String, Object> addedData = new HashMap<>();
                   for(QueryDocumentSnapshot documentSnapshots : task.getResult()){
                       Log.i("Success",documentSnapshots.toString());
                        Object latitude = documentSnapshots.get("Latitude");
                        Object longitude = documentSnapshots.get("Longitude");
                        Object bars = documentSnapshots.get("Bars");
                        Object wifiSpeed = documentSnapshots.get("WiFi Speed");
                        Object networkSpeed = documentSnapshots.get("Network Speed");
                        DecimalFormat decimalFormat = new DecimalFormat("#.####");
                        double lat = Double.valueOf(decimalFormat.format(Double.valueOf(latitude.toString())));
                        double lon =Double.valueOf(decimalFormat.format(Double.valueOf(longitude.toString())));
                        int intBar = Integer.valueOf(bars.toString());
                        double distance = distFrom(lat,lon);
                        int networkInt;
                        int wifiInt;

                        if(networkSpeed != null) {
                            networkInt = Integer.valueOf(networkSpeed.toString());
                        }
                        else{
                            networkInt = 0;
                        }

                        if(wifiSpeed != null) {
                            wifiInt = Integer.valueOf(wifiSpeed.toString());
                        }
                        else{
                            wifiInt = 0;
                        }

                        addedData.put("Latitude", lat);
                        addedData.put("Longitude",lon);
                        addedData.put("Bars",intBar);
                        addedData.put("Network Speed", networkInt);
                        addedData.put("WiFi Speed", wifiInt);
                        Log.i("distance", Double.toString(distance));
                        Log.i("intBar", Integer.toString(intBar));

                        if(intBar > currenthigh && distance < 1000){
                            currenthigh = intBar;
                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(Integer.toString(intBar)).snippet("Distance: " + (int) distance + "ft"));
                        }

                       if(!documentSnapshots.contains(Double.toString(lat).replace('.',' ') + Double.toString(lon).replace('.', ' '))){
                           databaseReference.child("shortPath").child(addressList.get(0).getCountryName()).child(addressList.get(0).getAdminArea()).child(addressList.get(0).getSubAdminArea()).child(addressList.get(0).getLocality()).child(addressList.get(0).getPostalCode()).child(addressList.get(0).getThoroughfare()).child(Double.toString(lat).replace('.',' ') + Double.toString(lon).replace('.', ' ')).setValue(addedData).addOnFailureListener(new OnFailureListener() {
                            @Override
                           public void onFailure(@NonNull Exception e) {
                               Log.i("What Happened", e.toString());
                           }
                            });
                       }


                        distances[i] = distance;
                        listLat[i] = lat;
                        listLon[i] = lon;
                        //locationLatLang.add(new LatLng(lat,lon));
                       i++;
                   }
               }
               Arrays.sort(distances);
           }
       });
    }

    public void getLocationInfo(){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addressList = geocoder.getFromLocation(47.7758434, -122.1880514, 1);
        }
        catch(Exception e){
            e.printStackTrace();
        }
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
                            }
                        }
                    });

        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(userLocation == null){
            Log.e("USER LOCATION", "IS NULL");
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        userLocation = location;

                        LatLng latLng = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
                        CameraUpdateFactory.newLatLng(latLng);
                        CameraPosition.builder().target(latLng);

                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
                        mMap.animateCamera(cameraUpdate);
                        getCellData();
                        mMap.addMarker(new MarkerOptions().position(latLng).title("Current Bars: " + cellStrength).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    }
                });
            }
        }

        }

    public void getCellData(){  //Collects cellphone data and places it into the CellStrength value

        TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            SignalStrength signalStrength = telephonyManager.getSignalStrength();
            cellStrength = signalStrength.getLevel() + 1;

        }

        else{ ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},99 ); }
    }

    private void addHeatMap(){
        List<LatLng> list = null;

       Provider mProvider;
       //mProvider = new HeatmapTileProvider().Builder().data(list).build();
    }

    public double distFrom(double lat1, double lng1) {     //Distance from lat and lon to user

        userLocation = myLocationManager.getLocation();
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

        return  dist * 3.28084;       //Convert to feet


    }
}
