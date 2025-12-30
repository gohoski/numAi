package io.github.gohoski.numai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Use this class to change preferences
 *
 * Handles loading/saving user's settings and validates configuration format.
 */
class ConfigManager {
    private static final String PREFS_NAME = "numAi",
        KEY_BASE_URL = "baseUrl",
        KEY_API_KEY = "apiKey",
        KEY_CHAT_MODEL = "chatModel",
        KEY_THINKING_MODEL = "thinkingModel",
        KEY_SHRINK_THINK = "shrinkThink",
        KEY_SYSTEM_PROMPT = "systemPrompt",
        KEY_UPDATE_DELAY = "updateDelay";

    private static ConfigManager instance;
    private final SharedPreferences preferences;
    private Config config;

    private ConfigManager(Context appContext) {
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        config = loadConfig();
    }

    // Get singleton instance
    static synchronized ConfigManager getInstance(Context context) {
        if (instance == null)
            instance = new ConfigManager(context.getApplicationContext());
        return instance;
    }

    static ConfigManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("ConfigManager not initialized; call getInstance(Context) first");
        return instance;
    }

    // Load configuration from SharedPreferences
    private Config loadConfig() {
        return new Config(preferences.getString(KEY_BASE_URL, "https://api.voidai.app/v1"),
            preferences.getString(KEY_API_KEY, ""),
            preferences.getString(KEY_CHAT_MODEL, ""),
            preferences.getString(KEY_THINKING_MODEL, ""),
            preferences.getBoolean(KEY_SHRINK_THINK, false),
            preferences.getString(KEY_SYSTEM_PROMPT, ""),
            preferences.getInt(KEY_UPDATE_DELAY, 250));
    }

    // Save configuration to SharedPreferences
    private void saveConfig() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_BASE_URL, config.getBaseUrl());
        editor.putString(KEY_API_KEY, config.getApiKey());
        editor.putString(KEY_CHAT_MODEL, config.getChatModel());
        editor.putString(KEY_THINKING_MODEL, config.getThinkingModel());
        editor.putBoolean(KEY_SHRINK_THINK, config.getShrinkThink());
        editor.putString(KEY_SYSTEM_PROMPT, config.getSystemPrompt());
        editor.putInt(KEY_UPDATE_DELAY, config.getUpdateDelay());
        editor.commit(); //apply() doesn't exist in Android 1.0
    }

    Config getConfig() {
        return config;
    }

    void setConfig(Config config) {
        this.config = config;
        saveConfig();
    }

    void updateBaseUrl(String baseUrl) {
        config.setBaseUrl(baseUrl);
        saveConfig();
    }

    void updateApiKey(String apiKey) {
        config.setApiKey(apiKey);
        saveConfig();
    }

    void updateChatModel(String model) {
        config.setChatModel(model);
        saveConfig();
    }
    void updateThinkingModel(String model) {
        config.setThinkingModel(model);
        saveConfig();
    }

    void updateSystemPrompt(String systemPrompt) {
        config.setSystemPrompt(systemPrompt);
        saveConfig();
    }

    public boolean isConfigValid() {
        return config.isValid();
    }
}