package pk.gov.pbs.utils;

import android.content.DialogInterface;

public abstract class UXEventListeners {
    public interface ConfirmDialogueEventsListener extends AlertDialogueEventListener {
        void onCancel(DialogInterface dialog, int which);
    }

    public interface AlertDialogueEventListener {
        void onOK(DialogInterface dialog, int which);
    }
}
