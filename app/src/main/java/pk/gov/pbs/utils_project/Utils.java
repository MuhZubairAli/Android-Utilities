package pk.gov.pbs.utils_project;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Field;

import pk.gov.pbs.utils.CustomActivity;
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


    public static String generateText(){
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

    public static void setVersionTextAndBehaviour(CustomActivity context, TextView tv){
        tv.setText(getDeviceOS());
        tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                context.startActivity(new Intent(context, MainActivity.class));
                context.finish();
                return true;
            }
        });
    }

}
