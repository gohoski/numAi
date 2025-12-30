package io.github.gohoski.numai;

/**
 * Created by Gleb on 08.10.2025.
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ApiManager {
    private static final Map<String, String>
            NAME_TO_URL = new LinkedHashMap<String, String>(),
            URL_TO_NAME = new LinkedHashMap<String, String>();

    static {
        addApi("VoidAI", "https://api.voidai.app/v1");
        addApi("Ollama", "https://ollama.com/v1");
        addApi("NavyAI", "https://api.navy/v1");
        addApi("OpenRouter","https://openrouter.ai/api/v1");
        addApi("Baseten","https://inference.baseten.co/v1");
        addApi("Gemini","https://generativelanguage.googleapis.com/v1beta/openai");
        addApi("Together", "https://api.together.xyz/v1");
        addApi("Upstage", "https://api.upstage.ai/v1");
    }

    private static void addApi(String name, String url) {
        NAME_TO_URL.put(name, url);
        URL_TO_NAME.put(url, name);
    }

    static String getUrlByName(String name) {
        String s = NAME_TO_URL.get(name);
        if (s == null)
            return name;
        return s;
    }

    static String getNameByUrl(String url) {
        return URL_TO_NAME.get(url);
    }

    static List<String> getAllApiNames() {
        return new ArrayList<String>(NAME_TO_URL.keySet());
    }
}