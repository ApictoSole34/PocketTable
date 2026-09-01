package com.fizzycoyote.pockettable.utils;

import android.app.AlertDialog;
import android.content.Context;

import com.fizzycoyote.pockettable.R;

public final class AppDialog {

    private AppDialog() {}

    public static AlertDialog.Builder builder(Context context) {
        return new AlertDialog.Builder(context, R.style.AppAlertDialogTheme);
    }
}
