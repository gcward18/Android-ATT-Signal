package com.example.findmysignal;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    FirebaseFirestore db;      //Firestone Database instance
    Location userLocation;     //When updated, stores the location of the user
    int cellStrength;          //Stores cell signal strength from 0 - 4 where 0 is very poor and 4 is very strong
    int mbps;                   //Stores the speed of current connection in MBPS
    Map<String, Object> locationData = new HashMap<>();     //HashMap of all the local data that is pushed to Firebase
    DecimalFormat decimalFormat = new DecimalFormat("#.####");
    MyLocationManager myLocationManager;
    private final float mOrientation[] = new float[3];
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mMagneticSensor;
    private Sensor mOrientationSensor;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float mCurrentAzimuth = 0;
    @Nullable
    private int mIntervalTime = 0;
    private long mLastTime = 0;
    private float[] Ro = new float[9];
    private float[] I = new float[9];
    float azimuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myLocationManager = new MyLocationManager(this,this);
        getLocation();        //Gets information on where the user is located
        setupLayout();
        setupDatabase();        //Starts the database initialization
        getCellData();      //Gets information from the cellphone like strength, speed and network typ
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        setupSensors();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void setupSensors(){
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void setupLayout(){

        Button cameraButton = findViewById(com.example.findmysignal.R.id.cameraButton);
        Button launchCamera = findViewById(R.id.cameraButton);
        Button mapButton = findViewById(R.id.mapButton);

        final Intent cameraIntent = new Intent(this,CameraActivity.class);
        final Intent mapsIntent = new Intent(this, MapsActivity.class);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCellData();
                getLocation();            }
        });

        launchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(cameraIntent);
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(mapsIntent);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;
        long time = System.currentTimeMillis();
        if (time - mLastTime > mIntervalTime) {
            synchronized (this) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                    mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                    mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
                }

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // mGeomagnetic = event.values;
                    mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                    mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                    mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];

                    // Log.e(TAG, Float.toString(event.values[0]));
                    float magneticField = (float) Math.sqrt(mGeomagnetic[0] * mGeomagnetic[0]
                            + mGeomagnetic[1] * mGeomagnetic[1]
                            + mGeomagnetic[2] * mGeomagnetic[2]);

                }


                boolean success = SensorManager.getRotationMatrix(Ro, I, mGravity, mGeomagnetic);
                if (success) {
                    SensorManager.getOrientation(Ro, mOrientation);
                     azimuth = (float) Math.toDegrees(mOrientation[0]);
                    azimuth = (azimuth + 360) % 360;
                    float pitch = (float) Math.toDegrees(mOrientation[1]);
                    float roll = (float) Math.toDegrees(mOrientation[2]);

                    Log.i("Azimuth", Float.toString(azimuth));
                    //Log.i("Pitch", Float.toString(pitch));
                    //Log.i("Roll", Float.toString(roll));

                }
            }
            mLastTime = time;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Acc Changed", "onAccuracyChanged() called with: sensor = [" + sensor + "], accuracy = [" + accuracy + "]");
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
        myLocationManager.startLocationReceiving();
    }
}
