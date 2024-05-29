package pk.gov.pbs.utils_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.LocationActivity;
import pk.gov.pbs.utils.UXEventListeners;
import pk.gov.pbs.utils.UXToolkit;
import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public class UxActivity extends LocationActivity {
    LocationReceiver mLocationReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ux);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Utils.setVersionTextAndBehaviour(this, findViewById(R.id.tvApiLevel));

        findViewById(R.id.btnAlert).setOnClickListener(v -> {
            mUXToolkit.showAlertDialogue(
                    "Example Title",
                    Utils.generateText(),
                    "Affirmative Label",
                    (dialog, which) -> mUXToolkit.showToast("Alert closed")
            );
        });

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            mUXToolkit.showConfirmDialogue(
                    "Example Title",
                    Utils.generateText(),
                    "Affirmative Label",
                    "Negative Label",
                    new UXEventListeners.ConfirmDialogueEventsListener() {
                        @Override
                        public void onCancel(DialogInterface dialog, int which) {
                            mUXToolkit.showToast("Confirm Dialog Cancelled");
                        }

                        @Override
                        public void onOK(DialogInterface dialog, int which) {
                            mUXToolkit.showToast("Confirm Dialog OK");
                        }
                    }
            );
        });

        findViewById(R.id.btnProgress).setOnClickListener(v -> {
            mUXToolkit.showProgressDialogue("Example tile for progress dialog, it is cancellable, it could be made non-cancellable by setting cancellable to false",true);
        });

        findViewById(R.id.btnShowKB).setOnClickListener(v -> {
            EditText et = findViewById(R.id.etText);
            mUXToolkit.showKeyboardTo(et);
        });

        findViewById(R.id.btnHideKB).setOnClickListener(v -> {
            EditText et = findViewById(R.id.etText);
            mUXToolkit.hideKeyboardFrom(et);
        });

        LocationService.start(this, pk.gov.pbs.utils_project.LocationActivity.class);
        mLocationReceiver = new LocationReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            registerReceiver(mLocationReceiver, new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED), RECEIVER_EXPORTED);
        else
            registerReceiver(mLocationReceiver, new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED));

    }

    public static final class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_ACTION_LOCATION_CHANGED)){
                Location location = intent.getParcelableExtra(LocationService.BROADCAST_EXTRA_LOCATION_DATA);
                UXToolkit.CommonAlerts.buildConfirmDialogue(
                        context,
                        "Location Changed",
                        "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude(),
                        "OK",
                        "Cancel",
                        new UXEventListeners.ConfirmDialogueEventsListener() {
                            @Override
                            public void onCancel(DialogInterface dialog, int which) {
                                Toast.makeText(context, "Location Dialog Cancelled", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onOK(DialogInterface dialog, int which) {
                                Toast.makeText(context, "Location Changed -> " + location, Toast.LENGTH_LONG).show();
                            }
                        }
                ).show();
            }
        }
    }
}