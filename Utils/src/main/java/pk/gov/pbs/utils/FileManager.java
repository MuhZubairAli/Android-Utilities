package pk.gov.pbs.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FileManager {
    private final String TAG = "FileManager";
    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 20;
    protected Context mContext;

    public FileManager(Context context){
        this.mContext = context;
    }

    public static String[] getPermissionsRequired(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
            };
        }

        return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    /**
     * Checks if the app has permission to write to device storage
     * For API >= 30 it verifies if has permission to manage all files
     *
     * Returns true if has all permissions other wise returns false which indicates
     * that one or more permissions regarding storage are not granted
     */
    public static boolean hasPermissions(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        }else {
            int write = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests for all storage related permissions
     * For API >= 30 it opens up the activity to allow current app the permission to manage all files
     */
    public static void requestPermissions(Activity context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!hasPermissions(context)) {
                requestForFileManagerPermission(context);
            }
        }

        if (!hasPermissions(context)) {
            ActivityCompat.requestPermissions(
                    context,
                    getPermissionsRequired(),
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }

    /**
     * For API >= 30 and API < 33 this method checks if current app has permission to manage all files
     * for other API return true by default because api < 30 don't need it
     * and API >= 33 it already has it
     * @return true if app has permission to manage all files, false otherwise
     */
    public static boolean hasFileManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * For API >= 30 this method opens screen for allowing current app to be
     * Manage Application for All Files Access Permission
     * This is required for CRUD operations
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestForFileManagerPermission(Context context){
        StaticUtils.getHandler().post(()->{
            Toast.makeText(context, "On API 30 and above permission to manage all files is required, Please enable the option of \'Allow access to manage all files\'.", Toast.LENGTH_SHORT).show();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()){
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    /**
     * Write file into internal cache storage of the app, it is not accessible via FileExplorer apps unless
     * your device is rooted
     * */
    public boolean writeFileInternalCache(String fileName, String data, int mode) {
        try {

            FileOutputStream fos = new FileOutputStream(new File(mContext.getCacheDir(), fileName));
            OutputStreamWriter out = new OutputStreamWriter(fos);

            if(mode == Context.MODE_APPEND) {
                out.append(data).append("\n");
            }else {
                out.write(data);
            }
            out.close();
            return true;
        } catch (Exception e) {
            ExceptionReporter.handle(e);
        }
        return false;
    }

    public String readFileInternalCache(String fileName) {

        try {
            FileInputStream fis = new FileInputStream(
                    new File(mContext.getCacheDir(), fileName)
            );
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean deleteFileInternalCache(String fileName){
        File file = new File(mContext.getCacheDir(), fileName);
        if (file.exists())
            return file.delete();
        return true;
    }


    /**
     * Write file into internal storage of the app, it is not accessible via FileExplorer apps unless
     * your device is rooted
     * @param mode :
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION  --> for share file.
     * */
    public boolean writeFileInternal(String fileName, String data, int mode) {
        try {
            FileOutputStream fos = mContext.openFileOutput(fileName, mode);
            OutputStreamWriter out = new OutputStreamWriter(fos);

            if(mode == Context.MODE_APPEND) {
                out.append(data).append("\n");
            }else {
                out.write(data);
            }
            out.close();
            return true;
        } catch (Exception e) {
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public String readFileInternal(String fileName) {

        try {
            FileInputStream fis = mContext.openFileInput(fileName);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean deleteFileInternal(String fileName){
        return mContext.deleteFile(fileName);
    }

    /* ******************************************************************************************** *
     *                                                                                              *
     *  - Handle file in private external storage in low api (below 18), don't require permission.  *
     *  - Don't be confused external storage with SD external card, since internal SD card is       *
     *  considered as external storage. And internal SD card is a default external storage.         *
     *                                                                                              *
     * ******************************************************************************************** */

    // Checks if external storage is available to at least read.
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    // Checks if external storage is available for read and write.
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageAvailable(){
        return isExternalStorageWritable() && isExternalStorageReadable();
    }

    /** Write to a public external directory.
     *  @param mode:
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION    --> share file.
     *  @param mainDir: representing the appropriate directory on the external storage ( Environment.DIRECTORY_MUSIC, ...)
     *  @param subFolder: usually an app name to distinguish with another app.
     *  @param fileName: ".nomedia" + fileName to hide it from MediaStore scanning.
     */
    public boolean writeFileExternalPublic(String mainDir, String subFolder, String fileName, String data, int mode){

        if(!hasPermissions(mContext) || !isExternalStorageAvailable()){
            ExceptionReporter.handle(new IOException("Either app do not have storage permissions, or external storage is not available"));
            return false;
        }

        // Get the directory for the user's public mainDir directory.
        String directory = getPathDirectoryExternalPublic(mainDir, subFolder);
        File folder = new File(directory);

        // If directory doesn't exist, create it.
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, fileName);
        try {
            if (!file.exists()){
                if(!file.createNewFile())
                    throw new IOException("Failed to create new file. " + file.getAbsolutePath());
            }

            FileOutputStream fos;
            if(mode == Context.MODE_APPEND) {
                fos = new FileOutputStream(file, true);
            }else {
                fos = new FileOutputStream(file);
            }

            // Instantiate a stream writer.
            OutputStreamWriter out = new OutputStreamWriter(fos);

            // Add data.
            if(mode == Context.MODE_APPEND) {
                out.append(data + "\n");
            }else {
                out.write(data);
            }
            out.close();
            return true;
        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public String readFileExternalPublic(String mainDir, String subFolder, String fileName) {

        try {

            String directory = getPathDirectoryExternalPublic(mainDir, subFolder);
            File folder = new File(directory);

            File file = new File(folder, fileName);

            // If file doesn't exist.
            if (!file.exists()) {
                ExceptionReporter.handle(new IOException("file does not exists " + mainDir + "/" + subFolder + "/" + fileName));
                return null;
            }

            FileInputStream fis = new FileInputStream(file);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean deleteFileExternalPublic(String mainDir, String subFolder, String fileName){

        String directory = getPathDirectoryExternalPublic(mainDir, subFolder);
        File folder = new File(directory);

        File file = new File(folder, fileName);

        // If file doesn't exist.
        if (!file.exists()) {
            ExceptionReporter.handle(new IOException("specified file to be delete does not exists " + mainDir + "/" + subFolder + "/" + fileName));
            return true;
        }
        return file.delete();
    }

    /** Write to a public external directory.
     *  @param mode:
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION    --> share file.
     *  @param mainDir: - Representing the appropriate directory on the external storage ( Environment.DIRECTORY_MUSIC, ...)
     *                  - It can be null --> represent that directory as a parent file of private external storage in the app.
     *  @param subFolder: usually an app name to distinguish with another app.
     */
    public boolean writeFileExternalPrivate(String mainDir, String subFolder, String fileName, String data, int mode){
        // Get the directory for the user's private mainDir directory.
        String directory = mContext.getExternalFilesDir(mainDir) + File.separator  + subFolder;
        File folder = new File(directory);

        // If directory doesn't exist, create it.
        if (!folder.exists()) {
            if(!folder.mkdirs())
                ExceptionReporter.handle(new IOException("Failed to create new directory. " + directory));
        }

        File file = new File(folder, fileName);

        try {
            FileOutputStream fos;
            fos = new FileOutputStream(file);
            OutputStreamWriter out = new OutputStreamWriter(fos);
            if(mode == Context.MODE_APPEND) {
                out.append(data + "\n");
            }else {
                out.write(data);
            }
            out.close();
            return true;
        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public String readFileExternalPrivate(String mainDir, String subFolder, String fileName) {
        try {
            String directory = buildPathExternalPrivate(mainDir, subFolder);
            File folder = new File(directory);

            File file = new File(folder, fileName);
            if (!file.exists()) {
                ExceptionReporter.handle(new IOException("file does not exists '" + mainDir + "/" + subFolder + "/" + fileName + "'"));
                return null;
            }

            FileInputStream fis = new FileInputStream(file);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean deleteFileExternalPrivate(String mainDir, String subFolder, String fileName){

        String directory = buildPathExternalPrivate(mainDir, subFolder);
        File folder = new File(directory);

        File file = new File(folder, fileName);
        if (!file.exists()) {
            ExceptionReporter.handle(new IOException("Failed to delete file '" + mainDir + "/" + subFolder + "/" + fileName +"'"));
            return true;
        }
        return file.delete();
    }

    // Looking for File directory of all external cards (including onboard sd card).
    public ArrayList<File> getExternalSDCardDirectory(){

        // Retrieve the primary External Storage (usually onboard sd card, it's based on user setting).
        final File primaryExternalStorage = Environment.getExternalStorageDirectory();

        // Primary external storage (onboard sd card) usually has path: [storage]/emulated/0
        File externalStorageRoot = primaryExternalStorage.getParentFile().getParentFile();

        // Get list folders under externalStorageRoot (which includes primaryExternalStorage folder).
        File[] files = externalStorageRoot.listFiles();

        ArrayList<File> listStorage = new ArrayList<>();

        for (File file : files) {
            // it is a real directory (not a USB drive)...
            if ( file.isDirectory() && file.canRead() && (file.listFiles().length > 0) ) {
                listStorage.add(file);
            }
        }

        return listStorage;
    }


    /**********************************************************************************************/
    private String buildPath(String... args){
        if (args != null && args.length > 0){
            StringBuilder sb = new StringBuilder();
            sb.append(args[0]);
            for (int i = 1; i < args.length; i++){
                sb.append(File.separator).append(args[i]);
            }
            return sb.toString();
        }
        return null;
    }

    public String getPathRootExternalPublic(){
        return  Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public String getPathDirectoryExternalPublic(String mainDir, String subFolder){
        File root;
        if(mainDir.isEmpty()){
            root = Environment.getExternalStorageDirectory();
        }else {
            root = Environment.getExternalStoragePublicDirectory(mainDir);
        }

        return root + File.separator  + subFolder;
    }

    public String buildPathExternalPrivate(String mainDir, String subFolder){
        return mContext.getExternalFilesDir(mainDir) + File.separator  + subFolder;
    }

    public File getDirectoryExternalPublic(String... dirPath){
        if (dirPath == null || dirPath[0] == null)
            ExceptionReporter.handle(new IllegalArgumentException("file path / address not provided"));

        try {
            String path = buildPath(dirPath);
            if (path == null)
                return null;

            File file = Environment.getExternalStoragePublicDirectory(path);

            if (!file.exists())
                return file.mkdirs() ? file : null;

            return file;
        }catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public File getFileExternalPublic(String... filePath){
        if (filePath == null || filePath[0] == null) {
            ExceptionReporter.handle(new IllegalArgumentException("file address not provided"));
            return null;
        }

        try {
            File file = null;
            if (filePath.length > 1) {
                String fileName = filePath[filePath.length - 1];
                String[] path = new String[filePath.length-1];
                System.arraycopy(filePath,0, path, 0, filePath.length-1);
                File dir = getDirectoryExternalPublic(path);
                file = new File(dir, fileName);
            } else {
                file = new File(getPathRootExternalPublic() + File.separator + filePath[0]);
            }

            return file;
        }catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean writeFile(File file, String data, int mode){
        try {
            FileOutputStream fos = (mode == Context.MODE_APPEND) ?
                    new FileOutputStream(file, true) :
                    new FileOutputStream(file);
            OutputStreamWriter out = new OutputStreamWriter(fos);

            if (mode == Context.MODE_APPEND) {
                out.append(data).append("\n");
            } else {
                out.write(data);
            }

            out.close();
            return true;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public String readFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean deleteFile(@NonNull File file){
        if(file.exists()) {
            return file.delete();
        }
        return false;
    }
}
