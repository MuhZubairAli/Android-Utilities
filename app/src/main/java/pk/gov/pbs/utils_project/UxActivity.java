package pk.gov.pbs.utils_project;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.UXEvent;
import pk.gov.pbs.utils.location.LocationService;

public class UxActivity extends CustomActivity {
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
            mUXToolkit.alert(
                    "Example Title",
                    Utils.generateText(),
                    "Affirmative Label",
                    (dialog, which) -> mUXToolkit.toast("Alert closed")
            );
        });

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            mUXToolkit.confirm(
                    "Example Title",
                    Utils.generateText(),
                    "Affirmative Label",
                    "Negative Label",
                    new UXEvent.ConfirmDialogue() {
                        @Override
                        public void onCancel(DialogInterface dialog, int which) {
                            mUXToolkit.toast("Confirm Dialog Cancelled");
                        }

                        @Override
                        public void onOK(DialogInterface dialog, int which) {
                            mUXToolkit.toast("Confirm Dialog OK");
                        }
                    }
            );
        });

        findViewById(R.id.btnProgress).setOnClickListener(v -> {
            ProgressDialog dialog = mUXToolkit.buildProgressDialog(
                    "Test Progress",
                    "Example tile for progress dialog, it is cancellable, it could be made non-cancellable by setting cancellable to false"
                    , new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            mUXToolkit.toast("Progress Dialog Cancelled");
                        }
                    }
            );

            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
        });

        findViewById(R.id.btnShowKB).setOnClickListener(v -> {
            EditText et = findViewById(R.id.etText);
            mUXToolkit.showKeyboardTo(et);
        });

        findViewById(R.id.btnHideKB).setOnClickListener(v -> {
            EditText et = findViewById(R.id.etText);
            mUXToolkit.hideKeyboardFrom(et);
        });

        LocationService.startActiveMode(this, pk.gov.pbs.utils_project.LocationActivity.class);
        mLocationReceiver = new LocationReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            registerReceiver(mLocationReceiver, new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED), RECEIVER_EXPORTED);
        else
            registerReceiver(mLocationReceiver, new IntentFilter(LocationService.BROADCAST_ACTION_LOCATION_CHANGED));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLocationReceiver);
    }

    public final class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(LocationService.BROADCAST_ACTION_LOCATION_CHANGED)){
                Location location = intent.getParcelableExtra(LocationService.BROADCAST_EXTRA_LOCATION_DATA);
                UxActivity.this.mUXToolkit.buildConfirm(
                        "Location Changed",
                        "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude(),
                        "OK",
                        "Cancel",
                        new UXEvent.ConfirmDialogue() {
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