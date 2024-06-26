package pk.gov.pbs.utils_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.ExceptionReporter;

public class MainActivity extends CustomActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());
        findViewById(R.id.btnPermissions).setOnClickListener(v -> {
            startActivity(new Intent(this, PermissionActivity.class));
        });

        findViewById(R.id.btnFileManager).setOnClickListener(v -> {
            startActivity(new Intent(this, FileManagerActivity.class));
        });

        findViewById(R.id.btnText).setOnClickListener(v -> {
            startActivity(new Intent(this, TextActivity.class));
        });

        findViewById(R.id.btnLocation).setOnClickListener(v -> {
            startActivity(new Intent(this, LocationActivity.class));
        });

        findViewById(R.id.btnUiux).setOnClickListener(v -> {
            startActivity(new Intent(this, UxActivity.class));
        });
    }
}