package de.dickger.android.location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Arrays;

import static android.util.Log.d;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GPS-Location";
    private static final String EXTRA_TEXTVIEW = "de.dickger.android.location.textView";
    private static final String EXTRA_LOC_REQ_STATE = "de.dickger.android.location.locReqState";
    private static final int LOC_PERM_REQUEST_CODE = 1;

    private Button requestButton;
    private TextView resultView;
    private ScrollView scrollView;
    private boolean locationRequestActive = false;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            String msg = location.getLatitude() + " " + location.getLongitude() + ", alt: " + location.getAltitude();
            msg += ", accuracy:" + location.getAccuracy() + "by: " + location.getProvider();
            print(msg);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            print("Status changed: new: " + statusToString(status));
        }

        @Override
        public void onProviderEnabled(String provider) {
            print("Provider enabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            print("Provider disabled: " + provider);
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        scrollView = (ScrollView) findViewById(R.id.view_scroll);
        requestButton = (Button) findViewById(R.id.button_request);
        resultView = (TextView) findViewById(R.id.view_result);
        resultView.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultView.setText("");
            }
        });

        if (savedInstanceState != null) {
            String s = (String) savedInstanceState.getSerializable(EXTRA_TEXTVIEW);
            if (s != null) {
                resultView.setText(s);
            }
            locationRequestActive = savedInstanceState.getBoolean(EXTRA_LOC_REQ_STATE, false);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            print("running on Marshmallow or better, checking permissions");
            String granted = null;
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                granted = "COARSE_LOCATION";
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    granted = "ACCESS_FINE_LOCATION";
                }
            }
            if(granted == null) {
                print("permissions not granted, requesting 'COARSE_LOCATION'");
                requestPermissions(new String[]{/*Manifest.permission.ACCESS_FINE_LOCATION,*/
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, LOC_PERM_REQUEST_CODE);
            } else {
                print(granted + " permissions already granted, no grant request necessary");
                enableButton();
            }
        } else {
            enableButton();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        print("onRequestPermissionsResult() called");
        if(requestCode == LOC_PERM_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            print("permissions now granted" + Arrays.toString(permissions));
            enableButton();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_TEXTVIEW, resultView.getText().toString());
        outState.putBoolean(EXTRA_LOC_REQ_STATE, locationRequestActive);
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("MissingPermission")
    private void enableButton() {
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(locationRequestActive) {
            String provider = LocationManager.GPS_PROVIDER;
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                provider = LocationManager.NETWORK_PROVIDER;
            }
            requestButton.setText(R.string.button_request_stop);
            print("request LocationUpdates, provider=" + provider);
            locationManager.requestLocationUpdates(provider, 5000, 0, locationListener);
        } else {
            requestButton.setText(R.string.button_request_start);
            print("stop LocationUpdates");
            locationManager.removeUpdates(locationListener);
        }
        print("setting clickListener");
        requestButton.setEnabled(true);
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationRequestActive) {
                    locationRequestActive = false;
                    locationManager.removeUpdates(locationListener);
                    requestButton.setText(R.string.button_request_start);
                    print("stopped LocationUpdates");
                } else {
                    locationRequestActive = true;
                    String provider = LocationManager.GPS_PROVIDER;
                    if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                        provider = LocationManager.NETWORK_PROVIDER;
                    }
                    requestButton.setText(R.string.button_request_stop);
                    locationManager.requestLocationUpdates(provider, 5000, 0, locationListener);
                    requestButton.setText(R.string.button_request_stop);
                    print("requested LocationUpdates, provider=" + provider);
                }
            }
        });
    }

    private String statusToString(int status) {
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE: return "OUT_OF_SERVICE";
            case LocationProvider.AVAILABLE: return "AVAILABLE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE: return "TEMPORARILY_UNAVAILABLE";
        }
        return "unknown";
    }

    private void print(String s) {
        // Log.d(TAG, s);
        resultView.append("\n" + s);
        scrollView.smoothScrollTo(0, resultView.getBottom());
    }
}
