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
        if (
                FileManager
                        .pathToFile("UtilNewWay","World","External","Test.txt")
                        .inPublic()
                        .write(content)
        )
            mUXToolkit.showToast("External File Created");
    }

    public void readFileExternal(View view) {
        String content = FileManager.pathToFile("UtilNewWay","World","External","Test.txt").inPublic().read();
        mUXToolkit.showAlertDialogue("External Public File", content);
    }

    public void createFilePrivate(View view) {
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(
                mFileManager.writeFileString(
                mFileManager.getFileExternalPrivate("Hello","World","internal.txt"),
                content, MODE_APPEND)
        )
            mUXToolkit.showToast("External Private File Created");
    }

    public void readFilePrivate(View view) {
        String content = mFileManager.readFileString(
                mFileManager.getFileExternalPrivate("Test","Private","File","internal.txt")
        );
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

    public void debug(View view) {
        String content = "some random content here, for testing new internal apis...\n";
        FileManager.pathToFile("Utils","Test","test.txt").inPublic().append(content);
        FileManager.pathToFile("Utils","Test","test.txt").inPrivate().append(content);
        FileManager.pathToFile("Example", "1.txt").inPrivateCache().append(content);
        FileManager.pathToFile("Example", "2.txt").inPrivateCache().append(content);
        FileManager.pathToFile("Test", "3.txt").inPrivateCache().append(content);

        FileManager.pathToFile("Test", "Utils", "test.txt").inInternal().append(content);
        FileManager.pathToFile("Test", "text.txt").inInternal().append(content);
        FileManager.pathToFile("cache.txt").inInternalCache().append(content);
        FileManager.pathToFile("Example","1.txt").inInternalCache().append(content);
        FileManager.pathToFile("Example","2.txt").inInternalCache().append(content);
        FileManager.pathToFile("Example","3.txt").inInternalCache().append(content);

        mUXToolkit.showToast("Debug completed, files created in all areas");
    }
}