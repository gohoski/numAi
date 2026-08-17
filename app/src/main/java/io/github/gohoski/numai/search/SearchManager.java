package io.github.gohoski.numai.search;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import io.github.gohoski.numai.api.ApiCallback;
import io.github.gohoski.numai.api.ApiError;
import io.github.gohoski.numai.data.ConfigManager;

import java.util.List;

/**
 * Created by Gleb on 30.07.2026.
 */

public class SearchManager {
    private static SearchManager instance;

    private SearchManager() {}

    public static synchronized SearchManager getInstance() {
        if (instance == null) {
            instance = new SearchManager();
        }
        return instance;
    }

    public SearchEngine getEngine(Context context) {
        String name = ConfigManager.getInstance(context).getConfig().getSearchEngine();
        if ("duckduckgo".equalsIgnoreCase(name))
            return new DuckDuckGo();
        if ("yandex".equalsIgnoreCase(name))
            return new Yandex();
        return new Bing();
    }

    public void executeSearch(final Context context, final String query, final ApiCallback<List<SearchResult>> callback) {
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SearchEngine engine = getEngine(context);
                    final List<SearchResult> results = engine.search(query);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(results);
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new ApiError(e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }
}