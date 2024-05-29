package pk.gov.pbs.utils;

import static pk.gov.pbs.utils.UXToolkit.CommonAlerts.showAlertLocationSettings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public abstract class LocationActivity extends CustomActivity {
    private static final String TAG = "LocationActivity]:";
    private boolean IS_LOCATION_SERVICE_BOUND = false;
    private Runnable mAfterLocationServiceStartCallback;
    private LocationService mLocationService = null;
    private ServiceConnection mLocationServiceConnection = null;
    private BroadcastReceiver GPS_PROVIDER_ACCESS = null;
    private final List<ILocationChangeCallback> mLocationChangeCallbacks = new ArrayList<>();
    private static byte mLocationAttachAttempts = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mLocationService != null) {
            if (!mLocationService.isNetworkEnabled() && !mLocationService.isGPSEnabled())
                showAlertLocationSettings(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (LocationService.isRunning()) {
            unregisterReceiver(GPS_PROVIDER_ACCESS);
            unbindLocationService();
        }
        super.onDestroy();
    }

    private void initialize(){
        addPermissions(LocationService.getPermissionsRequired());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addSpecialPermission(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    "This application requires background location to work properly, Please enable the option of 'Allow all the time' on Location Permissions screen."
                    , new PermissionRequest() {
                        @Override
                        public boolean hasPermission() {
                            return
                                    (
                                        ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        || ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    ) && ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                        }

                        @Override
                        public void askPermission() {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
                        }
                    }
            );
        }

        GPS_PROVIDER_ACCESS = new ProviderDisabledReceiver();
        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(GPS_PROVIDER_ACCESS, intentFilter, RECEIVER_NOT_EXPORTED);
        } else
            registerReceiver(GPS_PROVIDER_ACCESS, intentFilter);
    }


    /**
     * This method start the LocationService and bind this activity with the service
     * in order to user addLocationChangedCallback(ILocationChangeCallback) use this method
     * to start the service. otherwise location change event do not execute the callbacks
     * and current location is only delivered by BroadcastReceiver
     *
     * Please note that if service is running then it will always send new location to BroadcastReceivers
     * no matter an activity is bound with the service or not
     */
    protected void startLocationService(){
        startLocationService(null);
    }

    /**
     * This method start the LocationService and bind this activity with the service
     * in order to user addLocationChangedCallback(ILocationChangeCallback) use this method
     * to start the service. otherwise location change event do not execute the callbacks
     * and current location is only delivered by BroadcastReceiver
     * @param notificationActivity activity for PendingIntent (for Service Notification)
     */
    protected void startLocationService(@Nullable Class<? extends Context> notificationActivity){
        Log.d(TAG, "startLocationService: Starting location service");
        if (mLocationServiceConnection == null) {
            mLocationServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
                    mLocationService = binder.getService();
                    binder.setLocationChangeCallbacks(mLocationChangeCallbacks);
                    if (!mLocationService.isNetworkEnabled() && !mLocationService.isGPSEnabled())
                        showAlertLocationSettings(LocationActivity.this);

                    if (mAfterLocationServiceStartCallback != null)
                        mAfterLocationServiceStartCallback.run();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mLocationService = null;
                    IS_LOCATION_SERVICE_BOUND = false;
                }
            };
        }

        if (mLocationService == null) {
            Intent intent = new Intent(this, LocationService.class);
            if (notificationActivity != null){
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        LocationService.BROADCAST_EXTRA_NOTIFICATION_INTENT,
                        PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, notificationActivity),
                                PendingIntent.FLAG_UPDATE_CURRENT |
                                        PendingIntent.FLAG_IMMUTABLE
                        )
                );
                intent.putExtras(bundle);
            }

            IS_LOCATION_SERVICE_BOUND = bindService(
                    intent,
                    mLocationServiceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }

    protected void stopLocationService(){
        if (LocationService.isRunning()) {
            unbindLocationService();
            if(!stopService(new Intent(this, LocationService.class)))
                getLocationService().stopSelf();
        }
    }

    /**
     * in order to unbind the LocationService use this method
     * this will not stop the service and BroadcastReceivers shall still
     * receive new locations but LocationChangeCallbacks will not be executed
     */
    protected void unbindLocationService(){
        if(LocationService.isRunning()) {
            if (GPS_PROVIDER_ACCESS.isOrderedBroadcast()) {
                GPS_PROVIDER_ACCESS.clearAbortBroadcast();
                unregisterReceiver(GPS_PROVIDER_ACCESS);
            }

            if (IS_LOCATION_SERVICE_BOUND) {
                unbindService(mLocationServiceConnection);
                IS_LOCATION_SERVICE_BOUND = false;
            }
        }
    }
    public LocationService getLocationService(){
        if (mLocationService == null && LocationService.isRunning())
            mLocationService = LocationService.getInstance();
        return mLocationService;
    }

    public void addLocationChangedCallback(ILocationChangeCallback callback){
        mLocationChangeCallbacks.add(callback);
    }
    public void removeLocationChangedCallback(ILocationChangeCallback callback){
        mLocationChangeCallbacks.remove(callback);
    }

    /**
     * this helper method attaches OTC with location service
     * it will attempt 5 times with 1 sec gap if locationService is not available then throws exception
     * it will not start location service (only waits 5 secs if location service is starting)
     * @param callback OTC to be attached with the service
     */
    public void addLocationChangedOneTimeCallback(ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                getLocationService().addLocationChangedOTC(callback);
                mLocationAttachAttempts = 0;
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    ExceptionReporter.handle(
                            new Exception("addOTC] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started")
                    );
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangedOneTimeCallback(callback);
            }
        },1000);
    }

    public void verifyCurrentLocation(ILocationChangeCallback callback){
        if (mLocationService != null) {
            checkLocationAndRunCallback(callback);
        } else {
            startLocationService();
            mAfterLocationServiceStartCallback = () -> {
                checkLocationAndRunCallback(callback);
            };
        }
    }

    private void checkLocationAndRunCallback(@NonNull ILocationChangeCallback callback){
        if (mLocationService.getLocation() == null) {
            mUXToolkit.showProgressDialogue("Getting current location, please wait...");
            mLocationService.addLocationChangedOTC((location -> {
                mUXToolkit.dismissProgressDialogue();
                callback.onLocationChange(location);
            }));
        } else {
            callback.onLocationChange(mLocationService.getLocation());
        }
    }

    public class ProviderDisabledReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED)) {
                showAlertLocationSettings(context);
            }
        }
    }
}
