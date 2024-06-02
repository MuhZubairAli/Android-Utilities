package pk.gov.pbs.utils;

import static pk.gov.pbs.utils.UXToolkit.CommonAlerts.showAppPermissionsSetting;
import static pk.gov.pbs.utils.UXToolkit.CommonAlerts.showLocationSettings;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import pk.gov.pbs.utils.location.ILocationChangeCallback;
import pk.gov.pbs.utils.location.LocationService;

public abstract class CustomActivity extends AppCompatActivity {
    private static final String TAG = ":Utils] CustomActivity";
    private static final int mSystemControlsHideFlags =
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private ActionBar actionBar;
    private final List<String> mPermissions = new ArrayList<>();
    private final Map<String, PermissionRequestHandler> mSpecialPermissionsHandlers = new HashMap<>(2);
    private final Stack<PermissionRequestHandler> mSpecialPermissions = new Stack<>();
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    private Runnable mAfterLocationServiceStartCallback;
    private LocationService mLocationService = null;
    private ServiceConnection mLocationServiceConnection = null;
    private BroadcastReceiver GPS_PROVIDER_ACCESS = null;
    private final List<ILocationChangeCallback> mLocationChangeCallbacks = new ArrayList<>();
    private static byte mLocationAttachAttempts = 0;

    private LayoutInflater mLayoutInflater;
    protected UXToolkit mUXToolkit;
    protected FileManager mFileManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!mSpecialPermissions.isEmpty()){
            mSpecialPermissions.pop().askPermission();
        }

        if (LocationService.isRunning()) {
            if (!LocationService.getInstance().isNetworkProviderEnabled() && !LocationService.getInstance().isGpsProviderEnabled())
                showLocationSettings(getUXToolkit());

            if (mLocationService == null)
                mLocationService = LocationService.getInstance();
        }
    }

    @Override
    protected void onDestroy() {
        if (LocationService.isRunning()) {
            unbindLocationService();
        }
        super.onDestroy();
    }

    @NonNull
    public LayoutInflater getLayoutInflater(){
        if (mLayoutInflater == null)
            mLayoutInflater = LayoutInflater.from(this);
        return mLayoutInflater;
    }
    public UXToolkit getUXToolkit(){
        return mUXToolkit;
    }

    public FileManager getFileManager(){
        return mFileManager;
    }

//    For android 11 and below, onRequestPermissionsResult() will be called
//    protected void requestPermissions(int requestCode, String[] permissions){
//        ActivityCompat.requestPermissions(
//                this,
//                permissions,
//                requestCode
//        );
//    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        List<String> askAgain = new ArrayList<>();
//        boolean missingPermission = false;
//        for (int i = 0; i < grantResults.length; i++) {
//            missingPermission = missingPermission | grantResults[i] == PackageManager.PERMISSION_DENIED;
//            if (grantResults[i] == PackageManager.PERMISSION_DENIED)
//                askAgain.add(permissions[i]);
//        }
//
//        if (requestCode == PERMISSIONS_REQUEST_FIRST && missingPermission) {
//            boolean showRationale = false;
//            for (String perm : askAgain)
//                showRationale = showRationale | ActivityCompat.shouldShowRequestPermissionRationale(this, perm);
//
//            if (showRationale) {
//                mUXToolkit.showConfirmDialogue(
//                        getString(R.string.alert_dialog_permission_require_all_title)
//                        , getString(R.string.alert_dialog_permission_require_all_message)
//                        , getString(R.string.label_btn_request_again)
//                        , "Cancel"
//                        , new UXEventListeners.ConfirmDialogueEventsListener(){
//
//                            @Override
//                            public void onOK(DialogInterface dialog, int which) {
//                                requestPermissions(PERMISSIONS_REQUEST_SECOND, askAgain.toArray(new String[0]));
//                            }
//
//                            @Override
//                            public void onCancel(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                            }
//                        });
//            } else {
//                // No explanation needed, we can request the permissions..
//                requestPermissions(PERMISSIONS_REQUEST_SECOND, askAgain.toArray(new String[0]));
//            }
//        } else if (requestCode == PERMISSIONS_REQUEST_SECOND && missingPermission) {
//            showAlertAppPermissionsSetting();
//        }
//
//        if (!mSpecialPermissions.isEmpty()){
//            requestSpecialPermissions();
//        }
//    }

    private void initialize(){
        mUXToolkit = new UXToolkit(this);
        mFileManager = FileManager.getInstance(this);

        mPermissions.add(Manifest.permission.READ_PHONE_STATE);
        // Adding STORAGE_MANGER_PERMISSION request handler because FileManager utility is provided by default
        addPermissions(FileManager.getPermissionsRequired());
        addPermissions(LocationService.getPermissionsRequired());

        // Adding POST_NOTIFICATIONS request handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            addSpecialPermission(Manifest.permission.POST_NOTIFICATIONS, "Notification permission is required in order to Show Notifications from foreground services", new PermissionRequest() {
                @Override
                public boolean hasPermission() {
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    return notificationManager.areNotificationsEnabled();
                }

                @Override
                public void askPermission() {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                }
            });
        }
        // Adding MANAGE_EXTERNAL_STORAGE request handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            addSpecialPermission(
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    , "In order to read and write files to external storage, Permission to manage all files is required, Please enable the option of 'Allow access to manage all files' on next screen."
                    , new PermissionRequest() {
                        @Override
                        public boolean hasPermission() {
                            return Environment.isExternalStorageManager();
                        }

                        @Override
                        public void askPermission() {
                            Uri uri = Uri.parse("package:" + CustomActivity.this.getPackageName());
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                            startActivity(intent);
                        }
                    }
            );
        }
        // Adding BACKGROUND_LOCATION request handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addSpecialPermission(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    "This application requires background location to work properly, Please enable the option of 'Allow all the time' on Location Permissions screen."
                    , new PermissionRequest() {
                        @Override
                        public boolean hasPermission() {
                            return
                                    (
                                        ActivityCompat.checkSelfPermission(CustomActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        || ActivityCompat.checkSelfPermission(CustomActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    ) && ActivityCompat.checkSelfPermission(CustomActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                        }

                        @Override
                        public void askPermission() {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
                        }
                    }
            );
        }

        requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean showRationale = false;
                    List<String> deniedPermission = new ArrayList<>();
                    for (String perm : permissions.keySet()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean show = shouldShowRequestPermissionRationale(perm);
                            showRationale |= show;
                        }

                        if (Boolean.FALSE.equals(permissions.get(perm)))
                            deniedPermission.add(perm);
                    }

                    if (!deniedPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (showRationale) {
                            getUXToolkit().confirm(
                                "Permission Required"
                                , "All requested permissions are required to work properly, Kindly grant all the requested permissions"
                                , "Open Permissions Settings"
                                , "Request Again"
                                , new UXEvent.ConfirmDialogue() {
                                    @Override
                                    public void onCancel(DialogInterface dialog, int which) {
                                        requestPermissions(deniedPermission.toArray(new String[0]));
                                    }

                                    @Override
                                    public void onOK(DialogInterface dialog, int which) {
                                        final Intent i = new Intent();
                                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        i.addCategory(Intent.CATEGORY_DEFAULT);
                                        i.setData(Uri.parse("package:" + getPackageName()));
                                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                        startActivity(i);
                                    }
                                });
                        } else {
                            if (permissions.size() == 1 && permissions.containsKey(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
                                requestSpecialPermissions();
                            else
                                showAppPermissionsSetting(getUXToolkit());
                        }
                    } else if (deniedPermission.isEmpty() && !permissions.isEmpty()) {
                        requestSpecialPermissions();
                    }
                });

        GPS_PROVIDER_ACCESS = new ProviderDisabledReceiver();
        IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(GPS_PROVIDER_ACCESS, intentFilter, RECEIVER_NOT_EXPORTED);
        } else
            registerReceiver(GPS_PROVIDER_ACCESS, intentFilter);
    }

    protected String[] getAllPermissions(){
        return mPermissions.toArray(new String[0]);
    }

    protected String[] getSpecialPermissions(){
        return mSpecialPermissionsHandlers.keySet().toArray(new String[0]);
    }

    protected String[] getDeniedPermissions(){
        List<String> denied = new ArrayList<>();

        for (String perm : getAllPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_DENIED)
                denied.add(perm);
        }

        if (!denied.isEmpty()){
            return denied.toArray(new String[0]);
        }

        return null;
    }

    private String[] getPendingPermissionsRequests(){
        String[] deniedPermissions = getDeniedPermissions();
        if (deniedPermissions == null || deniedPermissions.length == 0)
            return null;

        ArrayList<String> permission = new ArrayList<>(
                Arrays.asList(deniedPermissions)
        );

        permission.removeAll(Arrays.asList(getSpecialPermissions()));
        return permission.toArray(new String[0]);
    }

    protected boolean hasAllPermissions(){
        boolean has = true;
        for (String perm : getAllPermissions())
            has &= ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
        return has && hasSpecialPermissions();
    }

    protected boolean hasSpecialPermissions(){
        boolean has = true;
        if (!mSpecialPermissionsHandlers.isEmpty()) {
            for (String perm : mSpecialPermissionsHandlers.keySet()) {
                has &= mSpecialPermissionsHandlers.get(perm).permissionRequest.hasPermission();
            }
        }
        return has;
    }

    protected void checkAllPermissions() {
        String[] perms = getPendingPermissionsRequests();
        if (perms != null && perms.length > 0)
            requestPermissions(perms);
        else if (!hasAllPermissions())
            requestSpecialPermissions();
    }

    protected void requestPermissions(String[] permissions){
        requestPermissionLauncher.launch(permissions);
    }

    private void requestSpecialPermissions(){
        if (!mSpecialPermissionsHandlers.isEmpty()) {
            for (String key : mSpecialPermissionsHandlers.keySet()){
                PermissionRequestHandler handler = mSpecialPermissionsHandlers.get(key);
                if (handler != null) {
                    if (!handler.permissionRequest.hasPermission() && handler.status == PermissionRequestStatus.PROCEEDED)
                        handler.status = PermissionRequestStatus.DENIED;
                    else if(handler.permissionRequest.hasPermission())
                        handler.status = PermissionRequestStatus.GRANTED;

                    if (
                        !(
                            handler.status == PermissionRequestStatus.ASKED
                            || handler.status == PermissionRequestStatus.GRANTED
                        )
                        && !mSpecialPermissions.contains(handler)
                    )
                        mSpecialPermissions.push(mSpecialPermissionsHandlers.get(key));
                }
            }

            if (!mSpecialPermissions.isEmpty()) {
                mSpecialPermissions.pop().askPermission();
            }
        }
    }

    protected void addPermissions(String... permissions){
        if (permissions == null || permissions.length == 0)
            return;

        mPermissions.addAll(
                Arrays.asList(permissions)
        );
    }

    protected void addSpecialPermission(String permission, String explanation, PermissionRequest requestHandler){
        mSpecialPermissionsHandlers.put(
                permission,
                new PermissionRequestHandler(
                        permission,
                        explanation,
                        requestHandler
                )
        );
    }

    protected void setActivityTitle(@NonNull String title, @Nullable String subtitle){
        if(actionBar != null){
            Spanned htm = Html.fromHtml(title);
            ((TextView) actionBar.getCustomView().findViewById(R.id.tv_1)).setText(htm);
            if(subtitle != null)
                ((TextView) actionBar.getCustomView().findViewById(R.id.tv_2)).setText(subtitle);
            else
                actionBar.getCustomView().findViewById(R.id.tv_2).setVisibility(View.INVISIBLE);
        }{
            Objects.requireNonNull(getSupportActionBar())
                    .setTitle(title);
        }
    }

    protected void setActivityTitle(int title, int subtitle){
        setActivityTitle(getString(title),getString(subtitle));
    }

    protected void setActivityTitle(@NonNull String subtitle){
        setActivityTitle(getString(R.string.app_name),subtitle);
    }

    protected void setActivityTitle(int subtitle){
        setActivityTitle(getString(R.string.app_name),getString(subtitle));
    }

    protected void showActionBar(){
        try {
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (!actionBar.isShowing())
                    actionBar.show();
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                actionBar.setCustomView(R.layout.custom_action_bar);
            }
        } catch (NullPointerException npe) {
            ExceptionReporter.handle(npe);
        }
    }

    protected void hideActionBar(){
        try {
            actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (actionBar.isShowing())
                    actionBar.hide();
            }
            actionBar = null; //so that setActivityTitle() would not proceed
        } catch (NullPointerException npe) {
            ExceptionReporter.handle(npe);
        }
    }

    protected void showSystemControls(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        showActionBar();
    }

    protected void hideSystemControls(){
        if (getWindow().getDecorView().getSystemUiVisibility() != mSystemControlsHideFlags)
            getWindow().getDecorView().setSystemUiVisibility(mSystemControlsHideFlags);
        hideActionBar();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            getWindow().getDecorView().postDelayed(()->{
                getWindow().getDecorView().setSystemUiVisibility(mSystemControlsHideFlags);
            }, 3000);
        });
    }

    /**
     * This method start the LocationService and bind this activity with the service
     * in order to user addLocationChangedCallback(ILocationChangeCallback) use this method
     * to start the service. otherwise location change event do not execute the callbacks
     * and current location is only delivered by BroadcastReceiver
     * Please note that if service is running then it will always send new location to BroadcastReceivers
     * no matter an activity is bound with the service or not
     */
    protected void startLocationService() throws Exception {
        startLocationService(LocationService.Mode.IDLE,null);
    }

    /**
     * This method start the LocationService and bind this activity with the service
     * in order to user addLocationChangedCallback(ILocationChangeCallback) use this method
     * to start the service. otherwise location change event do not execute the callbacks
     * and current location is only delivered by BroadcastReceiver
     * @param notificationActivity activity for PendingIntent (for Service Notification)
     */
    protected void startLocationService(LocationService.Mode serviceMode, @Nullable Class<? extends Context> notificationActivity) throws Exception {
        Log.d(TAG, "startLocationService: Starting location service");
        if (mLocationServiceConnection == null) {
            mLocationServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
                    mLocationService = binder.getService();
                    binder.setLocationChangeCallbacks(mLocationChangeCallbacks);
                    if (!mLocationService.isNetworkProviderEnabled() && !mLocationService.isGpsProviderEnabled())
                        showLocationSettings(getUXToolkit());

                    if (mAfterLocationServiceStartCallback != null)
                        mAfterLocationServiceStartCallback.run();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mLocationService = null;
                }
            };
        }

        Intent intent = new Intent(this, LocationService.class);
        if (!LocationService.isRunning()) {
            if (notificationActivity != null) {
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
                intent.putExtra(LocationService.BROADCAST_EXTRA_SERVICE_MODE, serviceMode);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(intent);
                else
                    startService(intent);
            }
        }

        if (!bindService(intent, mLocationServiceConnection, Context.BIND_AUTO_CREATE)){
            throw new Exception("startLocationService] - Failed to bind to LocationService");
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
        if(mLocationService != null) {
            if (GPS_PROVIDER_ACCESS.isOrderedBroadcast()) {
                GPS_PROVIDER_ACCESS.clearAbortBroadcast();
                unregisterReceiver(GPS_PROVIDER_ACCESS);
            }

            if (mLocationServiceConnection != null)
                unbindService(mLocationServiceConnection);
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

    public void verifyCurrentLocation(ILocationChangeCallback callback) {
        if (mLocationService != null) {
            checkLocationAndRunCallback(callback);
        } else {
            try {
                startLocationService();
                mAfterLocationServiceStartCallback = () -> {
                    checkLocationAndRunCallback(callback);
                };
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
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

    public static class ProviderDisabledReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationService.BROADCAST_ACTION_PROVIDER_DISABLED)) {
                showLocationSettings(context);
            }
        }
    }

    protected class PermissionRequestHandler {
        private final String permissionRequestStatement;
        private final String mPermission;
        private final PermissionRequest permissionRequest;
        private PermissionRequestStatus status;
        public PermissionRequestHandler(String permission, String explanation, PermissionRequest callback){
            mPermission = permission;
            permissionRequestStatement = explanation;
            permissionRequest = callback;
            status = PermissionRequestStatus.ADDED;
        }
        public void askPermission() {
            status = permissionRequest.hasPermission() ?
                    PermissionRequestStatus.GRANTED :
                    PermissionRequestStatus.ASKED;

            if (
                    permissionRequest.hasPermission() &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(CustomActivity.this, mPermission)
            )
                return;

            mUXToolkit.confirm(
                    "Permission Required"
                    , permissionRequestStatement
                    , "Proceed"
                    , "Cancel"
                    , new UXEvent.ConfirmDialogue() {
                        @Override
                        public void onCancel(DialogInterface dialog, int which) {
                            status = PermissionRequestStatus.DENIED;
                            dialog.dismiss();
                        }

                        @Override
                        public void onOK(DialogInterface dialog, int which) {
                            status = PermissionRequestStatus.PROCEEDED;
                            permissionRequest.askPermission();
                        }
                    }
            );
        }
    }

    public interface PermissionRequest {
        boolean hasPermission();
        void askPermission();
    }

    private enum PermissionRequestStatus{
        ADDED, // when PermissionRequestHandler is added in specialPermissionsHandlers
        ASKED, // when PermissionRequestHandler is poped from mSpecialPermissions and askPermission() is called
        PROCEEDED, // when user click proceed on Permission Dialog
        GRANTED, // when user grant permission
        DENIED // when user click cancel on Permission Dialog
    }
}
