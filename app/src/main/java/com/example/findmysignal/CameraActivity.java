package com.example.findmysignal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.EnumSet;

import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALLED;
import static com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED;

public class CameraActivity extends AppCompatActivity {

    private Session mSession;
    ArFragment arFragment;
    private ModelRenderable arrowRenderable;
    Button mArButton;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        setContentView((R.layout.camera_layout));
        Button mArButton = findViewById(R.id.arButton);
        maybeEnableArButton();
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_ux_fragment);



    }

    public void maybeEnableArButton(){
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Re-query at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
        if (availability.isSupported()) {
            mArButton.setVisibility(View.VISIBLE);
            mArButton.setEnabled(true);
            // indicator on the button.
        } else { // Unsupported or unknown.
            mArButton.setVisibility(View.INVISIBLE);
            mArButton.setEnabled(false);
        }

    }


    private boolean mUserRequestedInstall = true;

    @Override
    protected void onResume() {
        super.onResume();
        checkARCore();
        setupCamera();


    }

    public void checkARCore(){

        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        mSession = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                        return;
                    }
                 }
            }
        catch (UnavailableUserDeclinedInstallationException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
            }

        catch (Exception e) {  // Current catch statements.
            e.printStackTrace();
            return;  // mSession is still null.
        }


    }

    public void requestInstall() {

        ArCoreApk.Availability availability =
                ArCoreApk.getInstance().checkAvailability(this);

// Request ARCore installation or update if needed.
        switch (availability) {
            case SUPPORTED_INSTALLED:
                break;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:

                //ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this,true);

                break;

        }
    }

    public void setupCamera(){

        try {

            // Create ARCore session that supports camera sharing.
            Session sharedSession = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));

// Store the ARCore shared camera reference.
            SharedCamera sharedCamera = sharedSession.getSharedCamera();

// Store the ID of the camera used by ARCore.
           String cameraId = sharedSession.getCameraConfig().getCameraId();


        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    public void openCamera() {
    }
}
