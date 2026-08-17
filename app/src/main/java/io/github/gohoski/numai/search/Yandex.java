package io.github.gohoski.numai.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.numai.api.ApiClient;
import io.github.gohoski.numai.api.ApiError;
import io.github.gohoski.numai.api.ApiRequest;
import io.github.gohoski.numai.api.ApiResponse;

/**
 * Created by ququnta on 17.08.2026.
 * Yandex Search.
 */
class Yandex implements SearchEngine {
    private final ApiClient api;

    Yandex() {
        this.api = new ApiClient(null);
    }

    @Override
    public List<SearchResult> search(String query) throws SearchException, ApiError, IOException {
        if (query == null || query.trim().length() == 0) {
            return new ArrayList<SearchResult>();
        }

        ApiRequest request = new ApiRequest("http://yandex.ru", "/search/site/", "GET")
                .addParam("searchid", "1")
                .addParam("web", "1")
                .addParam("text", query)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.3; en-us) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8");

        ApiResponse response = api.execute(request);
        if (!response.isSuccessful()) {
            throw new SearchException("Yandex HTTP Error Response: " + response.getStatusCode());
        }
        String html = readResponseAsString(response);
        return parse(html);
    }

    private String readResponseAsString(ApiResponse response) throws IOException {
        if (response == null || response.getBody() == null) {
            return "";
        }
        InputStream is = response.getBody();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {}
        }
    }

    private List<SearchResult> parse(String html) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        if (html == null || html.trim().length() == 0) {
            return results;
        }

        HtmlParser.Node root = HtmlParser.parseHtmlTree(html);
        List<HtmlParser.Node> items = new ArrayList<HtmlParser.Node>();
        findResultItems(root, items);

        for (int i = 0; i < items.size(); i++) {
            HtmlParser.Node item = items.get(i);

            HtmlParser.Node titleLink = findNodeByClass(item, "b-serp-item__title-link");
            String title = "";
            String url = "";
            if (titleLink != null) {
                title = extractText(titleLink);
                url = titleLink.getAttribute("href");
            }

            HtmlParser.Node snippetNode = findNodeByClass(item, "b-serp-item__text");
            String snippet = snippetNode != null ? extractText(snippetNode) : "";

            if (url == null) {
                url = "";
            }

            if (title.length() > 0 || url.length() > 0) {
                results.add(new SearchResult(title, url, snippet));
            }
        }

        return results;
    }

    private void findResultItems(HtmlParser.Node node, List<HtmlParser.Node> result) {
        if ("li".equals(node.tagName) && node.hasClass("b-serp-item")) {
            result.add(node);
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            findResultItems(node.children.get(i), result);
        }
    }

    private HtmlParser.Node findNodeByClass(HtmlParser.Node node, String className) {
        if (node.hasClass(className)) {
            return node;
        }
        for (int i = 0; i < node.children.size(); i++) {
            HtmlParser.Node res = findNodeByClass(node.children.get(i), className);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private String extractText(HtmlParser.Node node) {
        StringBuilder sb = new StringBuilder();
        collectAllText(node, sb);
        return cleanWhitespace(sb.toString());
    }

    private void collectAllText(HtmlParser.Node node, StringBuilder sb) {
        if ("#text".equals(node.tagName)) {
            if (node.text != null) {
                sb.append(node.text).append(" ");
            }
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            collectAllText(node.children.get(i), sb);
        }
    }

    private String cleanWhitespace(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length());
        boolean lastSpace = false;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastSpace) {
                    sb.append(' ');
                    lastSpace = true;
                }
            } else {
                sb.append(c);
                lastSpace = false;
            }
        }
        return sb.toString().trim();
    }
}
