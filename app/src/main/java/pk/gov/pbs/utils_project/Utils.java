package pk.gov.pbs.utils_project;

import android.os.Build;

import java.lang.reflect.Field;

import pk.gov.pbs.utils.ExceptionReporter;

public class Utils {
    public static String getDeviceOS(){
        //String apiLevel = Build.VERSION.CODENAME + " - Android "  + Build.VERSION.RELEASE + " (Api Level " + Build.VERSION.SDK_INT  + ")";

        StringBuilder builder = new StringBuilder();
        builder.append("Android ").append(Build.VERSION.RELEASE);

        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException | IllegalAccessException | NullPointerException e) {
                ExceptionReporter.handle(e);
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                builder.append(" (").append(fieldName).append(") : ");
                builder.append("sdk=").append(fieldValue);
            }
        }
        return builder.toString();
    }
}
