package pk.gov.pbs.utils_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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

public class LocationActivity extends pk.gov.pbs.utils.LocationActivity {

    TextView tvLocation;
    TableLayout tblLocation;
    BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_ACTION_LOCATION_CHANGED)
                    || intent.getAction().equalsIgnoreCase("android.location.LOCATION_CHANGED")) {
                Location location = intent.getParcelableExtra(LocationService.BROADCAST_EXTRA_LOCATION_DATA);
                if (location != null) {
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
    };

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

        tvLocation = findViewById(R.id.tvLocation);
        tblLocation = findViewById(R.id.tblLocation);

        findViewById(R.id.btnStart).setOnClickListener((v) -> startLocationService(MainActivity.class));
        findViewById(R.id.btnStop).setOnClickListener((v) -> stopLocationService());

        findViewById(R.id.btnLocation).setOnClickListener((v) -> {
            if (getLocationService() == null) {
                mUXToolkit.showToast("Location service not started!");
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

        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());

        findViewById(R.id.btnPermissions).setOnClickListener((v) -> {
            if (!LocationService.hasRequiredPermissions(this)) {
                LocationService.requestRequiredPermissions(this);
            }
            else
                mUXToolkit.showToast("Required permissions granted! LocationService could be used now!");
        });

        findViewById(R.id.btnLocSettings).setOnClickListener((v) -> {
            showAlertLocationSettings();
        });

        findViewById(R.id.btnLastKnown).setOnClickListener((v) -> {
            if (getLocationService() == null)
                mUXToolkit.showAlertDialogue("Location service not started");
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

        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED);
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
                mUXToolkit.showAlertDialogue("Location permissions not granted");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!LocationService.hasPermissionBackgroundAccess(this)) {
                    LocationService.requestPermissionBackground(this);
                }
            }
        }
    }
    private void showLocation(Location location) throws IllegalAccessException {
        TableRow header = (TableRow) getLayoutInflater().inflate(R.layout.header_row, tblLocation, false);
        ((TextView) header.findViewById(R.id.tv_1)).setText("Location Received at " + DateTimeUtil.formatDateTime(location.getTime()/1000));
        tblLocation.addView(header);

        TableRow row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Latitude");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getLatitude() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Longitude");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getLongitude() + "");
        tblLocation.addView(row);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Altitude");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getAltitude() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Accuracy");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getAccuracy() + "");
        tblLocation.addView(row);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Bearing");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getBearing() + "");
        ((TextView) row.findViewById(R.id.tv_3)).setText("Speed");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getSpeed() + "");
        tblLocation.addView(row);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Location Time");
        String dt = DateTimeUtil.formatDateTime(location.getTime()/1000);
        ((TextView) row.findViewById(R.id.tv_2)).setText(dt);
        ((TextView) row.findViewById(R.id.tv_3)).setText("Provider");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getProvider());
        tblLocation.addView(row);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Elapsed Time");
        ((TextView) row.findViewById(R.id.tv_2)).setText(DateTimeUtil.formatDateTime(location.getElapsedRealtimeNanos()/1000000));
        tblLocation.addView(row);
    }

    private TableRow getRow(){
        return (TableRow) getLayoutInflater().inflate(R.layout.row_4_cols, tblLocation, false);
    }
}