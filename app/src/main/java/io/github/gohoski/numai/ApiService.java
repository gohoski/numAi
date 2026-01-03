package io.github.gohoski.numai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Created by Gleb on 21.08.2025.
 * бля как же мне лень эту залупу кодить
 */
class ApiService {
    private final ApiClient apiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ConfigManager config;
    private Context ctx;

    ApiService(Context context) {
        this.apiClient = new ApiClient(context);
        this.config = ConfigManager.getInstance(context);
        ctx = context;
    }

    void chatCompletion(final List<Message> msg, final boolean thinking, final ApiCallback<ApiResult> callback) {
        System.out.println("chatCompletion...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApiRequest request = new ApiRequest("/chat/completions?include_think=true", "POST");
                    JSONArray messages = new JSONArray();
                    String systemStr = config.getConfig().getSystemPrompt();
                    if (systemStr.length() != 0) {
                        JSONObject system = new JSONObject();
                        system.put("role", "system");
                        system.put("content", systemStr);
                        messages.add(system);
                    }
                    boolean hasImg = false;
                    //convert all messages to json
                    for (Message message : msg) {
                        JSONObject messageJson = new JSONObject();
                        messageJson.put("role", message.getRole());
                        List<String> inputImages = message.getInputImages();
                        if (inputImages == null || inputImages.isEmpty()) {
                            messageJson.put("content", message.getContent());
                        } else {
                            hasImg = true;
                            JSONArray content = new JSONArray();
                            JSONObject inputText = new JSONObject();
                            inputText.put("type", "text");
                            inputText.put("text", message.getContent());
                            content.add(inputText);
                            for (String image: inputImages) {
                                JSONObject input = new JSONObject();
                                input.put("type", "image_url");
                                JSONObject imageUrl = new JSONObject();
                                imageUrl.put("url", image);
                                input.put("image_url", imageUrl);
                                content.add(input);
                            }
                            messageJson.put("content", content);
                        }
                        messages.add(messageJson);
                    }
                    JSONObject body = new JSONObject();
                    final String model = thinking ? config.getConfig().getThinkingModel() : config.getConfig().getChatModel();
                    body.put("model", model);
                    body.put("messages", messages);
                    body.put("stream", true);
                    if (thinking) {
                        //apparently the OpenAI Chat Completions API format still doesn't have proper reasoning support.
                        // I would've used Responses API if possible, but very few platforms support it.
                        // Flags are different on each API so this needs to be constantly changed in the future...
                        switch (config.getConfig().getBaseUrl()) {
                            case "https://openrouter.ai/api/v1":
                                JSONObject reasoning = new JSONObject();
                                reasoning.put("enabled", true);
                                body.put("reasoning", reasoning);
                                break;
                            case "https://api.together.xyz/v1":
                                JSONObject kw = new JSONObject();
                                kw.put("thinking", true);
                                body.put("chat_template_kwargs", kw);
                                break;
                            case "https://dashscope.aliyuncs.com/compatible-mode/v1":
                            case "https://dashscope-intl.aliyuncs.com/compatible-mode/v1":
                                body.put("enable_thinking", true); break;
                            default:
                                body.put("reasoning_effort", "high");
                        }
                    }
                    request.setBody(body.toString());

                    ApiResponse response = apiClient.execute(request);
                    if (response.isSuccessful()) {
                        deliverSuccess(callback, new ApiResult(model, response.getBody()));
                    } else {
                        String errorBody = "no body";
                        try {
                            errorBody = apiClient.readInputStreamToString(response.getBody());
                        } catch(IOException ignored) {}
                        deliverError(callback, new ApiError(ctx.getString(hasImg ? R.string.fail_send_vision : R.string.fail_send, response.getStatusCode() + " " + errorBody)));
                    }
                } catch (ApiError e) {
                    deliverError(callback, e);
                }
            }
        }).start();
    }

    void getModels(final ApiCallback<ArrayList<String>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApiRequest request = new ApiRequest("/models", "GET");
                    String response = apiClient.executeAsString(request);
                    ArrayList<String> models = new ArrayList<>();

                    JSONObject resp = JSON.getObject(response);
                    if (resp.has("error"))
                        deliverError(callback, new ApiError(resp.getObject("error").getString("message")));
                    else {
                        JSONArray json = resp.getArray("data");
                        for (int i = 0; i < json.size(); i++) {
                            JSONObject model = json.getObject(i);
                            if (model.has("endpoints")) {
                                if (model.getArray("endpoints").has("/v1/chat/completions"))
                                    models.add(json.getObject(i).getString("id"));
                            } else
                                models.add(json.getObject(i).getString("id"));
                        }
                        deliverSuccess(callback, models);
                    }
                } catch (ApiError e) {
                    deliverError(callback, e);
                }
            }
        }).start();
    }

    private <T> void deliverSuccess(final ApiCallback<T> callback, final T result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result);
            }
        });
    }

    private <T> void deliverError(final ApiCallback<T> callback, final ApiError error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                error.printStackTrace();
                callback.onError(error);
            }
        });
    }
}