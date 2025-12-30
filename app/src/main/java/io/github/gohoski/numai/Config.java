package io.github.gohoski.numai;

/**
 * Created by Gleb on 21.08.2025.
 *
 * ! Use ConfigManager for managing !
 */
class Config {
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String thinkingModel;
    private boolean shrinkThink;
    private String systemPrompt;
    private int updateDelay;

    // Empty constructor for loading from SharedPreferences
    Config() {}

    // Constructor for creating new config
    Config(String baseUrl, String apiKey, String chatModel, String thinkingModel, boolean shrinkThink, String systemPrompt, int updateDelay) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.thinkingModel = thinkingModel;
        this.shrinkThink = shrinkThink;
        this.systemPrompt = systemPrompt;
        this.updateDelay = updateDelay;
    }

    String getBaseUrl() { return baseUrl; }
    void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    String getApiKey() { return apiKey; }
    void setApiKey(String apiKey) { this.apiKey = apiKey; }

    String getChatModel() { return chatModel; }
    void setChatModel(String chatModel) { this.chatModel = chatModel; }

    String getThinkingModel() { return thinkingModel; }
    void setThinkingModel(String thinkingModel) { this.thinkingModel = thinkingModel; }

    boolean getShrinkThink() { return shrinkThink; }
    void setShrinkThink(boolean shrinkThink) { this.shrinkThink = shrinkThink; }

    String getSystemPrompt() { return systemPrompt; }
    void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    int getUpdateDelay() { return updateDelay; }
    void setUpdateDelay(int updateDelay) { this.updateDelay = updateDelay; }

    boolean isValid() {
        return baseUrl != null && baseUrl.trim().length() != 0 &&
                apiKey != null && apiKey.trim().length() != 0 &&
                chatModel != null && chatModel.trim().length() != 0;
    }
}