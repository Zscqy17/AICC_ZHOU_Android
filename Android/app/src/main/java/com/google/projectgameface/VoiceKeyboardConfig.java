package com.google.projectgameface;

import android.content.Context;
import android.content.SharedPreferences;

public final class VoiceKeyboardConfig {
    public static final String PREFS_NAME = "GameFaceLocalConfig";
    public static final String PREF_INTERACTION_MODE = "VOICE_KEYBOARD_INTERACTION_MODE";
    public static final String PREF_KEYBOARD_SCALE = "VOICE_KEYBOARD_SCALE_PERCENT";
    public static final String PREF_FOLLOW_POINTER_STRATEGY = "VOICE_KEYBOARD_FOLLOW_POINTER_STRATEGY";
    public static final String PREF_KEYBOARD_THEME = "VOICE_KEYBOARD_THEME";
    public static final String PREF_PANEL_X = "VOICE_KEYBOARD_PANEL_X";
    public static final String PREF_PANEL_Y = "VOICE_KEYBOARD_PANEL_Y";

    public static final int SCALE_STANDARD = 100;
    public static final int SCALE_LARGE = 120;
    public static final int SCALE_EXTRA_LARGE = 140;
    public static final float DEFAULT_PANEL_X = 0.5f;
    public static final float DEFAULT_PANEL_Y = 0.15f;

    public enum InteractionMode {
        DOCKED,
        FREE,
        FOLLOW_POINTER
    }

    public enum FollowPointerStrategy {
        CENTER_ON_POINTER,
        OFFSET_ABOVE_POINTER,
        EDGE_DOCK
    }

    public enum KeyboardTheme {
        GAMEFACE,
        CLASSIC
    }

    private VoiceKeyboardConfig() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static InteractionMode getInteractionMode(SharedPreferences preferences) {
        int storedMode = preferences.getInt(PREF_INTERACTION_MODE, InteractionMode.DOCKED.ordinal());
        if (storedMode < 0 || storedMode >= InteractionMode.values().length) {
            storedMode = InteractionMode.DOCKED.ordinal();
        }
        return InteractionMode.values()[storedMode];
    }

    public static void setInteractionMode(SharedPreferences preferences, InteractionMode mode) {
        preferences.edit().putInt(PREF_INTERACTION_MODE, mode.ordinal()).apply();
    }

    public static int getKeyboardScalePercent(SharedPreferences preferences) {
        int scale = preferences.getInt(PREF_KEYBOARD_SCALE, SCALE_STANDARD);
        if (scale != SCALE_STANDARD && scale != SCALE_LARGE && scale != SCALE_EXTRA_LARGE) {
            return SCALE_STANDARD;
        }
        return scale;
    }

    public static void setKeyboardScalePercent(SharedPreferences preferences, int scalePercent) {
        preferences.edit().putInt(PREF_KEYBOARD_SCALE, scalePercent).apply();
    }

    public static FollowPointerStrategy getFollowPointerStrategy(SharedPreferences preferences) {
        int storedStrategy = preferences.getInt(
                PREF_FOLLOW_POINTER_STRATEGY,
                FollowPointerStrategy.CENTER_ON_POINTER.ordinal());
        if (storedStrategy < 0 || storedStrategy >= FollowPointerStrategy.values().length) {
            storedStrategy = FollowPointerStrategy.CENTER_ON_POINTER.ordinal();
        }
        return FollowPointerStrategy.values()[storedStrategy];
    }

    public static void setFollowPointerStrategy(
            SharedPreferences preferences,
            FollowPointerStrategy strategy) {
        preferences.edit().putInt(PREF_FOLLOW_POINTER_STRATEGY, strategy.ordinal()).apply();
    }

    public static KeyboardTheme getKeyboardTheme(SharedPreferences preferences) {
        int storedTheme = preferences.getInt(PREF_KEYBOARD_THEME, KeyboardTheme.GAMEFACE.ordinal());
        if (storedTheme < 0 || storedTheme >= KeyboardTheme.values().length) {
            storedTheme = KeyboardTheme.GAMEFACE.ordinal();
        }
        return KeyboardTheme.values()[storedTheme];
    }

    public static void setKeyboardTheme(SharedPreferences preferences, KeyboardTheme theme) {
        preferences.edit().putInt(PREF_KEYBOARD_THEME, theme.ordinal()).apply();
    }

    public static float getPanelNormalizedX(SharedPreferences preferences) {
        return preferences.getFloat(PREF_PANEL_X, DEFAULT_PANEL_X);
    }

    public static float getPanelNormalizedY(SharedPreferences preferences) {
        return preferences.getFloat(PREF_PANEL_Y, DEFAULT_PANEL_Y);
    }

    public static void setPanelNormalizedPosition(
            SharedPreferences preferences,
            float normalizedX,
            float normalizedY) {
        preferences.edit()
                .putFloat(PREF_PANEL_X, normalizedX)
                .putFloat(PREF_PANEL_Y, normalizedY)
                .apply();
    }

    public static void resetPanelPosition(SharedPreferences preferences) {
        setPanelNormalizedPosition(preferences, DEFAULT_PANEL_X, DEFAULT_PANEL_Y);
    }
}