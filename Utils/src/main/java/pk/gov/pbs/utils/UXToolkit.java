package pk.gov.pbs.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * Todo: add functionality to add buttons to progress dialog (i,e for location ProgressDialog after some time show button to use last location)
 */

public class UXToolkit {
    protected final InputMethodManager mInputMethodManager;
    protected AlertDialog.Builder mDialogBuilder;
    protected ProgressDialog mProgressDialog;

    public UXToolkit(Context activityContext){
        mInputMethodManager = (InputMethodManager) activityContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mDialogBuilder = getDialogBuilder(activityContext);
    }
    private AlertDialog.Builder getDialogBuilder(Context context, int theme){
        return  (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ?
                new AlertDialog.Builder(context) :
                new AlertDialog.Builder(
                        context, theme
                );
    }

    private AlertDialog.Builder getDialogBuilder(Context context){
        return getDialogBuilder(context, R.style.AlertDialogTheme);
    }

    public AlertDialog.Builder getDialogBuilder() {
        return mDialogBuilder;
    }

    private View inflateDefaultDialogue(String title, String message) throws Exception {
        return inflateDefaultDialogue(title, message, R.layout.custom_dialogue_alert);
    }

    private View inflateDefaultDialogue(String title, String message, int resourceLayout) throws Exception {
        if (mDialogBuilder != null) {
            Spanned htm = Html.fromHtml(message);
            View defaultDialog = LayoutInflater.from(mDialogBuilder.getContext())
                    .inflate(resourceLayout, null);
            ((TextView) defaultDialog.findViewById(R.id.tv_title)).setText(title);
            ((TextView) defaultDialog.findViewById(R.id.tv_message)).setText(htm);
            return defaultDialog;
        } else
            throw new Exception("Dialog builder is null, can not use LayoutInflater");
    }

    private View getFocusedView(@NonNull View view) {
        View focusedView;
        if (!view.hasFocus()) {
            if (view.isFocusable() && view.requestFocus()){
                focusedView = view;
            } else {
                focusedView = view.findFocus();
            }
            if (!focusedView.hasFocus()){
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                view.requestFocus();
                focusedView = view;
            }
        } else
            focusedView = view;
        return focusedView;
    }

    public boolean hideKeyboardFrom(View view) {
        View focusedView = getFocusedView(view);
        return mInputMethodManager.hideSoftInputFromWindow(
                focusedView.getWindowToken(), 0
        );
    }

    public boolean showKeyboardTo(View view) {
        View focusedView = getFocusedView(view);
        return mInputMethodManager.showSoftInput(focusedView, 0);
    }

    public AlertDialog.Builder getDialogueBuilder(int contentView){
        return getDialogueBuilder(contentView, true);
    }

    public AlertDialog.Builder getDialogueBuilder(int contentView, boolean cancelable) {
        return getDialogBuilder()
                .setView(contentView)
                .setCancelable(cancelable);
    }

    public AlertDialog.Builder getDialogueBuilder(View contentView) {
        return getDialogueBuilder(contentView, true);
    }

    public AlertDialog.Builder getDialogueBuilder(View contentView, boolean cancelable){
        return getDialogBuilder()
                .setView(contentView)
                .setCancelable(cancelable);
    }

    public AlertDialog buildAlertDialogue(
            String title,
            String message,
            String btnLabel,
            UXEvent.AlertDialogue callback
    ) {
        try {
            if (mDialogBuilder == null)
                throw new IllegalStateException("Dialog builder is null, can not proceed to build AlertDialog");

            AlertDialog alertDialog;
            if (btnLabel == null)
                btnLabel = mDialogBuilder
                        .getContext()
                        .getResources()
                        .getString(R.string.label_btn_ok);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Spanned htm = Html.fromHtml(message);
                alertDialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(
                                btnLabel
                                , (dialog, which) -> {
                                    if (callback != null)
                                        callback.onOK(dialog, which);
                                    else
                                        dialog.dismiss();
                                }
                        )
                        .create();
            } else {
                alertDialog = getDialogBuilder()
                        .setView(inflateDefaultDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                btnLabel
                                , (dialog, which) -> {
                                    if (callback != null)
                                        callback.onOK(dialog, which);
                                }
                        )
                        .create();
            }
            return alertDialog;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public void showAlertDialogue(String title, String message, String btnLabel, UXEvent.AlertDialogue event){
        buildAlertDialogue(title, message, btnLabel, event).show();
    }

    public void showAlertDialogue(int title, int message, int btnLabel, UXEvent.AlertDialogue event){
        buildAlertDialogue(
                mDialogBuilder.getContext().getString(title),
                mDialogBuilder.getContext().getString(message),
                mDialogBuilder.getContext().getString(btnLabel),
                event
        ).show();
    }

    public void showAlertDialogue(String title, String message, UXEvent.AlertDialogue event){
        buildAlertDialogue(title, message, null, event).show();
    }

    public void showAlertDialogue(int title, int message, UXEvent.AlertDialogue event){
        buildAlertDialogue(
                mDialogBuilder.getContext().getString(title),
                mDialogBuilder.getContext().getString(message),
                null, event
        ).show();
    }

    public void showAlertDialogue(String title, String message){
        showAlertDialogue(title,message,null, null);
    }

    public void showAlertDialogue(int title, int message){
        showAlertDialogue(
                mDialogBuilder.getContext().getString(title),
                mDialogBuilder.getContext().getString(message),
                null, null);
    }

    public void showAlertDialogue(String message, UXEvent.AlertDialogue event){
        showAlertDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_alert_dialogue)
                , message, null, event);
    }

    public void showAlertDialogue(int message, UXEvent.AlertDialogue event){
        showAlertDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_alert_dialogue),
                mDialogBuilder.getContext().getString(message),
                null, event);
    }

    public void showAlertDialogue(String message){
        showAlertDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_alert_dialogue),
                message, null, null);
    }

    public void showAlertDialogue(int message){
        showAlertDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_alert_dialogue),
                mDialogBuilder.getContext().getString(message),
                null, null);
    }

    public AlertDialog buildConfirmDialogue(
            String title,
            String message,
            String positiveBtnLabel,
            String negativeBtnLabel,
            UXEvent.ConfirmDialogue events
    ) {
        try {
            if (mDialogBuilder == null)
                throw new IllegalStateException("Dialog builder is null, can not proceed to build AlertDialog");

            AlertDialog confirmDialog;

            if (positiveBtnLabel == null)
                positiveBtnLabel = mDialogBuilder
                        .getContext()
                        .getResources()
                        .getString(R.string.label_btn_ok);

            if (negativeBtnLabel == null)
                negativeBtnLabel = mDialogBuilder
                        .getContext()
                        .getResources()
                        .getString(R.string.label_btn_cancel);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Spanned htm = Html.fromHtml(message);
                confirmDialog = getDialogBuilder()
                        .setTitle(title)
                        .setMessage(htm)
                        .setCancelable(false)
                        .setPositiveButton(positiveBtnLabel, events::onOK)
                        .setNegativeButton(negativeBtnLabel, events::onCancel)
                        .create();
            } else {
                confirmDialog = getDialogBuilder()
                        .setView(inflateDefaultDialogue(title, message))
                        .setCancelable(false)
                        .setPositiveButton(
                                positiveBtnLabel
                                , events::onOK)
                        .setNegativeButton(
                                negativeBtnLabel
                                , events::onCancel)
                        .create();
            }
            return confirmDialog;
        } catch (Exception e){
            ExceptionReporter.handle(e);
            return null;
        }
    }

    public void showConfirmDialogue(String title, String message, String labelBtnPositive, String labelBtnNegative, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(title, message, labelBtnPositive, labelBtnNegative, events).show();
    }

    public void showConfirmDialogue(int title, int message, String labelBtnPositive, String labelBtnNegative, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(
                mDialogBuilder.getContext().getString(title),
                mDialogBuilder.getContext().getString(message),
                labelBtnPositive, labelBtnNegative, events)
                .show();
    }

    public void showConfirmDialogue(String title, String message, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(title, message, null, null, events).show();
    }

    public void showConfirmDialogue(int title, int message, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(
                mDialogBuilder.getContext().getString(title),
                mDialogBuilder.getContext().getString(message)
                , null, null, events)
                .show();
    }

    public void showConfirmDialogue(String message, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_confirm_dialogue),
                message, null, null, events).
                show();
    }

    public void showConfirmDialogue(int message, UXEvent.ConfirmDialogue events){
        buildConfirmDialogue(
                mDialogBuilder.getContext().getString(R.string.title_default_confirm_dialogue),
                mDialogBuilder.getContext().getString(message)
                , null, null, events)
                .show();
    }

    public ProgressDialog buildProgressDialogue(
            String title,
            String message,
            DialogInterface.OnCancelListener cancelListener
    ) {
        ProgressDialog dialog = new ProgressDialog(mDialogBuilder.getContext());
        if (title == null)
            title = mDialogBuilder.getContext().getString(R.string.title_default_progress_dialogue);
        dialog.setTitle(title);
        if (message != null)
            dialog.setMessage(message);
        if (cancelListener != null)
            dialog.setOnCancelListener(cancelListener);
        return dialog;
    }

    public ProgressDialog buildProgressDialogue(String title, String message) {
        return buildProgressDialogue(title, message, null);
    }

    public ProgressDialog buildProgressDialogue(String title) {
        return buildProgressDialogue(title, null, null);
    }

    public void showProgressDialogue(String title){
        mProgressDialog = buildProgressDialogue(title);
        mProgressDialog.show();
    }

    public void showProgressDialogue(){
        if(mProgressDialog != null)
            dismissProgressDialogue();
        mProgressDialog = buildProgressDialogue(null);
        mProgressDialog.show();
    }

    public void dismissProgressDialogue(){
        if(mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        mProgressDialog = null;
    }
    public void showToast(String message){
        Toast.makeText(mDialogBuilder.getContext(), message, Toast.LENGTH_LONG).show();
    }

    public void showToast(int stringResource){
        Toast.makeText(mDialogBuilder.getContext(), stringResource, Toast.LENGTH_LONG).show();
    }

    public void showToast(Spanned htm) {
        Toast.makeText(mDialogBuilder.getContext(), htm, Toast.LENGTH_LONG).show();
    }

    public static final class CommonAlerts {
        private static AlertDialog dialogLocationSettings;
        private static AlertDialog dialogAppSettings;

        public static void showAlertLocationSettings(UXToolkit toolkit){
            if(dialogLocationSettings == null) {
                dialogLocationSettings = toolkit.buildAlertDialogue(
                        toolkit.mDialogBuilder.getContext().getString(R.string.alert_dialog_gps_title)
                        ,toolkit.mDialogBuilder.getContext().getString(R.string.alert_dialog_gps_message)
                        ,toolkit.mDialogBuilder.getContext().getString(R.string.label_btn_location_settings)
                        , (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            toolkit.mDialogBuilder.getContext().startActivity(intent);
                        }
                );
            }

            if(!dialogLocationSettings.isShowing())
                dialogLocationSettings.show();
        }

        public static void showAlertAppPermissionsSetting(UXToolkit toolkit){
            if(dialogAppSettings == null) {
                dialogAppSettings = toolkit.buildConfirmDialogue(
                         toolkit.mProgressDialog.getContext().getString(R.string.alert_dialog_all_permissions_title)
                        , toolkit.mDialogBuilder.getContext().getString(R.string.alert_dialog_all_permissions_message)
                        , toolkit.mDialogBuilder.getContext().getString(R.string.label_btn_permissions_settings)
                        , "Cancel"
                        , new UXEvent.ConfirmDialogue() {
                            @Override
                            public void onCancel(DialogInterface dialog, int which) {
                                dialogAppSettings.dismiss();
                            }

                            @Override
                            public void onOK(DialogInterface dialog, int which) {
                                final Intent i = new Intent();
                                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                i.setData(Uri.parse("package:" + toolkit.mDialogBuilder.getContext().getPackageName()));
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                toolkit.mDialogBuilder.getContext().startActivity(i);
                            }
                        }

                );
            }
            if(!dialogAppSettings.isShowing())
                dialogAppSettings.show();
        }

    }
}
