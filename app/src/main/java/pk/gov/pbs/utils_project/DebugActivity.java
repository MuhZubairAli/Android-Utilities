package pk.gov.pbs.utils_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.location.LocationService;

public class DebugActivity extends CustomActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_debug);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LocationService.start(this);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_ACTION_LOCATION_CHANGED)){
                    Location location = intent.getParcelableExtra(LocationService.BROADCAST_EXTRA_LOCATION_DATA);
                    String loc = DateTimeUtil.getCurrentDateTimeString() + "<br />\n";
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
                    loc += StaticUtils.toPrettyJson(map);
                    ((TextView) findViewById(R.id.tvLocation)).setText(Html.fromHtml(loc));
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.BROADCAST_ACTION_LOCATION_CHANGED);
        registerReceiver(receiver, intentFilter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (LocationService.hasAllPermissions(this))
            LocationService.start(this);

        if (requestCode == LocationService.PERMISSION_REQUEST_CODE){
            boolean has = true;
            for (int result : grantResults)
                has &= result == PackageManager.PERMISSION_GRANTED;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (has && !LocationService.hasPermissionBackgroundAccess(this)) {
                    LocationService.requestPermissionBackground(this);
                }
            }
        }
    }
}