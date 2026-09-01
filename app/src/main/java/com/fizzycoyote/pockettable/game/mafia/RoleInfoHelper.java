package com.fizzycoyote.pockettable.game.mafia;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.utils.AppDialog;

public final class RoleInfoHelper {

    private RoleInfoHelper() {}

    public static void show(Context context) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int outerPad = dp(context, 16);
        container.setPadding(outerPad, outerPad, outerPad, outerPad);

        addRoleRow(context, container, MafiaRole.MAFIA, context.getString(R.string.mafia_role_mafia),
                context.getString(R.string.mafia_role_dialog_mafia));
        addRoleRow(context, container, MafiaRole.DETECTIVE, context.getString(R.string.mafia_role_detective),
                context.getString(R.string.mafia_role_dialog_detective));
        addRoleRow(context, container, MafiaRole.DOCTOR, context.getString(R.string.mafia_role_doctor),
                context.getString(R.string.mafia_role_dialog_doctor));
        addRoleRow(context, container, MafiaRole.VIGILANTE, context.getString(R.string.mafia_role_vigilante),
                context.getString(R.string.mafia_role_dialog_vigilante));
        addRoleRow(context, container, MafiaRole.MAYOR, context.getString(R.string.mafia_role_mayor),
                context.getString(R.string.mafia_role_dialog_mayor));
        addRoleRow(context, container, MafiaRole.JESTER, context.getString(R.string.mafia_role_jester),
                context.getString(R.string.mafia_role_dialog_jester));
        addRoleRow(context, container, MafiaRole.SERIAL_KILLER, context.getString(R.string.mafia_role_serial_killer),
                context.getString(R.string.mafia_role_dialog_serial_killer));
        addRoleRow(context, container, MafiaRole.CIVILIAN, context.getString(R.string.mafia_role_civilian),
                context.getString(R.string.mafia_role_dialog_civilian));

        scrollView.addView(container);

        AppDialog.builder(context)
                .setTitle(context.getString(R.string.mafia_role_dialog_title))
                .setView(scrollView)
                .setPositiveButton(context.getString(R.string.mafia_role_dialog_close), null)
                .show();
    }

    private static void addRoleRow(Context context, LinearLayout parent, MafiaRole role,
                                   String displayName, String description) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int vPad = dp(context, 10);
        row.setPadding(0, vPad, 0, vPad);

        ImageView icon = new ImageView(context);
        int iconSize = dp(context, 44);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginEnd(dp(context, 14));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(MafiaPlayerAdapter.getIconForRole(role));
        row.addView(icon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(context);
        title.setText(displayName);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        textCol.addView(title);

        TextView desc = new TextView(context);
        desc.setText(description);
        desc.setTextSize(13);
        textCol.addView(desc);

        row.addView(textCol);
        parent.addView(row);

        android.view.View divider = new android.view.View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)));
        divider.setBackgroundColor(0x22FFFFFF);
        parent.addView(divider);
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}