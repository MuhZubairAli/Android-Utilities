package pk.gov.pbs.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

//Todo: Add functionality to also upload error detail when endpoint has been setup
public class ExceptionReporter {
    private static final String TAG = "ExceptionReporter";
    private static FileManager.DirectoryOperator LOGS_DIR;
    private static FileManager.FileOperator LOGS_FILE;
    private static final String LOGS_FILE_NAME = "default.log";
    private static final String LOGS_DIR_NAME = ".logs";

    public static void handle(Throwable exception){
        log(exception);
        printStackTrace(exception);
    }

    public static void log(Throwable exception) {
        try {
            FileManager.FileOperator logOperator = getLogsFileOperator();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            logOperator.append(sw.toString());
            logOperator.append("\n ------------------------------- \n");
        } catch (IOException e) {
            // Not using ExceptionReporter.handle() because that uses this method to log the exception
            // if I use that, it will cause infinite loop
            Log.e(TAG, "log: failed to log exception to file", e);
        }
    }

    protected static FileManager.FileOperator getLogsFileOperator() throws IOException {
        if (LOGS_DIR == null) {
            File logDir = new File(Environment.getExternalStorageDirectory(), LOGS_DIR_NAME);
            LOGS_DIR = FileManager.directory(logDir);
            LOGS_DIR.createIfNotExists();
        }

        if (LOGS_FILE == null) {
            File logFile = new File(LOGS_DIR.get(), LOGS_FILE_NAME);
            LOGS_FILE = FileManager.file(logFile);
            LOGS_FILE.createIfNotExists();
        }
        return LOGS_FILE;
    }

    private static void printStackTrace(Throwable exception){
        if (Constants.DEBUG_MODE) {
            throw new RuntimeException(exception);
        }
    }
}
