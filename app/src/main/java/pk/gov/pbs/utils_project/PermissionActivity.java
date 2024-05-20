package pk.gov.pbs.utils_project;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.CustomActivity;

public class PermissionActivity extends CustomActivity {
    TableLayout tblPerms;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permission);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        checkAllPermissions();
        tblPerms = findViewById(R.id.tblPerms);
        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());
        showPermissionsTable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showPermissionsTable();
    }

    private void showPermissionsTable(){
        tblPerms.removeAllViewsInLayout();
        for (String perm : getAllPermissions()) {
            addPermissionRow(perm, getStatusLabel(perm));
        }
    }
    private void addPermissionRow(String perm, String status) {
        TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.perm_row, tblPerms, false);
        ((TextView) row.findViewById(R.id.tv_1)).setText(perm.replace("android.permission.", ""));
        ((TextView) row.findViewById(R.id.tv_2)).setText(status);
        tblPerms.addView(row);
    }

    private String getStatusLabel(String perm) {
        int status = ActivityCompat.checkSelfPermission(this, perm);
        return status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED";
    }
}