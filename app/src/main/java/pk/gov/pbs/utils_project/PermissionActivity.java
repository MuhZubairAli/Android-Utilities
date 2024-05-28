package pk.gov.pbs.utils_project;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.ExceptionReporter;
import pk.gov.pbs.utils.FileManager;
import pk.gov.pbs.utils.LocationActivity;

public class PermissionActivity extends LocationActivity {
    TableLayout tblPerms, tblPermsRequested;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PermissionActivity.super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permission);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        checkAllPermissions();
        tblPerms = findViewById(R.id.tblPerms);
        tblPermsRequested = findViewById(R.id.tblPermsRequested);
        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());
        showAppPermissionsTable();
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            checkAllPermissions();
            showAppPermissionsTable();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRequestedPermissionsTable();
        showAppPermissionsTable();
    }

    private void showRequestedPermissionsTable(){
        TableRow header = (TableRow) tblPermsRequested.getChildAt(0);
        tblPermsRequested.removeAllViews();
        tblPermsRequested.addView(header);

        for (String perm : getAllPermissions()) {
            addPermissionRow(tblPermsRequested, perm, getStatusLabel(perm));
        }

        for (String perm : getSpecialPermissions()) {
            addPermissionRow(tblPermsRequested, perm, getStatusLabel(perm));
        }
    }

    private void showAppPermissionsTable(){
        TableRow header = (TableRow) tblPerms.getChildAt(0);
        tblPerms.removeAllViews();
        tblPerms.addView(header);
        String[] permissions = null;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            permissions = info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            ExceptionReporter.handle(e);
        }
        if (permissions != null) {
            for (String perm : permissions) {
                addPermissionRow(tblPerms, perm, getStatusLabel(perm));
            }
        }
    }

    private void addPermissionRow(TableLayout container, String perm, String status) {
        TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.perm_row, tblPerms, false);
        ((TextView) row.findViewById(R.id.tv_1)).setText(perm.replace("android.permission.", ""));
        ((TextView) row.findViewById(R.id.tv_2)).setText(status);
        container.addView(row);
    }

    private String getStatusLabel(String perm) {
        if (perm.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            return FileManager.hasFileManagerPermission() ? "GRANTED" : "DENIED";
        }
        int status = ActivityCompat.checkSelfPermission(this, perm);
        return status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED";
    }
}