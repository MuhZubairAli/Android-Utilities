package pk.gov.pbs.utils_project;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.FileManager;

public class FileManagerActivity extends CustomActivity {
    FileManager mFileManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_file_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());
        mFileManager = new FileManager(this);
    }

    public void verifyPermissions(View view) {
        if (FileManager.hasPermissions(this))
            mUXToolkit.showToast("Has all required permissions for storage access");
        else
            FileManager.requestPermissions(this);
    }

    public void createFileExternal(View view) {

    }

    public void createFileInternal(View view) {
    }

    public void readFileExternal(View view) {
    }
}