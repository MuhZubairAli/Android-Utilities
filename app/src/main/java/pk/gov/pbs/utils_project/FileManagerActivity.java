package pk.gov.pbs.utils_project;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(
        mFileManager.writeFile(
                mFileManager.getFileExternalPublic("Test","File","Another","Test.txt")
                ,content
                , MODE_APPEND
        ))
            mUXToolkit.showToast("External File Created");
    }

    public void readFileExternal(View view) {
        String content = mFileManager.readFile(mFileManager.getFileExternalPublic("Test","File","Another","Test.txt"));
        mUXToolkit.showAlertDialogue("External Public File", content);
    }

    public void createFilePrivate(View view) {
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(mFileManager.writeFileExternalPrivate("Test", "Text", "file.txt", content, MODE_APPEND))
            mUXToolkit.showToast("External Private File Created");
    }

    public void readFilePrivate(View view) {
        String content = mFileManager.readFileExternalPrivate("Test", "Text", "internal.txt");
        mUXToolkit.showAlertDialogue("External Private File", content);
    }
    public void createFileInternal(View view) {
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(mFileManager.writeFileInternal("internal.txt", content, MODE_APPEND))
            mUXToolkit.showToast("Internal File Created");
    }

    public void readFileInternal(View view) {
        String content = mFileManager.readFileInternal("internal.txt");
        mUXToolkit.showAlertDialogue("Internal Public File", content);
    }

}