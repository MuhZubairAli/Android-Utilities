package pk.gov.pbs.utils_project;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.FileManager;

public class FileManagerActivity extends CustomActivity {
    FileManager mFileManager;
    TableLayout tblFiles;

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
        tblFiles = findViewById(R.id.tblExternalFiles);
        showRootFiles();
    }

    public void verifyPermissions(View view) {
        if (FileManager.hasPermissions(this))
            mUXToolkit.showToast("Has all required permissions for storage access");
        else
            FileManager.requestPermissions(this);
    }

    private void showRootFiles() {
        File root = Environment.getExternalStoragePublicDirectory("Documents").getParentFile();
        if (root != null){
            File[] files = root.listFiles();
            assert files != null;
            for (File file : files) {
                addFileRow(file);
            }
        }
    }


    private void addFileRow(File file) {
        TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.perm_row, tblFiles, false);
        ((TextView) row.findViewById(R.id.tv_1)).setText(file.getAbsolutePath());
        ((TextView) row.findViewById(R.id.tv_2)).setText(file.isDirectory() ? "Directory" : "File");
        tblFiles.addView(row);
    }


    public void createFileExternal(View view) {
        String content = ((EditText) findViewById(R.id.etTextExternal)).getText().toString();
        mFileManager.writeFile(
                mFileManager.getExternalPublicDirectory("Utils","external","files")
                ,"external.txt"
                ,content
        );
    }

    public void createFileInternal(View view) {
    }

    public void readFileExternal(View view) {
    }
}