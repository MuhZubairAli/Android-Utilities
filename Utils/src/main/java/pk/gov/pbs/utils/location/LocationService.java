package pk.gov.pbs.utils.location;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.WeakHashMap;

import pk.gov.pbs.utils.Constants;
import pk.gov.pbs.utils.DateTimeUtil;
import pk.gov.pbs.utils.ExceptionReporter;
import pk.gov.pbs.utils.R;
import pk.gov.pbs.utils.StaticUtils;
import pk.gov.pbs.utils.SystemUtils;
import pk.gov.pbs.utils.exceptions.InvalidIndexException;

public class LocationService extends Service {
    private static final String TAG = ":Utils] LocationService";
    public static final String BROADCAST_ACTION_PROVIDER_DISABLED =
            LocationService.class.getCanonicalName()+".ProviderDisabled";
    public static final String BROADCAST_ACTION_LOCATION_CHANGED =
            LocationService.class.getCanonicalName()+".LocationChanged";
    public static final String BROADCAST_EXTRA_LOCATION_DATA =
            LocationService.class.getCanonicalName()+".CurrentLocation";
    public static final String BROADCAST_EXTRA_NOTIFICATION_INTENT =
            LocationService.class.getCanonicalName()+".SrvPndIntent";
    public static final int PERMISSION_REQUEST_CODE = 10;
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static LocationService instance;
    private List<ILocationChangeCallback> mListOTCs;
    private WeakReference<LocationServiceBinder> mBinder;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5;

    protected LocationManager mLocationManager;
    protected LocationListener mLocationListener;
    private final String notificationTitle = "Location Service";
    private PendingIntent mNotificationIntent;

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
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "onStartCommand: Location service started");

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, Constants.Notification_Channel_ID)
                .setContentTitle(notificationTitle)
                .setContentText("Finding device location...")
                .setSmallIcon(R.drawable.ic_auto_mode);

        Bundle bundle;
        if (intent != null && (bundle = intent.getExtras()) != null && bundle.containsKey(BROADCAST_EXTRA_NOTIFICATION_INTENT)){
            mNotificationIntent = bundle.getParcelable(BROADCAST_EXTRA_NOTIFICATION_INTENT);
            if (mNotificationIntent != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mNotificationIntent.isForegroundService();
            }
            notification.setContentIntent(mNotificationIntent);
        }

        startForeground(SERVICE_NOTIFICATION_ID, notification.build());
        if(!requestLocationUpdates())
            ExceptionReporter.handle(new Exception("Failed to request location updates"));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(mLocationListener);
        instance = null;
    }

    public static void pause(){
        if (isRunning())
            getInstance().pauseLocationUpdates();
    }

    public static void resume(){
        if (isRunning())
            getInstance().resumeLocationUpdates();
    }

    public static void stop(Context context){
        Intent intent = new Intent(context, LocationService.class);
        context.stopService(intent);
    }

    public static LocationService getInstance(){
        return instance;
    }

    public static boolean isRunning(){
        return instance != null;
    }

    public static void start(Context context){
        start(context, null);
    }

    public synchronized static void start(Context context, Class<? extends Context> pendingIntentClass){
        if (LocationService.hasRequiredPermissions(context)) {
            Intent intent = new Intent(context, LocationService.class);
            if (pendingIntentClass != null){
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        LocationService.BROADCAST_EXTRA_NOTIFICATION_INTENT,
                        PendingIntent.getActivity(
                                context,
                                0,
                                new Intent(context, pendingIntentClass),
                                PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                        )
                );
                intent.putExtras(bundle);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else
                context.startService(intent);
        } else
            ExceptionReporter.handle(new Exception("can not start LocationService, Permissions pertaining to LocationService not granted"));
    }

    protected boolean requestLocationUpdates() {
        if (Constants.DEBUG_MODE)
            Log.d(TAG, "requestLocationUpdates]: requesting location updates");

        if (hasRequiredPermissions(this)) {
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
            ExceptionReporter.handle(new Exception("required permissions for LocationService not granted"));
        }
        return false;
    }

    public void resumeLocationUpdates() {
        if(!requestLocationUpdates())
            ExceptionReporter.handle(
                    new Exception("Failed to request location updates, either providers are disabled or permissions are not granted")
            );
    }

    public void pauseLocationUpdates() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public boolean isGPSEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * get available location, first tries to get current location
     * if current location is null, tries to get last known location
     * @return available location
     */
    public Location getAvailableLocation(){
        Location location = getLocation();
        if (location == null)
            location = getLastKnownLocation();
        return location;
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        requestLocationUpdates(); // onBind startRequesting updates in case location updates are paused
        mBinder = new WeakReference<>(new LocationServiceBinder());
        return mBinder.get();
    }

    public final class LocationServiceBinder extends Binder {
        private List<ILocationChangeCallback> mCallbacks;
        public LocationService getService(){
            return LocationService.this;
        }

        public void setLocationChangeCallbacks(List<ILocationChangeCallback> callbacks){
            mCallbacks = callbacks;
        }

        private List<ILocationChangeCallback> getLocationChangeCallbacks() {
            return mCallbacks;
        }

        private boolean hasCallbacks(){
            return mCallbacks != null && !mCallbacks.isEmpty();
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

            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(LocationService.this, Constants.Notification_Channel_ID)
                    .setContentTitle(notificationTitle)
                    .setContentText("Location received at " + DateTimeUtil.getCurrentDateTime(DateTimeUtil.defaultTimeOnlyFormat))
                    .setSmallIcon(R.drawable.ic_location);

            if (mNotificationIntent != null) {
                nBuilder.setContentIntent(mNotificationIntent);
            }

            NotificationManagerCompat
                    .from(LocationService.this)
                    .notify(SERVICE_NOTIFICATION_ID, nBuilder.build());

            Intent intent = new Intent(BROADCAST_ACTION_LOCATION_CHANGED);
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

            if (mBinder != null && mBinder.get() != null && mBinder.get().hasCallbacks()){
                StaticUtils.getHandler().post(() -> {
                    for (ILocationChangeCallback callback : mBinder.get().getLocationChangeCallbacks()) {
                        callback.onLocationChange(location);
                    }
                });
            }
        }
    }
}
