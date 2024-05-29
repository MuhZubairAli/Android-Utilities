package pk.gov.pbs.utils;

import static pk.gov.pbs.utils.UXToolkit.CommonAlerts.showAlertAppPermissionsSetting;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
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

        // Adding STORAGE_MANGER_PERMISSION request handler because FileManager utility is provided by default
        mPermissions.addAll(Arrays.asList(FileManager.getPermissionsRequired()));
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
                            mUXToolkit.showConfirmDialogue(
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
                                showAlertAppPermissionsSetting(getUXToolkit());
                        }
                    } else if (deniedPermission.isEmpty() && !permissions.isEmpty()) {
                        requestSpecialPermissions();
                    }
                });
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

            mUXToolkit.showConfirmDialogue(
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
