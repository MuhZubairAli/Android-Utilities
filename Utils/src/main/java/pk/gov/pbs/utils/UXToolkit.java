package pk.gov.pbs.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

/**
 * Todo: add functionality to add buttons to progress dialog (i,e for location ProgressDialog after some time show button to use last location)
 */

public class UXToolkit {
    protected final CustomActivity context;
    protected final LayoutInflater mLayoutInflater;
    protected final InputMethodManager mInputMethodManager;
    protected AlertDialog.Builder mDialogBuilder;
    protected ProgressDialog progressDialog;

    public UXToolkit(CustomActivity _context){
        context = _context;
        mInputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mLayoutInflater = LayoutInflater.from(context);
    }

    public AlertDialog.Builder getDialogBuilder(){
        if(mDialogBuilder == null) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                mDialogBuilder = new AlertDialog.Builder(context);
            else
                mDialogBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        }
        return mDialogBuilder;
    }

    private View inflateInfoAlertDialogue(String title, String message){
        Spanned htm = Html.fromHtml(message);
        View dlg = mLayoutInflater.inflate(R.layout.custom_dialogue_alert,null);
        ((TextView) dlg.findViewById(R.id.tv_title)).setText(title);
        ((TextView) dlg.findViewById(R.id.tv_message)).setText(htm);
        return dlg;
    }

    public void hideKeyboardFrom(View view) {
        if (view == null)
            view = context.getWindow().getCurrentFocus();
        if (view == null) {
            view = new View(context);
            view.setFocusable(true);
            view.requestFocus();
        }
        mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    public void showKeyboardTo(View view) {
        if (view == null)
            view = context.getCurrentFocus();
        if (view == null) {
            view = new View(context);
            view.setFocusable(true);
            view.requestFocus();
        }
        mInputMethodManager.showSoftInput(view, 0);
    }

    public AlertDialog buildAlertDialogue(String title, String message,@Nullable String positiveButtonLabel,@Nullable UXEventListeners.AlertDialogueEventListener callback){
        AlertDialog alertDialog;
        if(positiveButtonLabel == null)
            positiveButtonLabel = context.getResources().getString(R.string.label_btn_ok);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Spanned htm = Html.fromHtml(message);
            alertDialog = getDialogBuilder()
                    .setTitle(title)
                    .setMessage(htm)
                    .setCancelable(false)
                    .setPositiveButton(
                            positiveButtonLabel
                            , (dialog, which) -> {
                                if(callback != null)
                                    callback.onOK(dialog, which);
                            }
                    )
                    .create();
        } else {
            alertDialog = getDialogBuilder()
                    .setView(inflateInfoAlertDialogue(title, message))
                    .setCancelable(false)
                    .setPositiveButton(
                            positiveButtonLabel
                            , (dialog, which) -> {
                                if(callback != null)
                                    callback.onOK(dialog, which);
                            }
                    )
                    .create();
        }
        return alertDialog;
    }

    public AlertDialog buildConfirmDialogue(String title, String message, @Nullable String positiveBtnLabel, @Nullable String negativeBtnLabel, UXEventListeners.ConfirmDialogueEventsListener events) {
        if (positiveBtnLabel == null)
            positiveBtnLabel = context.getResources().getString(R.string.label_btn_ok);
        if (negativeBtnLabel == null)
            negativeBtnLabel = "Cancel";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                Spanned htm = Html.fromHtml(message);
                return getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(positiveBtnLabel, events::onOK)
                        .setNegativeButton(negativeBtnLabel, events::onCancel)
                        .create();
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        } else {
            try {
                return getDialogBuilder()
                        .setView(inflateInfoAlertDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                positiveBtnLabel
                                , events::onOK)
                        .setNegativeButton(
                                negativeBtnLabel
                                , events::onCancel)
                        .create();
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        }
        return null;
    }

    public AlertDialog showAlertDialogue(String title, String message, String positiveButtonLabel, @Nullable UXEventListeners.AlertDialogueEventListener event){
        AlertDialog dialog = buildAlertDialogue(title, message, positiveButtonLabel, event);
        dialog.show();
        return dialog;
    }

    public AlertDialog showAlertDialogue(String title, String message, @Nullable UXEventListeners.AlertDialogueEventListener event){
        return showAlertDialogue(title, message, null, event);
    }

    public AlertDialog showAlertDialogue(int title, int message, UXEventListeners.AlertDialogueEventListener event){
        return showAlertDialogue(context.getString(title), context.getString(message), event);
    }

    public AlertDialog showAlertDialogue(String title, String message){
        return showAlertDialogue(title,message,null);
    }

    public AlertDialog showAlertDialogue(int title, int message){
        return showAlertDialogue(context.getString(title), context.getString(message), null);
    }

    public AlertDialog showAlertDialogue(String message, UXEventListeners.AlertDialogueEventListener event){
        return showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), message, event);
    }

    public AlertDialog showAlertDialogue(int message, UXEventListeners.AlertDialogueEventListener event){
        return showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), context.getString(message), event);
    }

    public AlertDialog showAlertDialogue(String message){
        return showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), message, null);
    }

    public AlertDialog showAlertDialogue(int message){
        return showAlertDialogue(context.getString(R.string.title_default_alert_dialogue), context.getString(message), null);
    }

    public AlertDialog showConfirmDialogue(String title, String message, String positiveBtnLabel, String negativeBtnLabel, UXEventListeners.ConfirmDialogueEventsListener events){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            try {
                Spanned htm = Html.fromHtml(message);
                AlertDialog dialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(positiveBtnLabel, events::onOK)
                        .setNegativeButton(negativeBtnLabel, events::onCancel)
                        .create();

                dialog.show();
                return dialog;
            } catch (Exception e){
                ExceptionReporter.handle(e);
            }
        } else {
            try {
                AlertDialog alert = getDialogBuilder()
                        .setView(inflateInfoAlertDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(positiveBtnLabel, events::onOK)
                        .setNegativeButton(negativeBtnLabel, events::onCancel)
                        .create();
                alert.show();
                return alert;
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        }
        return null;
    }

    public AlertDialog showConfirmDialogue(String title, String message, UXEventListeners.ConfirmDialogueEventsListener events){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            try {
                Spanned htm = Html.fromHtml(message);
                AlertDialog dialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(R.string.label_btn_ok, events::onOK)
                        .setNegativeButton(R.string.label_btn_cancel, events::onCancel)
                        .create();

                dialog.show();
                return dialog;
            } catch (Exception e){
                ExceptionReporter.handle(e);
            }
        } else {
            try {
                AlertDialog alert = getDialogBuilder()
                        .setView(inflateInfoAlertDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                R.string.label_btn_ok
                                , events::onOK)
                        .setNegativeButton(
                                context
                                        .getResources()
                                        .getString(R.string.label_btn_cancel)
                                , events::onCancel)
                        .create();
                alert.show();
                return alert;
            } catch (Exception e) {
                ExceptionReporter.handle(e);
            }
        }
        return null;
    }

    public AlertDialog showConfirmDialogue(String message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(R.string.title_default_confirm_dialogue),message,events);
    }

    public AlertDialog showConfirmDialogue(int message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(R.string.title_default_confirm_dialogue),context.getString(message),events);
    }

    public AlertDialog showConfirmDialogue(int title, int message, UXEventListeners.ConfirmDialogueEventsListener events){
        return showConfirmDialogue(context.getString(title),context.getString(message),events);
    }

    public ProgressDialog changeProgressDialogueMessage(String message){
        synchronized (this) {
            if (progressDialog != null)
                progressDialog.setMessage(message);
        }
        return progressDialog;
    }

    public ProgressDialog showProgressDialogue(String message, boolean cancelable){
        synchronized (this) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(message);
            } else
                changeProgressDialogueMessage(message);
            showProgressDialogue(cancelable);
        }
        return progressDialog;
    }

    public ProgressDialog showProgressDialogue(String message){
        return showProgressDialogue(message, false);
    }

    public ProgressDialog showProgressDialogue(int stringResource, boolean cancelable){
        return showProgressDialogue(context.getResources().getString(stringResource), cancelable);
    }

    public ProgressDialog showProgressDialogue(int stringResource){
        return showProgressDialogue(stringResource, false);
    }

    public void dismissProgressDialogue(){
        synchronized (this) {
            if (progressDialog != null) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    public ProgressDialog showProgressDialogue(){
        return showProgressDialogue(false);
    }

    private ProgressDialog showProgressDialogue(boolean cancelable){
        if(!progressDialog.isShowing()) {
            progressDialog.setCancelable(cancelable);
            progressDialog.show();
        }
        return progressDialog;
    }

    public void showToast(String message){
        synchronized (this) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    public void showToast(int stringResource){
        synchronized (this) {
            Toast.makeText(context, context.getResources().getString(stringResource), Toast.LENGTH_LONG).show();
        }
    }

    public void showToast(Spanned htm) {
        synchronized (this) {
            Toast.makeText(context, htm, Toast.LENGTH_LONG).show();
        }
    }

}
