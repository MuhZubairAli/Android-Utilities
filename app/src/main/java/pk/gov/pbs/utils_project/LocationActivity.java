package pk.gov.pbs.utils_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.DateTimeUtil;
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.location.LocationService;

public class LocationActivity extends CustomActivity {

    TextView tvLocation;
    TableLayout tblLocation;
    LocationBroadcast locationReceiver;
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

        findViewById(R.id.btnStart).setOnClickListener((v) -> startLocationService());
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
            if (!LocationService.hasAllPermissions(this)) {
                LocationService.requestRequiredPermissions(this);
            }
            else
                mUXToolkit.showToast("All permissions granted!");
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.BROADCAST_ACTION_LOCATION_CHANGED);
        locationReceiver = new LocationBroadcast();
        registerReceiver(locationReceiver, intentFilter);
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
        ((TextView) row.findViewById(R.id.tv_3)).setText("Altitude");
        ((TextView) row.findViewById(R.id.tv_4)).setText(location.getAltitude() + "");
        ((TextView) row.findViewById(R.id.tv_1)).setText("Accuracy");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getAccuracy() + "");
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
        ((TextView) row.findViewById(R.id.tv_1)).setText("Provider");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getProvider());
        tblLocation.addView(row);
        row = getRow();
        ((TextView) row.findViewById(R.id.tv_1)).setText("Elapsed Time");
        ((TextView) row.findViewById(R.id.tv_2)).setText(location.getElapsedRealtimeNanos() + "");
        tblLocation.addView(row);
    }

    private TableRow getRow(){
        return (TableRow) getLayoutInflater().inflate(R.layout.row_4_cols, tblLocation, false);
    }

    public class LocationBroadcast extends BroadcastReceiver {
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
    }
}