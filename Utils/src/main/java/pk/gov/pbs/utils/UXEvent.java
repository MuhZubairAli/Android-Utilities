package pk.gov.pbs.utils;

import android.content.DialogInterface;

public abstract class UXEvent {
    public interface ConfirmDialogue extends AlertDialogue {
        void onCancel(DialogInterface dialog, int which);
    }

    public interface AlertDialogue {
        void onOK(DialogInterface dialog, int which);
    }
}
