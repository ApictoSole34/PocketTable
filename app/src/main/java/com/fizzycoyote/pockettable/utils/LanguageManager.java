package com.fizzycoyote.pockettable.utils;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Thin wrapper around AppCompatDelegate's per-app language API. Calling
 * {@link #setLanguage(String)} persists the choice automatically (survives
 * process death) and triggers a recreate of any currently visible Activity
 * so the new locale takes effect immediately - no manual Locale/Configuration
 * juggling or attachBaseContext overrides needed.
 */
public final class LanguageManager {

    private LanguageManager() {}

    /** @param languageTag e.g. "en", "pl" */
    public static void setLanguage(String languageTag) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    /** Reverts to following the device's system language. */
    public static void useSystemDefault() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
    }

    /** @return the current app-level language tag, or null if following system default. */
    public static String getCurrentLanguageTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return null;
        return locales.get(0).toLanguageTag();
    }
}