package pk.gov.pbs.utils_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
                if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED)){
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
        intentFilter.addAction(LocationService.BROADCAST_RECEIVER_ACTION_LOCATION_CHANGED);
        registerReceiver(receiver, intentFilter);

    }
}