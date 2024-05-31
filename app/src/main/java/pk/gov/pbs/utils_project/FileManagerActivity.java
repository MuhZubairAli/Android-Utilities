package pk.gov.pbs.utils_project;

import static pk.gov.pbs.utils_project.Utils.generateText;

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
import java.io.IOException;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.ExceptionReporter;
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

        findViewById(R.id.btnCreateFiles).setOnClickListener((view)->{
            for (int i = 0; i < 5; i++){
                FileManager.pathToFile("Utils","ZipTest", "files", "file_"+i+".txt").inPublic().append(generateText());
            }
            for (int i = 0; i < 10; i++){
                FileManager.pathToFile("Utils","ZipTest", "files", "more", "file_"+i+".txt").inPublic().append(generateText());
            }
            FileManager.pathToDirectory("Utils","ZipTest", "files", "empty").inPublic().createIfNotExists();
            FileManager.pathToDirectory("Utils","ZipTest", "files", "empty2").inPublic().createIfNotExists();
            FileManager.pathToDirectory("Utils","ZipTest", "files", "empty3").inPublic().createIfNotExists();
            mUXToolkit.alert("created multiple files and directories in Utils/ZipTest/files/");
        });

        findViewById(R.id.btnZipFiles).setOnClickListener((view)->{
            try {
                FileManager.pathToDirectory("Utils", "ZipTest", "files").inPublic().compress();
                getUXToolkit().toast("ZipTest/files compressed successfully");
            } catch (IOException e) {
                getUXToolkit().toast("failed to compress ZipTest/files");
                ExceptionReporter.handle(e);
            }
        });

        findViewById(R.id.btnUnzipFiles).setOnClickListener((view)->{
            try {
                FileManager.pathToDirectory("Utils", "ZipTest", "files.zip").inPublic().decompress();
                getUXToolkit().toast("Decompressed successfully");
            } catch (IOException e) {
                getUXToolkit().toast("Failed to decompress");
                ExceptionReporter.handle(e);
            }
        });
    }

    public void verifyPermissions(View view) {
        if (FileManager.hasAllPermissions(this))
            mUXToolkit.toast("Has all required permissions for storage access");
        else
            FileManager.requestAllPermissions(this);
    }

    private void showRootFiles() {
        File root = Environment.getExternalStoragePublicDirectory("Documents").getParentFile();
        if (root != null){
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    addFileRow(file);
                }
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
                        .pathToFile("Utils","myText.txt")
                        .inPublic()
                        .write(content)
        )
            mUXToolkit.toast("External File Created");
    }

    public void readFileExternal(View view) {
        String content = FileManager.pathToFile("Utils","myText.txt").inPublic().read();
        mUXToolkit.alert("External Public File", content);
    }

    public void createFilePrivate(View view) {
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(
                mFileManager.writeFileString(
                mFileManager.getFileExternalPrivate("Utils","internal.txt"),
                content, MODE_APPEND)
        )
            mUXToolkit.toast("External Private File Created");
    }

    public void readFilePrivate(View view) {
        String content = mFileManager.readFileString(
                mFileManager.getFileExternalPrivate("Utils","internal.txt")
        );
        mUXToolkit.alert("External Private File", content);
    }
    public void createFileInternal(View view) {
        String content = ((EditText) findViewById(R.id.etText)).getText().toString();
        if(mFileManager.writeFileInternal("internal.txt", content, MODE_APPEND))
            mUXToolkit.toast("Internal File Created");
    }

    public void readFileInternal(View view) {
        String content = mFileManager.readFileInternal("internal.txt");
        mUXToolkit.alert("Internal Public File", content);
    }

    public void debug(View view) {
        String content = "some random content here, for testing new internal apis...\n";
        FileManager.pathToFile("Utils","Test","test.txt").inPublic().append(content);
        FileManager.pathToFile("Utils","Test","test.txt").inPrivate().append(content);
        FileManager.pathToFile("Example", "1.txt").inPrivateCache().append(content);
        FileManager.pathToFile("Utils", "Example", "2.txt").inPrivateCache().append(content);
        FileManager.pathToFile("Utils", "3.txt").inPrivateCache().append(content);

        FileManager.pathToFile("Utils", "Test", "test.txt").inInternal().append(content);
        FileManager.pathToFile("Utils", "Test", "text.txt").inInternal().append(content);
        FileManager.pathToFile("cache.txt").inInternalCache().append(content);
        FileManager.pathToFile("Utils", "Example","1.txt").inInternalCache().append(content);
        FileManager.pathToFile("Utils", "Example","2.txt").inInternalCache().append(content);
        FileManager.pathToFile("Utils", "Example","3.txt").inInternalCache().append(content);
        mUXToolkit.toast("Debug completed, files created in all areas");
    }
}