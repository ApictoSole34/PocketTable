package com.fizzycoyote.pockettable.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import com.fizzycoyote.pockettable.R;

public final class DiagnosticsDialogHelper {

    private DiagnosticsDialogHelper() {}

    public static void show(Activity activity) {
        android.view.View dialogView = android.view.LayoutInflater.from(activity)
                .inflate(R.layout.dialog_diagnostics, null);

        TextView tvTips = dialogView.findViewById(R.id.tvDiagnosticsTips);
        tvTips.setText(R.string.diagnostics_tips_full);

        Button btnBattery = dialogView.findViewById(R.id.btnDiagnosticsBattery);
        Button btnRestart = dialogView.findViewById(R.id.btnDiagnosticsRestart);

        AlertDialog dialog = AppDialog.builder(activity)
                .setTitle(R.string.diagnostics_title)
                .setView(dialogView)
                .setNegativeButton(R.string.mafia_ok, null)
                .create();

        btnBattery.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        });

        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            Intent restartIntent = activity.getPackageManager()
                    .getLaunchIntentForPackage(activity.getPackageName());
            if (restartIntent != null) {
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(restartIntent);
            }
            activity.finishAffinity();
            Runtime.getRuntime().exit(0);
        });

        dialog.show();
    }
}
