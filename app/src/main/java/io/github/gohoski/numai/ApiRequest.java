package io.github.gohoski.numai;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class ApiRequest {
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private String body;

    ApiRequest(String endpoint, String method) {
        this.endpoint = endpoint;
        this.method = method;
        this.headers = new HashMap<String, String>();
        this.body = "";
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    // Original String-based method
    void setBody(String body) {
        this.body = body;
    }

    // Getters
    String getEndpoint() { return endpoint; }
    String getMethod() { return method; }
    Map<String, String> getHeaders() { return headers; }
    String getBody() { return body; }
}