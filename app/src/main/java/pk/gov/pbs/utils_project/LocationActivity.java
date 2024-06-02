package pk.gov.pbs.utils_project;

import static pk.gov.pbs.utils.UXToolkit.CommonAlerts.showLocationSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.DateTimeUtil;
import pk.gov.pbs.utils.ExceptionReporter;
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.location.LocationService;

public class LocationActivity extends CustomActivity {

    TextView tvLocation;
    TableLayout tblLocation;
    BroadcastReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Utils.setVersionTextAndBehaviour(this, findViewById(R.id.tvApiLevel));

        tvLocation = findViewById(R.id.tvLocation);
        tblLocation = findViewById(R.id.tblLocation);

        findViewById(R.id.btnStart).setOnClickListener((v) -> {
            try {
                if (LocationService.hasRequiredPermissions(this))
                    startLocationService(LocationService.Mode.ACTIVE, this.getClass());
                else
                    mUXToolkit.alert("Required permissions not granted");
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        });

        findViewById(R.id.btnStop).setOnClickListener((v) -> unbindLocationService());

        findViewById(R.id.btnLocation).setOnClickListener((v) -> {
            if (getLocationService() == null) {
                mUXToolkit.toast("Location service not started!");
                return;
            }

            Location location = getLocationService().getLocation();
            if (location != null) {
                try {
                    tvLocation.setText(StaticUtils.toPrettyJson(location));
                    showLocation(location);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        
        findViewById(R.id.btnModeIdle).setOnClickListener((v) -> {
            if (getLocationService() == null) {
                mUXToolkit.toast("Location service not started!");
                return;
            }
            getLocationService().setModeIdle();
        });

        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getLocationService() == null) {
                    mUXToolkit.toast("Location service not started!");
                    return;
                }
                stopLocationService();
            }
        });

        findViewById(R.id.btnModeActive).setOnClickListener((v) -> {
            if (getLocationService() == null) {
                mUXToolkit.toast("Location service not started!");
                return;
            }
            getLocationService().setModeActive();
        });

        findViewById(R.id.btnModePassive).setOnClickListener((v) -> {
            if (getLocationService() == null) {
                mUXToolkit.toast("Location service not started!");
                return;
            }
            getLocationService().setModePassive();
        });

        findViewById(R.id.btnPermissions).setOnClickListener((v) -> {
            if (!LocationService.hasRequiredPermissions(this)) {
                LocationService.requestRequiredPermissions(this);
            }
            else
                mUXToolkit.alert("Required permissions granted! LocationService could be used now!");
        });

        findViewById(R.id.btnLocSettings).setOnClickListener((v) -> {
            showLocationSettings(getUXToolkit());
        });

        findViewById(R.id.btnLastKnown).setOnClickListener((v) -> {
            if (getLocationService() == null)
                mUXToolkit.toast("Location service not started");
            else {
                Location location = getLocationService().getLastKnownLocation();
                if (location != null) {
                    try {
                        tvLocation.setText(StaticUtils.toPrettyJson(location));
                        showLocation(location);
                    } catch (IllegalAccessException e) {
                        ExceptionReporter.handle(e);
                    }
                }
            }
        });

        addLocationChangedCallback(location -> {
            Log.i("===BOUND===", "Location Changed Callback");
            mUXToolkit.alert("Location Received from : "+ location.getProvider(),"This alert is raised from location change callback because service is bound \n<br />" + StaticUtils.toPrettyJson(location));
        });

        locationReceiver = new LocationChangeReceiver();
        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(locationReceiver, intentFilter, RECEIVER_EXPORTED);
        } else
            registerReceiver(locationReceiver, intentFilter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationService.PERMISSION_REQUEST_CODE){
            boolean has = true;
            for (int result : grantResults)
                has &= result == PackageManager.PERMISSION_GRANTED;

            if (!has){
                mUXToolkit.alert("Location permissions not granted");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!LocationService.hasPermissionBackgroundAccess(this)) {
                    LocationService.requestPermissionBackground(this);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationReceiver != null) {

            unregisterReceiver(locationReceiver);
        }
    }

    private void showLocation(Location location) throws IllegalAccessException {
        TableRow header = (TableRow) getLayoutInflater().inflate(R.layout.header_row, tblLocation, false);

        TableRow row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Latitude");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getLatitude() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Longitude");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getLongitude() + "");
        tblLocation.addView(row, 0);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Altitude");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getAltitude() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Accuracy");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getAccuracy() + "");
        tblLocation.addView(row, 0);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Bearing");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getBearing() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Speed");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getSpeed() + "");
        tblLocation.addView(row,0);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Location Time");
        String dt = DateTimeUtil.formatDateTime(location.getTime()/1000);
        ((TextView) row.findViewById(R.id.tv_2)).setText(dt);
        ((TextView) row.findViewById(R.id.tv_3)).setText("Provider");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getProvider());
        tblLocation.addView(row,0);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Elapsed Time");
        ((TextView) row.findViewById(R.id.tv_2)).setText(DateTimeUtil.formatDateTime(location.getElapsedRealtimeNanos()/1000000));
        tblLocation.addView(row,0);

        ((TextView) header.findViewById(R.id.tv_1)).setText("Location Received at " + DateTimeUtil.formatDateTime(location.getTime()/1000));
        tblLocation.addView(header,0);

    }

    private TableRow getRow(){
        return (TableRow) getLayoutInflater().inflate(R.layout.row_4_cols, tblLocation, false);
    }

    public class LocationChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_ACTION_LOCATION_CHANGED)) {
                Location location = intent.getParcelableExtra(LocationService.BROADCAST_EXTRA_LOCATION_DATA);
                if (tvLocation != null && location != null) {
                    Map<String, String> map = new HashMap<>();
                    map.put("Provider", location.getProvider());
                    map.put("Latitude", location.getLatitude() + "");
                    map.put("Longitude", location.getLongitude() + "");
                    map.put("Altitude", location.getAltitude() + "");
                    map.put("Accuracy", location.getAccuracy() + "");
                    map.put("Bearing", location.getBearing() + "");
                    map.put("Speed", location.getSpeed() + "");
                    map.put("Location Time", DateTimeUtil.formatDateTime(location.getTime()/1000));
                    map.put("Elapsed Time", DateTimeUtil.formatDateTime(location.getElapsedRealtimeNanos()/1000000));
                    tvLocation.setText(StaticUtils.toPrettyJson(map));
                    try {
                        showLocation(location);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}