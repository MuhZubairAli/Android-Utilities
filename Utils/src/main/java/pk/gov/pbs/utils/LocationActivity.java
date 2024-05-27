package pk.gov.pbs.utils;

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
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import pk.gov.pbs.utils.exceptions.InvalidIndexException;
import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public abstract class LocationActivity extends CustomActivity {
    private static final String TAG = "LocationActivity";
    private boolean IS_LOCATION_SERVICE_BOUND = false;
    private AlertDialog dialogLocationSettings;
    private Runnable mAfterLocationServiceStartCallback;
    private LocationService mLocationService = null;
    private ServiceConnection mLocationServiceConnection = null;
    private BroadcastReceiver GPS_PROVIDER_ACCESS = null;
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
                showAlertLocationSettings();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getLocationService() != null)
            getLocationService().clearLocalLocationChangeCallbacks(this);
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

        GPS_PROVIDER_ACCESS = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showAlertLocationSettings();
            }
        };
    }

    public LocationService getLocationService(){
        return mLocationService;
    }

    public void addLocationChangeGlobalCallback(String index, ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                try {
                    getLocationService().addLocationChangeGlobalCallback(index, callback);
                } catch (InvalidIndexException e) {
                    ExceptionReporter.handle(e);
                }
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangeGlobalCallback(index, callback);
            }
        },1000);
    }

    /**
     * this helper method attaches LocationChange callback with location service
     * it will attempt 5 times with 1 sec gap if locationService is not available then throws exception
     * it will not start location service (only waits 5 secs if location service is starting)
     * @param callback OTC to be attached with the service
     */
    public void addLocationChangeCallback(ILocationChangeCallback callback) {
        StaticUtils.getHandler().postDelayed(()-> {
            if (getLocationService() != null) {
                getLocationService().addLocalLocationChangeCallback(this, callback);
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangeCallback(callback);
            }
        }, 1000);
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
            } else {
                if (++mLocationAttachAttempts >= 5) {
                    Exception e =  new Exception("addLocationChangeCallback] - Attempt to add location listener to LocationService failed after 5 tries, Location service has not started, make sure startLocationService() is called before adding listener");
                    ExceptionReporter.handle(e);
                    mLocationAttachAttempts = 0;
                } else
                    addLocationChangedOneTimeCallback(callback);
            }
        },1000);
    }

    protected void startLocationService(){
        startLocationService(null);
    }

    protected void startLocationService(@Nullable Class<? extends Context> notificationActivity){
        Log.d(TAG, "startLocationService: Starting location service");
        if (mLocationServiceConnection == null) {
            mLocationServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
                    mLocationService = binder.getService();

                    if (!mLocationService.isNetworkEnabled() && !mLocationService.isGPSEnabled())
                        showAlertLocationSettings();

                    if (mAfterLocationServiceStartCallback != null)
                        mAfterLocationServiceStartCallback.run();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mLocationService = null;
                }
            };
        }

        if (mLocationService == null) {
            Intent intent = new Intent(this, LocationService.class);
            if (notificationActivity != null){
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        LocationService.NOTIFICATION_EXTRA_PENDING_INTENT,
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent);

            IS_LOCATION_SERVICE_BOUND = bindService(intent,
                    mLocationServiceConnection, Context.BIND_AUTO_CREATE);
        }

        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED);
        registerReceiver(GPS_PROVIDER_ACCESS, intentFilter);
    }

    protected void stopLocationService(){
        if (mLocationService != null) {
            if (GPS_PROVIDER_ACCESS.isOrderedBroadcast())
                unregisterReceiver(GPS_PROVIDER_ACCESS);
            if (IS_LOCATION_SERVICE_BOUND) {
                unbindService(mLocationServiceConnection);
                IS_LOCATION_SERVICE_BOUND = false;
            }
            stopService(new Intent(this, LocationService.class));
        }
    }

    public void verifyCurrentLocation(@Nullable ILocationChangeCallback callback){
        if(Constants.DEBUG_MODE) {
            if (callback != null) {
                Location location = null;
                if (mLocationService != null)
                    location = mLocationService.getLocation();

                if (location == null)
                    location = mLocationService.getLastKnownLocation();

                if (location == null)
                    location = new Location(LocationManager.GPS_PROVIDER);

                callback.onLocationChange(location);
            }
            return;
        }

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

    protected void showAlertLocationSettings(){
        try {
            if (!isDestroyed() && !isFinishing()) {
                if(dialogLocationSettings == null) {
                    dialogLocationSettings = mUXToolkit.buildAlertDialogue(
                            getString(R.string.alert_dialog_gps_title)
                            ,getString(R.string.alert_dialog_gps_message)
                            ,getString(R.string.label_btn_location_settings)
                            , (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                    );
                }

                if(!dialogLocationSettings.isShowing())
                    dialogLocationSettings.show();
            }
        } catch (Exception e){
            ExceptionReporter.handle(e);
        }
    }
}
