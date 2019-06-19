package com.example.findmysignal;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/* Singleton class that tracks the users location */
public class MyLocationManager{

    private Context context = null;
    private Activity activity = null;
    private LocationManager mLocationManager = null;
    PermissionManager permissionManager = null;

    public static Location getLocation() {
        return location;
    }

    public static void setLocation(Location location) {
        MyLocationManager.location = location;
    }

    private static Location location = null;

    public MyLocationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.permissionManager = new PermissionManager(context,activity);
        startLocationReceiving();


    }

    private GPSLocationListener gpsLocationListener = new GPSLocationListener();
    private NetworkLocationListener networkLocationListener = new NetworkLocationListener();

    public void positionReceived(Location location){
        MyLocationManager.location = location;
    }


    private class GPSLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            Log.i("Location", "position received: " + location);
            if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
                return;
            }
            positionReceived(location);
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 100F, gpsLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400, 100F, networkLocationListener);
        }
    }

    public void stopLocationReceiving() {
        mLocationManager.removeUpdates(gpsLocationListener);
        mLocationManager.removeUpdates(networkLocationListener);
    }

    // Takes two set of coords and returns the amount of miles between those coords
    public int distFrom(double lat1, double lng1) {

            double lng2 = location.getLongitude();
            double lat2 = location.getLatitude();

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

}
