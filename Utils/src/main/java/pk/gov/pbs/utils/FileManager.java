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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileManager {
    private static FileManager instance;
    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 20;
    protected Context mContext;

    private FileManager(Context context){
        this.mContext = context.getApplicationContext();
    }

    public static synchronized FileManager getInstance(Context context){
        if (instance == null){
            instance = new FileManager(context);
        }
        return instance;
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
        boolean readWritePermission = true;
        for (String permission : getPermissionsRequired()) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                readWritePermission = false;
                break;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return hasFileManagerPermission() && hasFileManagerPermission();
        }
        return readWritePermission;
    }

    /**
     * Requests for all storage related permissions
     * For API >= 30 it opens up the activity to allow current app the permission to manage all files
     */
    public static void requestPermissions(Activity context){
        if (!hasPermissions(context)) {
            ActivityCompat.requestPermissions(
                    context,
                    getPermissionsRequired(),
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestForFileManagerPermission(context);
        }
    }

    /**
     * For API >= 30 and API < 33 this method checks if current app has permission to manage all files
     * for other API return true by default because api < 30 don't need it
     * and API >= 33 it already has it
     * @return true if app has permission to manage all files, false otherwise
     */
    public static boolean hasFileManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()){
            Toast.makeText(context, "On API 30 and above permission to manage all files is required, Please enable the option of \'Allow access to manage all files\'.", Toast.LENGTH_SHORT).show();
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
                out.append(data);
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
        return isExternalStorageWritable() || isExternalStorageReadable();
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

    public File getRootExternalPublic(){
        return  Environment.getExternalStorageDirectory();
    }

    public File getRootExternalPrivate(){
        return  mContext.getExternalFilesDir(null);
    }

    public File getRootExternalPrivateCache(){
        File[] cacheDirs = mContext.getExternalCacheDirs();
        File preferredCache = null;
        long freeSpace = 0;
        for (File cacheDir : cacheDirs) {
            if (cacheDir.canRead() && cacheDir.canWrite()) {
                if (cacheDir.getUsableSpace() > freeSpace) {
                    preferredCache = cacheDir;
                    freeSpace = cacheDir.getUsableSpace();
                }
            }
        }
        return preferredCache;
    }

    public File getDirectoryInternal(String... filePath){
        File files = mContext.getFilesDir();
        if (files != null) {
            if (filePath != null && filePath.length > 0 && !filePath[0].isEmpty()) {
                File file = new File(files, buildPath(filePath));
                if (file.exists() || file.mkdirs())
                    return file;
            }
            return files;
        }
        return null;
    }

    public File getFileInternal(String... filePath){
        if (filePath == null || filePath.length == 0)
            ExceptionReporter.handle(new IllegalArgumentException("at least file name in the filePath argument is required"));

        File filesDir = mContext.getFilesDir();
        if (filesDir != null) {
            assert filePath != null;
            String fileName = filePath[filePath.length - 1];
            File dir;
            if (filePath.length > 1) {
                String[] dirPath = new String[filePath.length - 1];
                System.arraycopy(filePath, 0, dirPath, 0, filePath.length - 1);
                dir = new File(filesDir, buildPath(dirPath));
            } else
                dir = filesDir;

            if (dir.exists() || dir.mkdirs()) {
                try {
                    File file = new File(dir, fileName);
                    if (file.exists() || file.createNewFile())
                        return file;
                }catch (Exception e){
                    ExceptionReporter.handle(e);
                }
            }
        }
        return null;
    }

    public File getDirectoryInternalCache(String... filePath){
        File cache = mContext.getCacheDir();
        if (cache != null) {
            if (filePath != null && filePath.length > 0 && !filePath[0].isEmpty()) {
                File file = new File(cache, buildPath(filePath));
                if (file.exists() || file.mkdirs())
                    return file;
            }
            return cache;
        }
        return null;
    }

    public File getFileInternalCache(String... filePath){
        if (filePath == null || filePath.length == 0  || filePath[0].isEmpty())
            ExceptionReporter.handle(new IllegalArgumentException("at least file name in the filePath argument is required"));

        File cache = mContext.getCacheDir();
        if (cache != null) {
            assert filePath != null;
            String fileName = filePath[filePath.length - 1];
            File dir;
            if (filePath.length > 1) {
                String[] dirPath = new String[filePath.length - 1];
                System.arraycopy(filePath, 0, dirPath, 0, filePath.length - 1);
                dir = new File(cache, buildPath(dirPath));
            } else
                dir = cache;

            if (dir.exists() || dir.mkdirs()) {
                try {
                    File file = new File(dir, fileName);
                    if (file.exists() || file.createNewFile())
                        return file;
                }catch (Exception e){
                    ExceptionReporter.handle(e);
                }
            }
        }
        return null;
    }

    public File getDirectoryExternalPrivateCache(String... filePath){
        File cache = getRootExternalPrivateCache();
        if (cache != null) {
            if (filePath != null && filePath.length > 0 && !filePath[0].isEmpty()) {
                File file = new File(cache, buildPath(filePath));
                if (file.exists() || file.mkdirs())
                    return file;
            }
            return cache;
        }
        return null;
    }

    public File getFileExternalPrivateCache(String... filePath){
        if (filePath == null || filePath.length == 0 || filePath[0].isEmpty())
            ExceptionReporter.handle(new IllegalArgumentException("at least file name in the filePath argument is required"));

        File cache = getRootExternalPrivateCache();
        if (cache != null) {
            assert filePath != null;
            String fileName = filePath[filePath.length - 1];
            File dir;
            if (filePath.length > 1) {
                String[] dirPath = new String[filePath.length - 1];
                System.arraycopy(filePath, 0, dirPath, 0, filePath.length - 1);
                dir = new File(cache, buildPath(dirPath));
            } else
                dir = cache;

            if (dir.exists() || dir.mkdirs()) {
                try {
                    File file = new File(dir, fileName);
                    if (file.exists() || file.createNewFile())
                        return file;
                }catch (Exception e){
                    ExceptionReporter.handle(e);
                }
            }
        }
        return null;
    }

    public File getDirectoryExternalPrivate(String... filePath){
        String path = null;
        if (filePath != null && filePath.length > 0)
            path = buildPath(filePath);
        return mContext.getExternalFilesDir(path);
    }

    public File getFileExternalPrivate(String... filePath){
        if (filePath == null || filePath.length == 0 || filePath[0].isEmpty())
            ExceptionReporter.handle(new IllegalArgumentException("at least file name in filePath argument must be provided"));
        else {
            String fileName = filePath[filePath.length - 1];
            File dir = null;
            if (filePath.length > 1) {
                String[] path = new String[filePath.length - 1];
                System.arraycopy(filePath, 0, path, 0, filePath.length - 1);
                dir = getDirectoryExternalPrivate(path);
            }
            if (dir == null)
                dir = getDirectoryExternalPrivate();
            File file = new File(dir, fileName);
            try {
                if (file.exists() || file.createNewFile())
                    return file;
            } catch (Exception e){
                ExceptionReporter.handle(e);
            }
        }
        return null;
    }

    public File getDirectoryExternalPublic(String... dirPath){
        if (dirPath == null || dirPath.length == 0 || dirPath[0].isEmpty())
            return getRootExternalPublic();

        try {
            String path = buildPath(dirPath);
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
        if (filePath == null || filePath[0] == null || filePath[0].isEmpty()) {
            ExceptionReporter.handle(new IllegalArgumentException("file address / path argument is not provided"));
            return null;
        }

        try {
            File file;
            if (filePath.length > 1) {
                String fileName = filePath[filePath.length - 1];
                String[] path = new String[filePath.length-1];
                System.arraycopy(filePath,0, path, 0, filePath.length-1);
                File dir = getDirectoryExternalPublic(path);
                file = new File(dir, fileName);
            } else {
                file = new File(getRootExternalPublic(), filePath[0]);
            }

            return file;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public boolean writeFileString(File file, String data, int mode){
        try {
            FileOutputStream fos = new FileOutputStream(file, mode == Context.MODE_APPEND);
            OutputStreamWriter out = new OutputStreamWriter(fos);
            out.write(data);
            out.close();
            return true;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public boolean writeFileBytes(File file, byte[] data){
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return false;
        }
    }

    public String readFileString(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String strLine;
            StringBuilder fileContentStrBuilder = new StringBuilder();
            while ((strLine = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(strLine);
            }
            bufferedReader.close();
            return fileContentStrBuilder.toString();
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public byte[] readFileBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            if(fis.read(bytes) == -1)
                throw new IOException("Failed to read file");
            fis.close();
            return bytes;
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

    public static FilePath pathToFile(String... path){
        return FilePath.toFile(path);
    }

    public static DirectoryPath pathToDirectory(String... path){
        return DirectoryPath.toDirectory(path);
    }

    public static class FilePath {
        private final String[] path;

        private FilePath(String[] path){
            if (FileManager.instance == null)
                throw new IllegalStateException("FileManager is not initialized, in order to use Path extension initialize FileManager singleton first");
            this.path = path;
        }

        private static FilePath toFile(String... args) {
            if (args == null || args.length == 0)
                throw new IllegalArgumentException("path to the File is not provided");
            return new FilePath(args);
        }

        public FileOperator inPublic(){
            return new FileOperator(FileManager.instance.getFileExternalPublic(path));
        }

        public FileOperator inPrivate(){
            return new FileOperator(FileManager.instance.getFileExternalPrivate(path));
        }

        public FileOperator inPrivateCache(){
            return new FileOperator(FileManager.instance.getFileExternalPrivateCache(path));
        }

        public FileOperator inInternal(){
            return new FileOperator(FileManager.instance.getFileInternal(path));
        }

        public FileOperator inInternalCache() {
            return new FileOperator(FileManager.instance.getFileInternalCache(path));
        }
    }

    public static class DirectoryPath {
        private final String[] path;
        private DirectoryPath(String[] path){
            if (FileManager.instance == null)
                throw new IllegalStateException("FileManager is not initialized, in order to use Path extension initialize FileManager singleton first");
            this.path = path;
        }

        private static DirectoryPath toDirectory(String... args) {
            if (args == null || args.length == 0)
                throw new IllegalArgumentException("path to the Directory is not provided");
            return new DirectoryPath(args);
        }

        public DirectoryOperator inPublic(){
            return new DirectoryOperator(FileManager.instance.getDirectoryExternalPublic(path));
        }

        public DirectoryOperator inPrivate(){
            return new DirectoryOperator(FileManager.instance.getDirectoryExternalPrivate(path));
        }

        public DirectoryOperator inPrivateCache(){
            return new DirectoryOperator(FileManager.instance.getDirectoryExternalPrivateCache(path));
        }

        public DirectoryOperator inInternal(){
            return new DirectoryOperator(FileManager.instance.getDirectoryInternal(path));
        }

        public DirectoryOperator inInternalCache() {
            return new DirectoryOperator(FileManager.instance.getDirectoryInternalCache(path));
        }
    }

    public static class FileOperator {
        private final File file;

        private FileOperator(File file){
            if (file == null)
                throw new IllegalArgumentException("file is not provided or is null or does not exists in file system");
            this.file = file;
        }

        public File get(){
            return file;
        }

        public boolean write(String data){
            // just passing any value for mode so it is not Context.MODE_APPEND and it will create new file or overwrite existing file.
            return FileManager.instance.writeFileString(file, data, Context.MODE_PRIVATE);
        }

        public boolean append(String data){
            return FileManager.instance.writeFileString(file, data, Context.MODE_APPEND);
        }

        public boolean write(byte[] data){
            return FileManager.instance.writeFileBytes(file, data);
        }

        public String read(){
            return FileManager.instance.readFileString(file);
        }

        public byte[] readBytes(){
            return FileManager.instance.readFileBytes(file);
        }

        public boolean delete(){
            return FileManager.instance.deleteFile(file);
        }
    }

    public static class DirectoryOperator {
        private final File file;

        private DirectoryOperator(File file){
            if (file == null)
                throw new IllegalArgumentException("file is not provided or is null or does not exists in file system");
            if (!file.isDirectory())
                throw new IllegalArgumentException("provided file is not of type Directory");
            this.file = file;
        }

        public File get(){
            return file;
        }

        public File[] lisFiles(){
            return file.listFiles();
        }

        public File[] listFile(FilenameFilter filenameFilter){
            return file.listFiles(filenameFilter);
        }

        public boolean emptyAndDelete(){
            return doEmpty(file) && file.delete();
        }

        private boolean doEmpty(File fileToEmpty){
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory())
                        doEmpty(child);
                    if(!child.delete())
                        return false;
                }
            }
            return true;
        }

        public boolean empty(){
            return doEmpty(file);
        }

        public boolean delete(){
            return FileManager.instance.deleteFile(file);
        }
    }
}
