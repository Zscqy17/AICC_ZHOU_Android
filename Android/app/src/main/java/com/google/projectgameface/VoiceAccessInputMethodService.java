package com.google.projectgameface;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class VoiceAccessInputMethodService extends InputMethodService
    implements RecognitionListener, TextToSpeech.OnInitListener {

    public static final String ACTION_IME_CONTROL = "GAMEFACE_IME_CONTROL";
    public static final String EXTRA_IME_COMMAND = "command";
    public static final String COMMAND_TOGGLE_DICTATION = "toggle_dictation";
    public static final String COMMAND_SWITCH_MODE = "switch_mode";
    public static final String COMMAND_TOGGLE_SIZE = "toggle_size";
    public static final String COMMAND_MOVE_TO_POINTER = "move_to_pointer";
    public static final String COMMAND_REFRESH_CONFIG = "refresh_config";

    private static final String[] KEY_ROW_1 = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
    private static final String[] KEY_ROW_2 = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
    private static final String[] KEY_ROW_3 = {"zh", "ch", "sh", "z", "x", "c", "v", "b", "n", "m"};
    private static final String[] KEY_ROW_4 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private static final String[] KEY_ROW_5 = {",", ".", "?", "'"};

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;
    private SharedPreferences preferences;
    private BroadcastReceiver cursorPositionReceiver;
    private BroadcastReceiver imeCommandReceiver;
    private boolean isCursorReceiverRegistered;
    private boolean isImeCommandReceiverRegistered;

    private FrameLayout keyboardRoot;
    private FrameLayout overlayContainer;
    private LinearLayout keyboardPanel;
    private LinearLayout keyboardRow1;
    private LinearLayout keyboardRow2;
    private LinearLayout keyboardRow3;
    private LinearLayout keyboardRow4;
    private LinearLayout keyboardSystemRow;
    private LinearLayout keyboardEditRow;
    private TextView statusView;
    private TextView resultView;
    private TextView modeSummaryView;
    private HorizontalScrollView candidateScroller;
    private LinearLayout candidateRow;
    private Button micButton;
    private Button shiftButton;
    private Button dockedModeButton;
    private Button freeModeButton;
    private Button followPointerButton;
    private Button largeModeButton;
    private Button moveToPointerButton;
    private Button standardInputButton;
    private Button ziyouInputButton;
    private Button spaceButton;
    private Button deleteButton;
    private Button enterButton;
    private Button confirmButton;

    private boolean isListening;
    private boolean isTtsReady;
    private boolean isUppercase;
    private int keyboardScalePercent = VoiceKeyboardConfig.SCALE_STANDARD;
    private float panelNormalizedX = VoiceKeyboardConfig.DEFAULT_PANEL_X;
    private float panelNormalizedY = VoiceKeyboardConfig.DEFAULT_PANEL_Y;
    private float dragStartRawX;
    private float dragStartRawY;
    private float dragStartPanelX;
    private float dragStartPanelY;
    private int latestCursorX;
    private int latestCursorY;
    private int latestScreenWidth = 1;
    private int latestScreenHeight = 1;
    private VoiceKeyboardConfig.InteractionMode interactionMode = VoiceKeyboardConfig.InteractionMode.DOCKED;
    private VoiceKeyboardConfig.FollowPointerStrategy followPointerStrategy =
        VoiceKeyboardConfig.FollowPointerStrategy.CENTER_ON_POINTER;
    private VoiceKeyboardConfig.KeyboardTheme keyboardTheme = VoiceKeyboardConfig.KeyboardTheme.GAMEFACE;
    private KeyboardInputMode keyboardInputMode = KeyboardInputMode.STANDARD;
    private final Map<String, String[]> ziyouFinalMap = ZiyouInputConfig.createFinalMap();
    private final ZiyouInputState ziyouInputState = new ZiyouInputState();
    private ZiyouCandidateRepository ziyouCandidateRepository;

    private enum KeyboardInputMode {
    STANDARD,
    ZIYOU
    }

    @Override
    public void onCreate() {
    super.onCreate();
    preferences = VoiceKeyboardConfig.preferences(this);
    ziyouCandidateRepository = new ZiyouCandidateRepository(this);
    textToSpeech = new TextToSpeech(this, this);
    initializeRecognizerIntent();
    initializeCursorPositionReceiver();
    initializeImeCommandReceiver();
    loadInteractionPreferences();
    }

    @Override
        public View onCreateInputView() {
        View inputView = getLayoutInflater().inflate(R.layout.input_method_voice, null);
        bindInputView(inputView);
        Button settingsButton = inputView.findViewById(R.id.voiceKeyboardSettingsButton);
        View dragHandle = inputView.findViewById(R.id.voiceKeyboardDragHandle);

        setupInputViewListeners(settingsButton, dragHandle);
        buildKeyboardRows();

        updateStatus(getString(R.string.voice_keyboard_status_idle), false);
        resetPreviewPlaceholder();
        registerCursorPositionReceiver();
        registerImeCommandReceiver();
        keyboardRoot.post(this::refreshInteractionUi);
        return inputView;
        }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        registerCursorPositionReceiver();
        registerImeCommandReceiver();
        updateStatus(getString(R.string.voice_keyboard_status_ready), false);
        resetPreviewPlaceholder();
        updateMicButton();
        if (keyboardRoot != null) {
            keyboardRoot.post(this::refreshInteractionUi);
        }
    }

    private void initializeRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

    private void bindInputView(View inputView) {
        keyboardRoot = inputView.findViewById(R.id.voiceKeyboardRoot);
        overlayContainer = inputView.findViewById(R.id.voiceKeyboardOverlay);
        keyboardPanel = inputView.findViewById(R.id.voiceKeyboardPanel);
        keyboardRow1 = inputView.findViewById(R.id.voiceKeyboardRow1);
        keyboardRow2 = inputView.findViewById(R.id.voiceKeyboardRow2);
        keyboardRow3 = inputView.findViewById(R.id.voiceKeyboardRow3);
        keyboardRow4 = inputView.findViewById(R.id.voiceKeyboardRow4);
        keyboardSystemRow = inputView.findViewById(R.id.voiceKeyboardSystemRow);
        keyboardEditRow = inputView.findViewById(R.id.voiceKeyboardEditRow);
        statusView = inputView.findViewById(R.id.voiceKeyboardStatus);
        resultView = inputView.findViewById(R.id.voiceKeyboardResult);
        modeSummaryView = inputView.findViewById(R.id.voiceKeyboardModeSummary);
        candidateScroller = inputView.findViewById(R.id.voiceKeyboardCandidateScroller);
        candidateRow = inputView.findViewById(R.id.voiceKeyboardCandidateRow);
        dockedModeButton = inputView.findViewById(R.id.voiceKeyboardDockedButton);
        freeModeButton = inputView.findViewById(R.id.voiceKeyboardFreeButton);
        followPointerButton = inputView.findViewById(R.id.voiceKeyboardFollowButton);
        largeModeButton = inputView.findViewById(R.id.voiceKeyboardLargeButton);
        moveToPointerButton = inputView.findViewById(R.id.voiceKeyboardMoveToPointerButton);
        standardInputButton = inputView.findViewById(R.id.voiceKeyboardStandardInputButton);
        ziyouInputButton = inputView.findViewById(R.id.voiceKeyboardZiyouInputButton);
    }

    private void setupInputViewListeners(Button settingsButton, View dragHandle) {
        overlayContainer.setOnClickListener(v -> dismissZiyouOverlay(true));
        dockedModeButton.setOnClickListener(
            v -> setInteractionMode(VoiceKeyboardConfig.InteractionMode.DOCKED));
        freeModeButton.setOnClickListener(
            v -> setInteractionMode(VoiceKeyboardConfig.InteractionMode.FREE));
        followPointerButton.setOnClickListener(
            v -> setInteractionMode(VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER));
        largeModeButton.setOnClickListener(v -> toggleLargeMode());
        moveToPointerButton.setOnClickListener(v -> snapPanelToPointer());
        standardInputButton.setOnClickListener(v -> setKeyboardInputMode(KeyboardInputMode.STANDARD));
        ziyouInputButton.setOnClickListener(v -> setKeyboardInputMode(KeyboardInputMode.ZIYOU));
        settingsButton.setOnClickListener(v -> openKeyboardSettings());
        dragHandle.setOnTouchListener((view, motionEvent) -> handlePanelDrag(motionEvent));
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        stopListening();
        unregisterCursorPositionReceiver();
        unregisterImeCommandReceiver();
        super.onFinishInputView(finishingInput);
    }

    private void initializeCursorPositionReceiver() {
        cursorPositionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                latestCursorX = intent.getIntExtra("x", latestCursorX);
                latestCursorY = intent.getIntExtra("y", latestCursorY);
                latestScreenWidth = Math.max(1, intent.getIntExtra("screenWidth", latestScreenWidth));
                latestScreenHeight = Math.max(1, intent.getIntExtra("screenHeight", latestScreenHeight));

                if (interactionMode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER
                        && keyboardRoot != null) {
                    keyboardRoot.post(VoiceAccessInputMethodService.this::applyFollowPointerPosition);
                }
            }
        };
    }

    private void initializeImeCommandReceiver() {
        imeCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                String command = intent.getStringExtra(EXTRA_IME_COMMAND);
                if (command == null) {
                    return;
                }
                if (keyboardRoot != null) {
                    keyboardRoot.post(() -> handleImeCommand(command));
                }
            }
        };
    }

    private void registerCursorPositionReceiver() {
        if (isCursorReceiverRegistered || cursorPositionReceiver == null) {
            return;
        }
        IntentFilter filter = new IntentFilter(CursorAccessibilityService.ACTION_CURSOR_POSITION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cursorPositionReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(cursorPositionReceiver, filter);
        }
        isCursorReceiverRegistered = true;
    }

    private void registerImeCommandReceiver() {
        if (isImeCommandReceiverRegistered || imeCommandReceiver == null) {
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_IME_CONTROL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(imeCommandReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(imeCommandReceiver, filter);
        }
        isImeCommandReceiverRegistered = true;
    }

    private void unregisterCursorPositionReceiver() {
        if (!isCursorReceiverRegistered || cursorPositionReceiver == null) {
            return;
        }
        unregisterReceiver(cursorPositionReceiver);
        isCursorReceiverRegistered = false;
    }

    private void unregisterImeCommandReceiver() {
        if (!isImeCommandReceiverRegistered || imeCommandReceiver == null) {
            return;
        }
        unregisterReceiver(imeCommandReceiver);
        isImeCommandReceiverRegistered = false;
    }

    private void handleImeCommand(String command) {
        switch (command) {
            case COMMAND_TOGGLE_DICTATION:
                if (isListening) {
                    stopListening();
                    updateStatus(getString(R.string.voice_keyboard_status_dictation_stopped), true);
                } else {
                    startListening();
                }
                break;
            case COMMAND_SWITCH_MODE:
                cycleInteractionMode();
                break;
            case COMMAND_TOGGLE_SIZE:
                toggleLargeMode();
                break;
            case COMMAND_MOVE_TO_POINTER:
                snapPanelToPointer();
                break;
            case COMMAND_REFRESH_CONFIG:
                loadInteractionPreferences();
                refreshInteractionUi();
                break;
            default:
                break;
        }
    }

    private void loadInteractionPreferences() {
        interactionMode = VoiceKeyboardConfig.getInteractionMode(preferences);
        keyboardScalePercent = VoiceKeyboardConfig.getKeyboardScalePercent(preferences);
        followPointerStrategy = VoiceKeyboardConfig.getFollowPointerStrategy(preferences);
        keyboardTheme = VoiceKeyboardConfig.getKeyboardTheme(preferences);
        panelNormalizedX = VoiceKeyboardConfig.getPanelNormalizedX(preferences);
        panelNormalizedY = VoiceKeyboardConfig.getPanelNormalizedY(preferences);
    }

    private void saveInteractionPreferences() {
        VoiceKeyboardConfig.setInteractionMode(preferences, interactionMode);
        VoiceKeyboardConfig.setKeyboardScalePercent(preferences, keyboardScalePercent);
        VoiceKeyboardConfig.setFollowPointerStrategy(preferences, followPointerStrategy);
        VoiceKeyboardConfig.setKeyboardTheme(preferences, keyboardTheme);
        VoiceKeyboardConfig.setPanelNormalizedPosition(preferences, panelNormalizedX, panelNormalizedY);
    }

    private void refreshInteractionUi() {
        applyKeyboardTheme();
        applyPanelScale();
        updateModeButtons();
        if (interactionMode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER) {
            applyFollowPointerPosition();
        } else {
            applyStoredPanelPosition();
        }
    }

    private void openKeyboardSettings() {
        Intent intent = new Intent(this, VoiceKeyboardSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void setKeyboardInputMode(KeyboardInputMode newMode) {
        keyboardInputMode = newMode;
        ziyouInputState.clearPendingInitial();
        clearCandidateStrip();
        dismissZiyouOverlay(false);
        buildKeyboardRows();
        updateModeButtons();
        resetPreviewPlaceholder();
        updateStatus(getString(newMode == KeyboardInputMode.ZIYOU
                ? R.string.voice_keyboard_status_ziyou_enabled
                : R.string.voice_keyboard_status_standard_enabled), false);
    }

    private void resetPreviewPlaceholder() {
        updatePreview(
                ziyouInputState.getPreviewText(
                        keyboardInputMode == KeyboardInputMode.ZIYOU,
                        getString(R.string.voice_keyboard_result_placeholder),
                        getString(R.string.voice_keyboard_result_ziyou_placeholder)));
    }

    private void dismissZiyouOverlay(boolean announce) {
        ziyouInputState.clearPendingInitial();
        if (overlayContainer != null) {
            overlayContainer.removeAllViews();
            overlayContainer.setVisibility(View.GONE);
        }
        if (announce) {
            updateStatus(getString(R.string.voice_keyboard_status_ziyou_cancelled), false);
        }
    }

    private void showZiyouFinalOverlay(String initial) {
        if (overlayContainer == null) {
            return;
        }
        String[] finals = ziyouFinalMap.get(initial);
        if (finals == null || finals.length == 0) {
            appendZiyouComposition(initial);
            return;
        }

        ziyouInputState.setPendingInitial(initial);
        overlayContainer.removeAllViews();

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackgroundResource(R.drawable.voice_keyboard_panel_gameface);
        sheet.setPadding(dp(16), dp(16), dp(16), dp(16));

        FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        sheet.setLayoutParams(sheetParams);

        TextView title = new TextView(this);
        title.setText(getString(R.string.voice_keyboard_status_choose_final, initial));
        title.setTextColor(getColor(R.color.black));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, dp(12), 0, 0);
        sheet.addView(rows);

        LinearLayout currentRow = null;
        for (int index = 0; index < finals.length; index++) {
            if (index % 4 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                rows.addView(currentRow);
            }
            String fin = finals[index];
            String label = fin.isEmpty() ? initial : fin;
            Button finalButton = createKeyboardButton(label, 1f, false);
            finalButton.setOnClickListener(v -> commitZiyouSelection(initial, fin));
            if (currentRow != null) {
                currentRow.addView(finalButton);
            }
        }

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(12), 0, 0);
        Button backButton = addActionKey(
                actionRow,
                getString(R.string.voice_keyboard_ring_back),
                getString(R.string.voice_keyboard_ring_back),
                1f,
                true,
                v -> dismissZiyouOverlay(true));
        backButton.setAllCaps(false);
        sheet.addView(actionRow);

        overlayContainer.addView(sheet);
        overlayContainer.setVisibility(View.VISIBLE);
        updateStatus(getString(R.string.voice_keyboard_status_choose_final, initial), false);
    }

    private void commitZiyouSelection(String initial, String fin) {
        dismissZiyouOverlay(false);
        appendZiyouComposition(fin.isEmpty() ? initial : initial + fin);
    }

    private void appendZiyouComposition(String syllable) {
        ziyouInputState.appendSyllable(syllable);
        updatePreview(ziyouInputState.getCompositionPreview());
        refreshZiyouCandidates();
        updateStatus(getString(R.string.voice_keyboard_status_ziyou_committed, syllable), false);
    }

    private void removeLastCompositionToken() {
        if (!ziyouInputState.removeLastSyllable()) {
            clearCandidateStrip();
            resetPreviewPlaceholder();
            updateStatus(getString(R.string.voice_keyboard_status_ziyou_empty), false);
            return;
        }
        refreshZiyouCandidates();
        resetPreviewPlaceholder();
    }

    private void refreshZiyouCandidates() {
        if (keyboardInputMode != KeyboardInputMode.ZIYOU) {
            clearCandidateStrip();
            return;
        }

        String composition = ziyouInputState.getTrimmedCompositionPreview();
        if (composition.isEmpty()) {
            clearCandidateStrip();
            return;
        }

        java.util.List<String> candidates = ziyouCandidateRepository.getCandidates(composition);
        if (candidates.isEmpty()) {
            clearCandidateStrip();
            updateStatus(getString(R.string.voice_keyboard_status_ziyou_no_candidates, composition), false);
            return;
        }

        renderCandidateStrip(candidates);
        updateStatus(getString(R.string.voice_keyboard_status_ziyou_candidates, composition), false);
    }

    private void showAssociationSuggestions(String prefix) {
        if (keyboardInputMode != KeyboardInputMode.ZIYOU) {
            return;
        }

        java.util.List<String> associations = ziyouCandidateRepository.getAssociations(prefix);
        if (associations.isEmpty()) {
            clearCandidateStrip();
            return;
        }

        renderCandidateStrip(associations);
        updateStatus(getString(R.string.voice_keyboard_status_ziyou_associations, prefix), false);
    }

    private void renderCandidateStrip(java.util.List<String> candidates) {
        if (candidateScroller == null || candidateRow == null) {
            return;
        }

        candidateRow.removeAllViews();
        for (int index = 0; index < candidates.size(); index++) {
            String candidate = candidates.get(index);
            String label = index == 0
                    ? getString(R.string.voice_keyboard_candidate_first) + ": " + candidate
                    : candidate;
            Button candidateButton = createKeyboardButton(label, 1f, index == 0);
            candidateButton.setOnClickListener(v -> commitZiyouCandidate(candidate));
            candidateRow.addView(candidateButton);
        }
        candidateScroller.setVisibility(View.VISIBLE);
    }

    private void clearCandidateStrip() {
        if (candidateRow != null) {
            candidateRow.removeAllViews();
        }
        if (candidateScroller != null) {
            candidateScroller.setVisibility(View.GONE);
        }
    }

    private void commitZiyouCandidate(String candidate) {
        commitText(candidate, true);
        ziyouInputState.clearComposition();
        resetPreviewPlaceholder();
        showAssociationSuggestions(candidate);
        updateStatus(getString(R.string.voice_keyboard_status_ziyou_candidate_committed, candidate), true);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics());
    }

    private void buildKeyboardRows() {
        clearKeyboardRows();
        buildCharacterRows();
        buildSystemRow();
        buildEditRow();

        updateMicButton();
        refreshKeyCaps();
    }

    private void clearKeyboardRows() {
        keyboardRow1.removeAllViews();
        keyboardRow2.removeAllViews();
        keyboardRow3.removeAllViews();
        keyboardRow4.removeAllViews();
        keyboardSystemRow.removeAllViews();
        keyboardEditRow.removeAllViews();
    }

    private void buildCharacterRows() {
        if (keyboardInputMode == KeyboardInputMode.ZIYOU) {
            addCharacterKeys(keyboardRow1, ZiyouInputConfig.ROW_1);
            addCharacterKeys(keyboardRow2, ZiyouInputConfig.ROW_2);
        } else {
            addCharacterKeys(keyboardRow1, KEY_ROW_1);
            addCharacterKeys(keyboardRow2, KEY_ROW_2);
        }

        shiftButton = addActionKey(
                keyboardRow3,
                getString(R.string.voice_keyboard_button_shift),
                getString(R.string.voice_keyboard_button_shift_description),
                1.2f,
                true,
                v -> toggleShift());
        addCharacterKeys(
            keyboardRow3,
            keyboardInputMode == KeyboardInputMode.ZIYOU ? ZiyouInputConfig.ROW_3 : KEY_ROW_3);
        enterButton = addActionKey(
                keyboardRow3,
                getString(R.string.voice_keyboard_button_enter),
                getString(R.string.voice_keyboard_button_enter_description),
                1.3f,
                true,
                v -> commitText("\n", false));

            addCharacterKeys(
                keyboardRow4,
                keyboardInputMode == KeyboardInputMode.ZIYOU ? ZiyouInputConfig.ROW_4 : KEY_ROW_4);
            }

            private void buildSystemRow() {
        micButton = addActionKey(
                keyboardSystemRow,
                getString(R.string.voice_keyboard_button_mic),
                getString(R.string.voice_keyboard_button_mic_description),
                1.05f,
                true,
                v -> {
                    if (isListening) {
                        stopListening();
                    } else {
                        startListening();
                    }
                });
        confirmButton = addActionKey(
                keyboardSystemRow,
                getString(R.string.voice_keyboard_button_confirm),
                getString(R.string.voice_keyboard_button_confirm_description),
                1.2f,
                true,
                v -> commitPreviewText());
        spaceButton = addActionKey(
                keyboardSystemRow,
                getString(R.string.voice_keyboard_button_space),
                getString(R.string.voice_keyboard_button_space_description),
                2.1f,
                true,
                v -> commitText(" ", false));
            }

            private void buildEditRow() {
        deleteButton = addActionKey(
                keyboardEditRow,
                getString(R.string.voice_keyboard_button_backspace),
                getString(R.string.voice_keyboard_button_backspace_description),
                1.5f,
                true,
                v -> deletePreviousCharacter());
        addCharacterKeys(keyboardEditRow, KEY_ROW_5);
        addActionKey(
                keyboardEditRow,
                getString(R.string.voice_keyboard_button_settings),
                getString(R.string.voice_keyboard_button_settings_description),
                1.5f,
                true,
                v -> openKeyboardSettings());
    }

    private void addCharacterKeys(LinearLayout row, String[] keys) {
        for (String key : keys) {
            Button keyButton = createKeyboardButton(getDisplayLabel(key), 1f, false);
            keyButton.setTag(key);
            keyButton.setOnClickListener(v -> {
                Object tag = v.getTag();
                if (tag instanceof String) {
                    String tappedKey = (String) tag;
                    if (keyboardInputMode == KeyboardInputMode.ZIYOU && ziyouFinalMap.containsKey(tappedKey)) {
                        showZiyouFinalOverlay(tappedKey);
                    } else {
                        commitText(formatKeyForCase(tappedKey), false);
                    }
                }
            });
            row.addView(keyButton);
        }
    }

    private Button addActionKey(
            LinearLayout row,
            String text,
            String description,
            float weight,
            boolean accent,
            View.OnClickListener listener) {
        Button button = createKeyboardButton(text, weight, accent);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        row.addView(button);
        return button;
    }

    private Button createKeyboardButton(String text, float weight, boolean accent) {
        Button button = new Button(this);
        int keyHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                50,
                getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                3,
                getResources().getDisplayMetrics());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, keyHeight, weight);
        params.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(params);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, accent ? 14f : 18f);
        button.setTextColor(getColor(R.color.black));
        button.setBackgroundResource(accent
                ? R.drawable.voice_keyboard_key_accent
                : R.drawable.voice_keyboard_key);
        button.setText(text);
        return button;
    }

    private void refreshKeyCaps() {
        refreshRowCaps(keyboardRow1);
        refreshRowCaps(keyboardRow2);
        refreshRowCaps(keyboardRow3);
        refreshRowCaps(keyboardRow4);
    }

    private void refreshRowCaps(LinearLayout row) {
        if (row == null) {
            return;
        }
        for (int index = 0; index < row.getChildCount(); index++) {
            View child = row.getChildAt(index);
            if (child instanceof Button) {
                Object tag = child.getTag();
                if (tag instanceof String) {
                    ((Button) child).setText(getDisplayLabel((String) tag));
                }
            }
        }
    }

    private String getDisplayLabel(String key) {
        return formatKeyForCase(key);
    }

    private String formatKeyForCase(String key) {
        if (isUppercase && isAlphabeticKey(key)) {
            return key.toUpperCase(Locale.getDefault());
        }
        return key;
    }

    private boolean isAlphabeticKey(String key) {
        return key.matches("[a-z]+");
    }

    private void toggleShift() {
        isUppercase = !isUppercase;
        refreshKeyCaps();
        updateModeButtons();
        updateStatus(
                getString(isUppercase
                        ? R.string.voice_keyboard_status_shift_on
                        : R.string.voice_keyboard_status_shift_off),
                false);
    }

    private void setInteractionMode(VoiceKeyboardConfig.InteractionMode newMode) {
        interactionMode = newMode;
        saveInteractionPreferences();
        refreshInteractionUi();
        updateStatus(getInteractionModeStatusText(), false);
    }

    private void cycleInteractionMode() {
        VoiceKeyboardConfig.InteractionMode[] modes = VoiceKeyboardConfig.InteractionMode.values();
        int nextIndex = (interactionMode.ordinal() + 1) % modes.length;
        setInteractionMode(modes[nextIndex]);
    }

    private void toggleLargeMode() {
        switch (keyboardScalePercent) {
            case VoiceKeyboardConfig.SCALE_STANDARD:
                keyboardScalePercent = VoiceKeyboardConfig.SCALE_LARGE;
                break;
            case VoiceKeyboardConfig.SCALE_LARGE:
                keyboardScalePercent = VoiceKeyboardConfig.SCALE_EXTRA_LARGE;
                break;
            case VoiceKeyboardConfig.SCALE_EXTRA_LARGE:
            default:
                keyboardScalePercent = VoiceKeyboardConfig.SCALE_STANDARD;
                break;
        }
        saveInteractionPreferences();
        refreshInteractionUi();
        updateStatus(getScaleStatusText(), false);
    }

    private void updateModeButtons() {
        updateModeButton(dockedModeButton, interactionMode == VoiceKeyboardConfig.InteractionMode.DOCKED);
        updateModeButton(freeModeButton, interactionMode == VoiceKeyboardConfig.InteractionMode.FREE);
        updateModeButton(
                followPointerButton,
                interactionMode == VoiceKeyboardConfig.InteractionMode.FOLLOW_POINTER);
        updateModeButton(largeModeButton, keyboardScalePercent > VoiceKeyboardConfig.SCALE_STANDARD);
        updateModeButton(moveToPointerButton, interactionMode != VoiceKeyboardConfig.InteractionMode.DOCKED);
        updateModeButton(standardInputButton, keyboardInputMode == KeyboardInputMode.STANDARD);
        updateModeButton(ziyouInputButton, keyboardInputMode == KeyboardInputMode.ZIYOU);
        updateModeButton(shiftButton, isUppercase);

        if (modeSummaryView != null) {
            modeSummaryView.setText(getModeSummaryText());
        }
    }

    private void updateModeButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.setAlpha(active ? 1f : 0.55f);
    }

    private String getModeSummaryText() {
        String modeName;
        switch (interactionMode) {
            case FREE:
                modeName = getString(R.string.voice_keyboard_mode_free);
                break;
            case FOLLOW_POINTER:
                modeName = getString(R.string.voice_keyboard_mode_follow_pointer);
                break;
            case DOCKED:
            default:
                modeName = getString(R.string.voice_keyboard_mode_docked);
                break;
        }
        return getString(R.string.voice_keyboard_mode_summary, modeName, keyboardScalePercent, getThemeLabel());
    }

    private String getThemeLabel() {
        switch (keyboardTheme) {
            case CLASSIC:
                return getString(R.string.voice_keyboard_theme_classic);
            case GAMEFACE:
            default:
                return getString(R.string.voice_keyboard_theme_gameface);
        }
    }

    private String getInteractionModeStatusText() {
        switch (interactionMode) {
            case FREE:
                return getString(R.string.voice_keyboard_status_free_mode);
            case FOLLOW_POINTER:
                return getString(R.string.voice_keyboard_status_follow_mode);
            case DOCKED:
            default:
                return getString(R.string.voice_keyboard_status_docked_mode);
        }
    }

    private String getScaleStatusText() {
        return getString(R.string.voice_keyboard_status_scale_changed, keyboardScalePercent);
    }

    private void applyPanelScale() {
        if (keyboardPanel == null) {
            return;
        }
        float scale = keyboardScalePercent / 100f;
        keyboardPanel.setScaleX(scale);
        keyboardPanel.setScaleY(scale);
    }

    private void applyKeyboardTheme() {
        if (keyboardPanel == null || keyboardRoot == null || resultView == null) {
            return;
        }
        switch (keyboardTheme) {
            case CLASSIC:
                keyboardRoot.setBackgroundColor(getColor(android.R.color.white));
                keyboardPanel.setBackgroundResource(R.drawable.voice_keyboard_panel_classic);
                resultView.setBackgroundResource(R.drawable.voice_keyboard_preview_classic);
                break;
            case GAMEFACE:
            default:
                keyboardRoot.setBackgroundColor(getColor(R.color.light_blue));
                keyboardPanel.setBackgroundResource(R.drawable.voice_keyboard_panel_gameface);
                resultView.setBackgroundResource(R.drawable.custom_binding_linear);
                break;
        }
    }

    private boolean handlePanelDrag(MotionEvent motionEvent) {
        if (keyboardPanel == null
                || keyboardRoot == null
                || interactionMode != VoiceKeyboardConfig.InteractionMode.FREE) {
            return false;
        }
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartRawX = motionEvent.getRawX();
                dragStartRawY = motionEvent.getRawY();
                dragStartPanelX = keyboardPanel.getTranslationX();
                dragStartPanelY = keyboardPanel.getTranslationY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = motionEvent.getRawX() - dragStartRawX;
                float deltaY = motionEvent.getRawY() - dragStartRawY;
                movePanelTo(dragStartPanelX + deltaX, dragStartPanelY + deltaY, false);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                persistCurrentPanelPosition();
                return true;
            default:
                return false;
        }
    }

    private void movePanelTo(float targetX, float targetY, boolean persist) {
        if (keyboardPanel == null || keyboardRoot == null) {
            return;
        }
        keyboardPanel.setTranslationX(clampPanelX(targetX));
        keyboardPanel.setTranslationY(clampPanelY(targetY));
        if (persist) {
            persistCurrentPanelPosition();
        }
    }

    private float clampPanelX(float targetX) {
        float maxX = Math.max(0f, keyboardRoot.getWidth() - getScaledPanelWidth());
        return Math.max(0f, Math.min(targetX, maxX));
    }

    private float clampPanelY(float targetY) {
        float maxY = Math.max(0f, keyboardRoot.getHeight() - getScaledPanelHeight());
        return Math.max(0f, Math.min(targetY, maxY));
    }

    private float getScaledPanelWidth() {
        float scale = keyboardScalePercent / 100f;
        return keyboardPanel.getWidth() * scale;
    }

    private float getScaledPanelHeight() {
        float scale = keyboardScalePercent / 100f;
        return keyboardPanel.getHeight() * scale;
    }

    private void applyStoredPanelPosition() {
        if (keyboardPanel == null || keyboardRoot == null) {
            return;
        }
        float maxX = Math.max(0f, keyboardRoot.getWidth() - getScaledPanelWidth());
        float maxY = Math.max(0f, keyboardRoot.getHeight() - getScaledPanelHeight());
        keyboardPanel.setTranslationX(maxX * panelNormalizedX);
        keyboardPanel.setTranslationY(maxY * panelNormalizedY);
    }

    private void persistCurrentPanelPosition() {
        if (keyboardPanel == null || keyboardRoot == null) {
            return;
        }
        float maxX = Math.max(1f, keyboardRoot.getWidth() - getScaledPanelWidth());
        float maxY = Math.max(1f, keyboardRoot.getHeight() - getScaledPanelHeight());
        panelNormalizedX = keyboardPanel.getTranslationX() / maxX;
        panelNormalizedY = keyboardPanel.getTranslationY() / maxY;
        saveInteractionPreferences();
    }

    private void snapPanelToPointer() {
        interactionMode = VoiceKeyboardConfig.InteractionMode.FREE;
        saveInteractionPreferences();
        refreshInteractionUi();
        if (keyboardPanel == null || keyboardRoot == null) {
            return;
        }
        float panelWidth = getScaledPanelWidth();
        float panelHeight = getScaledPanelHeight();
        float targetX = ((latestCursorX / (float) latestScreenWidth) * keyboardRoot.getWidth())
                - (panelWidth / 2f);
        float targetY = ((latestCursorY / (float) latestScreenHeight) * keyboardRoot.getHeight())
                - (panelHeight / 2f);
        movePanelTo(targetX, targetY, true);
        updateStatus(getString(R.string.voice_keyboard_status_move_to_pointer), true);
    }

    private void applyFollowPointerPosition() {
        if (keyboardPanel == null || keyboardRoot == null) {
            return;
        }
        float panelWidth = getScaledPanelWidth();
        float panelHeight = getScaledPanelHeight();
        float maxY = Math.max(0f, keyboardRoot.getHeight() - panelHeight);
        float pointerX = (latestCursorX / (float) latestScreenWidth) * keyboardRoot.getWidth();
        float pointerY = (latestCursorY / (float) latestScreenHeight) * keyboardRoot.getHeight();

        float targetX;
        float targetY;
        switch (followPointerStrategy) {
            case OFFSET_ABOVE_POINTER:
                float offset = getResources().getDisplayMetrics().density * 24f;
                targetX = pointerX - (panelWidth / 2f);
                targetY = pointerY - panelHeight - offset;
                break;
            case EDGE_DOCK:
                boolean pointerOnLeft = pointerX < (keyboardRoot.getWidth() / 2f);
                float edgeMargin = getResources().getDisplayMetrics().density * 12f;
                targetX = pointerOnLeft
                        ? keyboardRoot.getWidth() - panelWidth - edgeMargin
                        : edgeMargin;
                targetY = (latestCursorY / (float) latestScreenHeight) * maxY;
                break;
            case CENTER_ON_POINTER:
            default:
                targetX = pointerX - (panelWidth / 2f);
                targetY = pointerY - (panelHeight / 2f);
                break;
        }
        keyboardPanel.setTranslationX(clampPanelX(targetX));
        keyboardPanel.setTranslationY(clampPanelY(targetY));
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus(getString(R.string.voice_keyboard_status_unavailable), true);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus(getString(R.string.voice_keyboard_status_no_permission), true);
            return;
        }
        ensureSpeechRecognizer();
        if (speechRecognizer == null) {
            updateStatus(getString(R.string.voice_keyboard_status_unavailable), true);
            return;
        }
        isListening = true;
        updateMicButton();
        updateStatus(getString(R.string.voice_keyboard_status_listening), true);
        preparePreviewForListening();
        speechRecognizer.startListening(recognizerIntent);
    }

    private void preparePreviewForListening() {
        if (keyboardInputMode == KeyboardInputMode.ZIYOU) {
            resetPreviewPlaceholder();
            return;
        }
        updatePreview("");
    }

    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
        }
        isListening = false;
        updateMicButton();
    }

    private void ensureSpeechRecognizer() {
        if (speechRecognizer != null) {
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
    }

    private void updateMicButton() {
        if (micButton == null) {
            return;
        }
        if (isListening) {
            micButton.setText(R.string.voice_keyboard_button_stop);
            micButton.setContentDescription(getString(R.string.voice_keyboard_button_stop_description));
        } else {
            micButton.setText(R.string.voice_keyboard_button_mic);
            micButton.setContentDescription(getString(R.string.voice_keyboard_button_mic_description));
        }
    }

    private void updateStatus(String status, boolean speakFeedback) {
        if (statusView != null) {
            statusView.setText(status);
        }
        if (speakFeedback && isTtsReady) {
            textToSpeech.speak(status, TextToSpeech.QUEUE_FLUSH, null, "voice_keyboard_status");
        }
    }

    private void updatePreview(String preview) {
        if (resultView != null) {
            resultView.setText(preview);
        }
    }

    private void commitText(String text, boolean speakFeedback) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            updateStatus(getString(R.string.voice_keyboard_status_error), true);
            return;
        }
        inputConnection.commitText(text, 1);
        updateStatus(getString(R.string.voice_keyboard_status_committed), speakFeedback);
    }

    private void deletePreviousCharacter() {
        if (keyboardInputMode == KeyboardInputMode.ZIYOU) {
            removeLastCompositionToken();
            return;
        }
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.deleteSurroundingText(1, 0);
        }
    }

    private void commitPreviewText() {
        String preview = keyboardInputMode == KeyboardInputMode.ZIYOU
            ? ziyouInputState.getTrimmedCompositionPreview()
                : (resultView == null ? "" : resultView.getText().toString().trim());
        if (preview.isEmpty()
                || preview.equals(getString(R.string.voice_keyboard_result_placeholder))
                || preview.equals(getString(R.string.voice_keyboard_result_ziyou_placeholder))) {
            return;
        }
        commitText(preview, true);
        updateStatus(getString(R.string.voice_keyboard_status_confirmed), true);
        ziyouInputState.clearComposition();
        resetPreviewPlaceholder();
        showAssociationSuggestions(preview);
    }

    private void commitRecognitionResult(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            updateStatus(getString(R.string.voice_keyboard_status_error), true);
            return;
        }
        String bestMatch = matches.get(0);
        updatePreview(bestMatch);
        commitText(bestMatch + " ", true);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        updateStatus(getString(R.string.voice_keyboard_status_listening), false);
    }

    @Override
    public void onBeginningOfSpeech() {
        updateStatus(getString(R.string.voice_keyboard_status_listening), false);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
        isListening = false;
        updateMicButton();
    }

    @Override
    public void onError(int error) {
        isListening = false;
        updateMicButton();
        updateStatus(getString(R.string.voice_keyboard_status_error), true);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        updateMicButton();
        commitRecognitionResult(results);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches =
                partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            updatePreview(matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int languageResult = textToSpeech.setLanguage(Locale.getDefault());
            isTtsReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                    && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
        } else {
            isTtsReady = false;
        }
    }

    @Override
    public void onDestroy() {
        stopListening();
        unregisterCursorPositionReceiver();
        unregisterImeCommandReceiver();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        super.onDestroy();
    }
}