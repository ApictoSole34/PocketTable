package com.fizzycoyote.pockettable.utils;

import android.app.Activity;
import android.app.AlertDialog;

import com.fizzycoyote.pockettable.R;

/**
 * The "⚙ Options" dialog shown from the main menu: change the app's
 * language, or exit the app entirely (with confirmation).
 *
 * <p>{@link #LANGUAGE_TAGS} and the label array built in
 * {@link #showLanguagePicker(Activity)} must stay in the exact same order
 * as the locales declared in {@code res/xml/locales_config.xml} - the
 * index picked in the dialog is used directly to look up both arrays.</p>
 */
public final class AppOptionsDialogHelper {

    private AppOptionsDialogHelper() {}

    private static final String[] LANGUAGE_TAGS = {
            "en", "pl", "fr", "es", "de", "cs", "ar-EG", "sk",
            "lt", "nl", "ja", "zh-CN", "pt-BR", "hu", "tr"
    };

    public static void show(Activity activity) {
        String[] options = {
                activity.getString(R.string.options_change_language),
                activity.getString(R.string.options_exit_app)
        };

        AppDialog.builder(activity)
                .setTitle(R.string.options_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showLanguagePicker(activity);
                    } else {
                        confirmExit(activity);
                    }
                })
                .show();
    }

    private static void showLanguagePicker(Activity activity) {
        String[] languageLabels = {
                activity.getString(R.string.language_english),
                activity.getString(R.string.language_polish),
                activity.getString(R.string.language_french),
                activity.getString(R.string.language_spanish),
                activity.getString(R.string.language_german),
                activity.getString(R.string.language_czech),
                activity.getString(R.string.language_arabic_egypt),
                activity.getString(R.string.language_slovak),
                activity.getString(R.string.language_lithuanian),
                activity.getString(R.string.language_dutch),
                activity.getString(R.string.language_japanese),
                activity.getString(R.string.language_chinese),
                activity.getString(R.string.language_portuguese_br),
                activity.getString(R.string.language_hungarian),
                activity.getString(R.string.language_turkish)
        };

        AppDialog.builder(activity)
                .setTitle(R.string.options_change_language)
                .setItems(languageLabels, (dialog, which) -> LanguageManager.setLanguage(LANGUAGE_TAGS[which]))
                .show();
    }

    private static void confirmExit(Activity activity) {
        AppDialog.builder(activity)
                .setTitle(R.string.options_exit_confirm_title)
                .setMessage(R.string.options_exit_confirm_message)
                .setPositiveButton(R.string.options_exit_confirm_positive, (dialog, which) -> {
                    activity.finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton(R.string.leave_confirm_negative, null)
                .show();
    }
}