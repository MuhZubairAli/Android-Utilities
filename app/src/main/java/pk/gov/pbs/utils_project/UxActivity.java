package pk.gov.pbs.utils_project;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import pk.gov.pbs.utils.CustomActivity;
import pk.gov.pbs.utils.ExceptionReporter;
import pk.gov.pbs.utils.FileManager;
import pk.gov.pbs.utils.location.LocationService;

public class UxActivity extends CustomActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ux);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((TextView) findViewById(R.id.tvApiLevel)).setText(Utils.getDeviceOS());
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
            mUXToolkit.showAlertDialogue("created multiple files and directories in Utils/ZipTest/files/");
        });

        findViewById(R.id.btnZipFiles).setOnClickListener((view)->{
            try {
                FileManager.pathToDirectory("Utils", "ZipTest", "files").inPublic().compress();
                getUXToolkit().showToast("ZipTest/files compressed successfully");
            } catch (IOException e) {
                getUXToolkit().showToast("failed to compress ZipTest/files");
                ExceptionReporter.handle(e);
            }
        });

        findViewById(R.id.btnUnzipFiles).setOnClickListener((view)->{
            try {
                FileManager.pathToDirectory("Utils", "ZipTest", "files.zip").inPublic().decompress();
                getUXToolkit().showToast("Decompressed successfully");
            } catch (IOException e) {
                getUXToolkit().showToast("Failed to decompress");
                ExceptionReporter.handle(e);
            }
        });
    }

    private String generateText(){
        String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus mollis ac metus nec vehicula. Curabitur mollis purus tincidunt sapien dapibus, a pulvinar purus auctor. Donec vitae magna varius odio ornare molestie sed a nibh. Sed non imperdiet ipsum. Duis iaculis viverra augue, id condimentum orci finibus ut. Duis velit felis, vehicula id tristique vel, condimentum sit amet metus. Nullam quis malesuada nibh. Donec mattis, lectus et iaculis imperdiet, turpis neque vulputate libero, vel elementum tellus libero in ligula. Phasellus venenatis eget turpis in facilisis. Maecenas aliquet, metus id pulvinar cursus, dui mauris suscipit nibh, a vehicula justo orci quis augue. Proin quis urna nulla.\n" +
                "\n" +
                "Nullam pharetra cursus ipsum, ultrices hendrerit velit convallis sed. Mauris gravida convallis eleifend. Aliquam erat volutpat. Sed tincidunt, purus id ultrices viverra, risus lorem blandit ante, sit amet efficitur ligula elit at leo. Nam sit amet efficitur elit, sit amet posuere urna. Vestibulum eget velit auctor arcu sollicitudin tempor. Pellentesque molestie erat eu augue aliquet porttitor. Nulla euismod et justo a tristique.\n" +
                "\n" +
                "Nam et purus dignissim, scelerisque dui vel, convallis dui. Aliquam sodales aliquet dui, nec interdum libero varius quis. Proin non iaculis ex. Donec urna elit, mollis sed eleifend sit amet, pharetra at felis. Ut sit amet sem nulla. In venenatis lorem dolor, ultrices ornare ante tempor eu. Sed bibendum ipsum in leo aliquam pharetra. Nulla tempor ex eu dolor ultrices euismod. Quisque iaculis tincidunt vehicula. Suspendisse tortor justo, vestibulum nec bibendum ut, scelerisque vel est. Sed quam sapien, faucibus ac laoreet eu, consequat eget quam. Aenean tincidunt tellus sed enim feugiat porta. Integer pellentesque vestibulum aliquet.\n" +
                "\n" +
                "Interdum et malesuada fames ac ante ipsum primis in faucibus. Suspendisse sed neque augue. Vivamus elementum sed risus non elementum. Fusce efficitur, nisl eu vulputate semper, turpis lorem dictum odio, tempor tristique orci libero quis nisi. Vivamus lorem libero, convallis non quam quis, interdum maximus velit. Etiam risus urna, commodo ut purus sed, scelerisque efficitur lorem. Ut ex est, fermentum ac velit tempor, bibendum sollicitudin sapien. Ut pretium ante eu rutrum consequat.\n" +
                "\n" +
                "Sed lobortis felis at orci volutpat, at blandit odio tempor. Mauris ullamcorper odio sit amet dui tempus ultricies. Sed eu blandit arcu. In cursus massa tellus, non pellentesque nisl condimentum nec. In cursus mi at sem iaculis feugiat. Aenean mattis ante sed nibh laoreet semper. Nullam suscipit ultrices nisl. Aenean posuere eros est, quis porttitor magna bibendum a. Proin ac bibendum enim. Integer volutpat id turpis in feugiat. Sed felis turpis, finibus eget nulla eget, mattis dictum tellus. Sed et nisl sem. Nulla varius rutrum scelerisque. In malesuada metus quis libero blandit pharetra. Integer lorem tellus, eleifend sit amet risus vel, porta faucibus eros.";
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < Math.random()*1000; i++){
            sb.append(text);
        }
        return sb.toString();
    }
}