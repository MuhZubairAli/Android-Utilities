package pk.gov.pbs.utils.location;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pk.gov.pbs.utils.Constants;
import pk.gov.pbs.utils.DateTimeUtil;
import pk.gov.pbs.utils.ExceptionReporter;
import pk.gov.pbs.utils.R;
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.SystemUtils;
import pk.gov.pbs.utils.exceptions.InvalidIndexException;

public class LocationService extends Service {
    private static final String TAG = ":Utils] LocationService";
    public static final String BROADCAST_ACTION_PROVIDER_DISABLED = Constants.Location.BROADCAST_ACTION_PROVIDER_DISABLED;
    public static final String BROADCAST_ACTION_LOCATION_CHANGED = Constants.Location.BROADCAST_ACTION_LOCATION_CHANGED;
    public static final String BROADCAST_EXTRA_LOCATION_DATA = Constants.Location.BROADCAST_EXTRA_LOCATION_DATA;
    public static final int PERMISSION_REQUEST_CODE = 10;
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private HashMap<String, List<ILocationChangeCallback>> mOnLocationChangedLocalCallbacks;
    private HashMap<String, ILocationChangeCallback> mOnLocationChangedGlobalCallbacks;
    private List<ILocationChangeCallback> mListOTCs;
    private final LocationServiceBinder mBinder = new LocationServiceBinder();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5;

    protected LocationManager mLocationManager;
    protected LocationListener mLocationListener;
    private final String notificationTitle = "Location Service";

    public static String[] getPermissionsRequired(){
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static String getPermissionBackgroundAccess(){
        return Manifest.permission.ACCESS_BACKGROUND_LOCATION;
    }

    public static boolean hasAllPermissions(Context context){
        boolean has = hasRequiredPermissions(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            has = has && hasPermissionBackgroundAccess(context);
        return has;
    }

    public static boolean hasRequiredPermissions(Context context){
        boolean has = true;
        for (String perm : getPermissionsRequired())
            has = has && ActivityCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED;
        return has;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean hasPermissionBackgroundAccess(Context context){
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRequiredPermissions(Activity activity){
        ActivityCompat.requestPermissions(
                activity,
                getPermissionsRequired(),
                PERMISSION_REQUEST_CODE
        );
    }

    public static void requestAllPermissions(Activity activity){
        ActivityCompat.requestPermissions(
                activity,
                getPermissionsRequired(),
                PERMISSION_REQUEST_CODE
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void requestPermissionBackground(Activity activity){
        if (ActivityCompat.shouldShowRequestPermissionRationale( activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            Toast.makeText(activity, "Please select option 'Allow all the time' in order to grant background location access permission", Toast.LENGTH_SHORT).show();

        ActivityCompat.requestPermissions(
                activity,
                new String[]{getPermissionBackgroundAccess()},
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onCreate() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onCreate: Location service created");

        SystemUtils.createNotificationChannel(
                this
                , Constants.Notification_Channel_Name
                , Constants.Notification_Channel_ID
        );

        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationServiceListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, Constants.Notification_Channel_ID)
                .setContentTitle(notificationTitle)
                .setContentText("Observing device location")
                .setSmallIcon(R.drawable.ic_auto_mode)
                .build();

        if(!requestLocationUpdates())
            ExceptionReporter.handle(new Exception("Failed to request location updates"));

        startForeground(SERVICE_NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(mLocationListener);
    }

    public synchronized static void start(Context context){
        if (LocationService.hasRequiredPermissions(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, LocationService.class));
            } else
                context.startService(new Intent(context, LocationService.class));
        } else
            ExceptionReporter.handle(new Exception("can not start LocationService, Permissions pertaining to LocationService not granted"));
    }

    public boolean isGPSEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected boolean requestLocationUpdates() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "requestLocationUpdates]: requesting location updates");

        if (hasAllPermissions(this)) {
            if (isGPSEnabled() || isNetworkEnabled()) {
                if (isNetworkEnabled()) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            mLocationListener);
                }

                if (isGPSEnabled()){
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            mLocationListener);
                }
                return true;
            }
        } else {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "requestLocationUpdates]: Permissions for location access not granted");
        }
        return false;
    }

    public void startLocationUpdates() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "startLocationUpdates]: starting location updates");
        if(requestLocationUpdates())
            ExceptionReporter.handle(new Exception("Failed to request location updates"));
    }

    public void stopLocationUpdates() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "stopLocationUpdates]: stopping location updates");
        mLocationManager.removeUpdates(mLocationListener);
    }

    public Location getLocation() {
        Location gpsLocation = null;
        Location networkLocation = null;
        if (isGPSEnabled())
            gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (isNetworkEnabled())
            networkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(gpsLocation != null && networkLocation != null)
            return gpsLocation.getAccuracy() < networkLocation.getAccuracy() ? gpsLocation : networkLocation;
        return gpsLocation == null ? networkLocation :gpsLocation;
    }

    public Location getLastKnownLocation(){
        Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(gpsLocation != null && networkLocation != null)
            return gpsLocation.getAccuracy() < networkLocation.getAccuracy() ? gpsLocation : networkLocation;
        return gpsLocation == null ? networkLocation :gpsLocation;
    }

    public void addLocationChangeGlobalCallback(String index, ILocationChangeCallback callback) throws InvalidIndexException {
        if (mOnLocationChangedGlobalCallbacks == null)
            mOnLocationChangedGlobalCallbacks = new HashMap<>();

        if (mOnLocationChangedGlobalCallbacks.containsKey(index))
            throw new InvalidIndexException(index, "it already exists");

        mOnLocationChangedGlobalCallbacks.put(index, callback);
    }

    public void removeLocationChangeGlobalCallback(String index){
        if (mOnLocationChangedGlobalCallbacks != null)
            mOnLocationChangedGlobalCallbacks.remove(index);
    }

    /**
     * adds One Time Callback to the service
     * on receiving location it will execute once and then remove it
     * @param otc one time callback
     */
    public void addLocationChangedOTC(ILocationChangeCallback otc){
        if (mListOTCs == null)
            mListOTCs = new ArrayList<>();

        mListOTCs.add(otc);
    }

    public void addLocalLocationChangeCallback(Context context, ILocationChangeCallback changedCallback){
        if (mOnLocationChangedLocalCallbacks == null)
            mOnLocationChangedLocalCallbacks = new HashMap<>();

        if (mOnLocationChangedLocalCallbacks.containsKey(context.getClass().getSimpleName()))
            mOnLocationChangedLocalCallbacks.get(context.getClass().getSimpleName()).add(changedCallback);
        else {
            List<ILocationChangeCallback> list = new ArrayList<>();
            list.add(changedCallback);
            mOnLocationChangedLocalCallbacks.put(context.getClass().getSimpleName(), list);
        }
    }

    public void clearLocalLocationChangeCallbacks(Context context){
        if (mOnLocationChangedLocalCallbacks != null)
            mOnLocationChangedLocalCallbacks.remove(context.getClass().getSimpleName());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService(){
            return LocationService.this;
        }
    }

    private class LocationServiceListener implements LocationListener {

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            requestLocationUpdates();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "onStatusChanged: Change in GPS detected");
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Intent intent = new Intent();
            intent.setAction(BROADCAST_ACTION_PROVIDER_DISABLED);
            sendBroadcast(intent);
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (Constants.DEBUG_MODE)
                Log.d(TAG, "onLocationChanged: Location changed : " + location);

            Notification notification = new NotificationCompat.Builder(LocationService.this, Constants.Notification_Channel_ID)
                    .setContentTitle(notificationTitle)
                    .setContentText("Location received at " + DateTimeUtil.getCurrentDateTime(DateTimeUtil.defaultTimeOnlyFormat))
                    .setSmallIcon(R.drawable.ic_location)
                    .build();

            NotificationManagerCompat
                    .from(LocationService.this).notify(SERVICE_NOTIFICATION_ID, notification);

            Intent intent = new Intent();
            intent.setAction(BROADCAST_ACTION_LOCATION_CHANGED);
            intent.putExtra(BROADCAST_EXTRA_LOCATION_DATA, location);
            sendBroadcast(intent);

            // Executing OTC
            if (mListOTCs != null && !mListOTCs.isEmpty()) {
                StaticUtils.getHandler().post(()-> {
                    for (int i=0; i < mListOTCs.size(); i++) {
                        mListOTCs.get(i).onLocationChange(location);
                    }
                    mListOTCs.clear();
                });
            }

            //Executing Local Callbacks
            if (mOnLocationChangedLocalCallbacks != null && !mOnLocationChangedLocalCallbacks.isEmpty()) {
                StaticUtils.getHandler().post(()-> {
                    for (String groupId : mOnLocationChangedLocalCallbacks.keySet()) {
                        if (!mOnLocationChangedLocalCallbacks.get(groupId).isEmpty()) {
                            for (ILocationChangeCallback callback : mOnLocationChangedLocalCallbacks.get(groupId)) {
                                callback.onLocationChange(location);
                            }
                        }
                    }
                });
            }

            //Executing Global Callbacks
            if (mOnLocationChangedGlobalCallbacks != null && !mOnLocationChangedGlobalCallbacks.isEmpty()){
                StaticUtils.getHandler().post(()-> {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (String callbackIndex : mOnLocationChangedGlobalCallbacks.keySet()){
                                mOnLocationChangedGlobalCallbacks.get(callbackIndex).onLocationChange(location);
                            }
                        }
                    })).start();
                });
            }
        }
    }
}
