package com.google.projectgameface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class VoiceKeyboardSettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;

    private Button dockedModeButton;
    private Button freeModeButton;
    private Button followModeButton;
    private Button scaleStandardButton;
    private Button scaleLargeButton;
    private Button scaleExtraLargeButton;
    private Button followCenterButton;
    private Button followAboveButton;
    private Button followEdgeButton;
    private Button themeGameFaceButton;
    private Button themeClassicButton;
    private TextView modeValue;
    private TextView scaleValue;
    private TextView followStrategyValue;
    private TextView gestureSummary;
    private TextView imeStatusValue;
    private TextView themeValue;
    private LinearLayout previewPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_keyboard_settings);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        preferences = VoiceKeyboardConfig.preferences(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.voice_keyboard_settings_title);
        }

        modeValue = findViewById(R.id.voiceKeyboardSettingsModeValue);
        scaleValue = findViewById(R.id.voiceKeyboardSettingsScaleValue);
        followStrategyValue = findViewById(R.id.voiceKeyboardSettingsFollowValue);
        gestureSummary = findViewById(R.id.voiceKeyboardSettingsGestureSummary);
        imeStatusValue = findViewById(R.id.voiceKeyboardSettingsImeStatusValue);
        themeValue = findViewById(R.id.voiceKeyboardSettingsThemeValue);
        previewPanel = findViewById(R.id.voiceKeyboardSettingsPreviewPanel);

        dockedModeButton = findViewById(R.id.voiceKeyboardSettingsDockedButton);
        freeModeButton = findViewById(R.id.voiceKeyboardSettingsFreeButton);
        followModeButton = findViewById(R.id.voiceKeyboardSettingsFollowButton);
        scaleStandardButton = findViewById(R.id.voiceKeyboardSettingsScale100Button);
        scaleLargeButton = findViewById(R.id.voiceKeyboardSettingsScale120Button);
        scaleExtraLargeButton = findViewById(R.id.voiceKeyboardSettingsScale140Button);
        followCenterButton = findViewById(R.id.voiceKeyboardSettingsFollowCenterButton);
        followAboveButton = findViewById(R.id.voiceKeyboardSettingsFollowAboveButton);
        followEdgeButton = findViewById(R.id.voiceKeyboardSettingsFollowEdgeButton);
        themeGameFaceButton = findViewById(R.id.voiceKeyboardSettingsThemeGameFaceButton);
        themeClassicButton = findViewById(R.id.voiceKeyboardSettingsThemeClassicButton);

        dockedModeButton.setOnClickListener(v -> updateInteractionMode(VoiceKeyboardConfig.InteractionMode.DOCKED));
        freeModeButton.setOnClickListener(v -> updateInteractionMode(VoiceKeyboardConfig.InteractionMode.FREE));
        followModeButton.setOnClickListener(v -> updateInteractionMode(VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER));

        scaleStandardButton.setOnClickListener(v -> updateScale(VoiceKeyboardConfig.SCALE_STANDARD));
        scaleLargeButton.setOnClickListener(v -> updateScale(VoiceKeyboardConfig.SCALE_LARGE));
        scaleExtraLargeButton.setOnClickListener(v -> updateScale(VoiceKeyboardConfig.SCALE_EXTRA_LARGE));

        followCenterButton.setOnClickListener(v -> updateFollowStrategy(
                VoiceKeyboardConfig.FollowPointerStrategy.CENTER_ON_POINTER));
        followAboveButton.setOnClickListener(v -> updateFollowStrategy(
                VoiceKeyboardConfig.FollowPointerStrategy.OFFSET_ABOVE_POINTER));
        followEdgeButton.setOnClickListener(v -> updateFollowStrategy(
                VoiceKeyboardConfig.FollowPointerStrategy.EDGE_DOCK));
        themeGameFaceButton.setOnClickListener(v -> updateTheme(VoiceKeyboardConfig.KeyboardTheme.GAMEFACE));
        themeClassicButton.setOnClickListener(v -> updateTheme(VoiceKeyboardConfig.KeyboardTheme.CLASSIC));

        findViewById(R.id.voiceKeyboardSettingsEnableImeButton).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });

        findViewById(R.id.voiceKeyboardSettingsPickerButton).setOnClickListener(v -> {
            InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
            if (inputMethodManager != null) {
            inputMethodManager.showInputMethodPicker();
            }
        });

        findViewById(R.id.voiceKeyboardSettingsGestureButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, CursorBinding.class);
            startActivity(intent);
        });

        findViewById(R.id.voiceKeyboardSettingsResetPositionButton).setOnClickListener(v -> {
            VoiceKeyboardConfig.resetPanelPosition(preferences);
            notifyImeToRefresh();
        });

        refreshUi();
    }

    private void updateInteractionMode(VoiceKeyboardConfig.InteractionMode mode) {
        VoiceKeyboardConfig.setInteractionMode(preferences, mode);
        notifyImeToRefresh();
        refreshUi();
    }

    private void updateScale(int scalePercent) {
        VoiceKeyboardConfig.setKeyboardScalePercent(preferences, scalePercent);
        notifyImeToRefresh();
        refreshUi();
    }

    private void updateFollowStrategy(VoiceKeyboardConfig.FollowPointerStrategy strategy) {
        VoiceKeyboardConfig.setFollowPointerStrategy(preferences, strategy);
        notifyImeToRefresh();
        refreshUi();
    }

    private void updateTheme(VoiceKeyboardConfig.KeyboardTheme theme) {
        VoiceKeyboardConfig.setKeyboardTheme(preferences, theme);
        notifyImeToRefresh();
        refreshUi();
    }

    private void refreshUi() {
        VoiceKeyboardConfig.InteractionMode mode = VoiceKeyboardConfig.getInteractionMode(preferences);
        int scale = VoiceKeyboardConfig.getKeyboardScalePercent(preferences);
        VoiceKeyboardConfig.FollowPointerStrategy strategy =
                VoiceKeyboardConfig.getFollowPointerStrategy(preferences);
        VoiceKeyboardConfig.KeyboardTheme theme = VoiceKeyboardConfig.getKeyboardTheme(preferences);

        modeValue.setText(getModeLabel(mode));
        scaleValue.setText(getString(R.string.voice_keyboard_settings_scale_value, scale));
        followStrategyValue.setText(getFollowStrategyLabel(strategy));
        gestureSummary.setText(getString(R.string.voice_keyboard_settings_gesture_summary_text));
        themeValue.setText(getThemeLabel(theme));
        imeStatusValue.setText(getImeStatusText());
        applyPreviewTheme(theme);

        updateButtonState(dockedModeButton, mode == VoiceKeyboardConfig.InteractionMode.DOCKED);
        updateButtonState(freeModeButton, mode == VoiceKeyboardConfig.InteractionMode.FREE);
        updateButtonState(followModeButton, mode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER);

        updateButtonState(scaleStandardButton, scale == VoiceKeyboardConfig.SCALE_STANDARD);
        updateButtonState(scaleLargeButton, scale == VoiceKeyboardConfig.SCALE_LARGE);
        updateButtonState(scaleExtraLargeButton, scale == VoiceKeyboardConfig.SCALE_EXTRA_LARGE);

        updateButtonState(followCenterButton,
                strategy == VoiceKeyboardConfig.FollowPointerStrategy.CENTER_ON_POINTER);
        updateButtonState(followAboveButton,
                strategy == VoiceKeyboardConfig.FollowPointerStrategy.OFFSET_ABOVE_POINTER);
        updateButtonState(followEdgeButton,
                strategy == VoiceKeyboardConfig.FollowPointerStrategy.EDGE_DOCK);
        updateButtonState(themeGameFaceButton, theme == VoiceKeyboardConfig.KeyboardTheme.GAMEFACE);
        updateButtonState(themeClassicButton, theme == VoiceKeyboardConfig.KeyboardTheme.CLASSIC);

        float enabledAlpha = mode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER ? 1f : 0.45f;
        followCenterButton.setAlpha(enabledAlpha);
        followAboveButton.setAlpha(enabledAlpha);
        followEdgeButton.setAlpha(enabledAlpha);
        followCenterButton.setEnabled(mode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER);
        followAboveButton.setEnabled(mode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER);
        followEdgeButton.setEnabled(mode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER);
    }

    private void updateButtonState(Button button, boolean active) {
        button.setAlpha(active ? 1f : 0.55f);
    }

    private String getModeLabel(VoiceKeyboardConfig.InteractionMode mode) {
        switch (mode) {
            case FREE:
                return getString(R.string.voice_keyboard_mode_free);
            case FOLLOW_POINTER:
                return getString(R.string.voice_keyboard_mode_follow_pointer);
            case DOCKED:
            default:
                return getString(R.string.voice_keyboard_mode_docked);
        }
    }

    private String getFollowStrategyLabel(VoiceKeyboardConfig.FollowPointerStrategy strategy) {
        switch (strategy) {
            case OFFSET_ABOVE_POINTER:
                return getString(R.string.voice_keyboard_follow_strategy_above);
            case EDGE_DOCK:
                return getString(R.string.voice_keyboard_follow_strategy_edge);
            case CENTER_ON_POINTER:
            default:
                return getString(R.string.voice_keyboard_follow_strategy_center);
        }
    }

    private String getThemeLabel(VoiceKeyboardConfig.KeyboardTheme theme) {
        switch (theme) {
            case CLASSIC:
                return getString(R.string.voice_keyboard_theme_classic);
            case GAMEFACE:
            default:
                return getString(R.string.voice_keyboard_theme_gameface);
        }
    }

    private void applyPreviewTheme(VoiceKeyboardConfig.KeyboardTheme theme) {
        if (previewPanel == null) {
            return;
        }
        switch (theme) {
            case CLASSIC:
                previewPanel.setBackgroundResource(R.drawable.voice_keyboard_panel_classic);
                break;
            case GAMEFACE:
            default:
                previewPanel.setBackgroundResource(R.drawable.voice_keyboard_panel_gameface);
                break;
        }
    }

    private String getImeStatusText() {
        InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        if (inputMethodManager == null) {
            return getString(R.string.voice_keyboard_settings_ime_status_unknown);
        }

        String targetServiceName = VoiceAccessInputMethodService.class.getName();
        for (InputMethodInfo info : inputMethodManager.getInputMethodList()) {
            if (getPackageName().equals(info.getPackageName())
                    && targetServiceName.equals(info.getServiceName())) {
                return getString(R.string.voice_keyboard_settings_ime_status_available);
            }
        }
        return getString(R.string.voice_keyboard_settings_ime_status_missing);
    }

    private void notifyImeToRefresh() {
        Intent intent = new Intent(VoiceAccessInputMethodService.ACTION_IME_CONTROL);
        intent.putExtra(
                VoiceAccessInputMethodService.EXTRA_IME_COMMAND,
                VoiceAccessInputMethodService.COMMAND_REFRESH_CONFIG);
        sendBroadcast(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}