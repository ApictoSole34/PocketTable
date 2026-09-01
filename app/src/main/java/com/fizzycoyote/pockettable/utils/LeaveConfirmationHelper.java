package com.fizzycoyote.pockettable.utils;

import android.content.Context;

import com.fizzycoyote.pockettable.R;

/**
 * Shared "are you sure you want to leave?" confirmation dialog, used by the
 * Lobby and every game table screen so the leave/disconnect flow looks and
 * behaves the same everywhere.
 */
public final class LeaveConfirmationHelper {

    private LeaveConfirmationHelper() {}

    public interface OnConfirmLeave {
        void onConfirmedLeave();
    }

    public static void show(Context context, int messageResId, OnConfirmLeave onConfirm) {
        AppDialog.builder(context)
                .setTitle(R.string.leave_confirm_title)
                .setMessage(messageResId)
                .setPositiveButton(R.string.leave_confirm_positive, (dialog, which) -> onConfirm.onConfirmedLeave())
                .setNegativeButton(R.string.leave_confirm_negative, null)
                .setCancelable(true)
                .show();
    }
}