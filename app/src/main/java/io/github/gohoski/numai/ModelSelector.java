package io.github.gohoski.numai;

import java.util.List;
import java.util.Arrays;

/**
 * Created by Gleb on 27.09.2025.
 *
 * Automatic selection of models
 */

class ModelSelector {
    //Lists based on price, knowledge and overall usability (e.g. DSv3.2 although is much better than v3.1 it isn't
    // available for chatting on some APIs, while GLM are very powerful models, they don't have good multilingual support,etc.)
    private static final List<String> CHAT_MODELS = Arrays.asList(
            "deepseek-v3.1:671b", "deepseek-v3.2", "deepseek-v3.1", "gemini-3-flash",
            "qwen3-max", "qwen3-vl", "qwen3-next", "kimi-k2", "glm-4.7", "glm-4.6",
            "qwen3-235b", "grok-4-fast", "grok-4", "grok-3", "gpt-5-nano"
    );
    private static final List<String> THINKING_MODELS = Arrays.asList(
            "deepseek-v3.2", "gemini-3-pro", "glm-4.7", "deepseek-v3.1-terminus", "deepseek-v3.1",
            "qwen3-vl", "qwen3-235b-a22b-thinking-2507", "qwen3-next-80b-a3b-thinking", "gemini-2.5-pro",
            "glm-4.6", "grok-4", "grok-3-mini", "solar-pro2", "gpt-5-nano"
    );

    /**
     * Selects the first preferred chat model that is included in any of the available models.
     *
     * @param availableModels List of models available from the API
     * @return The name of the preferred model that was found, or first available if none found
     */
    static String selectChatModel(List<String> availableModels) {
        return selectPreferredModel(availableModels, CHAT_MODELS);
    }

    /**
     * Selects the first preferred thinking model that is included in any of the available models.
     *
     * @param availableModels List of models available from the API
     * @return The name of the preferred thinking model that was found, or first available if none found
     */
    static String selectThinkingModel(List<String> availableModels) {
        return selectPreferredModel(availableModels, THINKING_MODELS);
    }

    /**
     * Generic method to select the first preferred model from available models.
     * Uses contains() for partial matching of model names in a case-insensitive manner.
     *
     * @param availableModels List of models available from the API
     * @param prefModels List of preferred models in priority order
     * @return The name of the preferred model that was found, or first available if none found
     */
    private static String selectPreferredModel(List<String> availableModels, List<String> prefModels) {
        System.out.println(availableModels);

        if (availableModels == null || availableModels.size() == 0) {
            return null;
        }
        if (prefModels == null || prefModels.size() == 0) {
            return availableModels.get(0);
        }

        // Look for preferred models in order of preference
        for (String prefModel : prefModels) {
            for (String availableModel : availableModels) {
                if (availableModel != null && prefModel != null &&
                        availableModel.toLowerCase().contains(prefModel)) {
                    return availableModel;
                }
            }
        }

        // If no preferred model is available, return the first one
        return availableModels.get(0);
    }
}