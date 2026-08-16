package io.github.gohoski.numai.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import io.github.gohoski.numai.R;
import io.github.gohoski.numai.data.ConfigManager;

/**
 * Native Gemini Interactions API client for image generation and editing.
 * It intentionally uses a distinct, user-supplied key; no key is embedded in
 * the application or sent to an OpenAI-compatible provider.
 */
public class GeminiImageService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final int MAX_RESPONSE_IMAGE_BASE64_CHARS = 7000000;

    private final ApiClient apiClient;
    private final ConfigManager config;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GeminiImageService(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new ApiClient(context);
        this.config = ConfigManager.getInstance(context);
    }

    public void generate(final String prompt, final List<String> inputImages,
                         final ApiCallback<GeminiImageResult> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String key = config.getGeminiImageApiKey();
                if (key == null || key.length() == 0) {
                    deliverError(callback, new ApiError(context.getString(R.string.gemini_image_key_required)));
                    return;
                }

                try {
                    ApiRequest request = new ApiRequest(GEMINI_BASE_URL, "/interactions", "POST");
                    request.addHeader("x-goog-api-key", key);
                    request.addHeader("Content-Type", "application/json");
                    request.setReadTimeout(90000);
                    request.setBody(buildRequestBody(prompt, inputImages).toString());

                    ApiResponse response = apiClient.execute(request);
                    String responseText = apiClient.readInputStreamToString(response.getBody());
                    if (!response.isSuccessful()) {
                        deliverError(callback, new ApiError(context.getString(R.string.gemini_image_failed,
                                response.getStatusCode() + " " + extractErrorMessage(responseText))));
                        return;
                    }

                    GeminiImageResult result = parseResult(responseText);
                    if (result == null || result.getImageData() == null || result.getImageData().length() == 0) {
                        deliverError(callback, new ApiError(context.getString(R.string.gemini_image_no_result)));
                    } else if (result.getImageData().length() > MAX_RESPONSE_IMAGE_BASE64_CHARS) {
                        deliverError(callback, new ApiError(context.getString(R.string.gemini_image_too_large)));
                    } else {
                        deliverSuccess(callback, result);
                    }
                } catch (ApiError e) {
                    deliverError(callback, e);
                } catch (Exception e) {
                    deliverError(callback, new ApiError(context.getString(R.string.gemini_image_failed,
                            e.getMessage() == null ? "unknown error" : e.getMessage())));
                }
            }
        }).start();
    }

    /**
     * Reads the models available to the user's Gemini key and returns only
     * native Gemini image models supported by this app's Interactions request.
     */
    public void getAvailableImageModels(final ApiCallback<ArrayList<String>> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String key = config.getGeminiImageApiKey();
                if (key == null || key.length() == 0) {
                    deliverError(callback, new ApiError(context.getString(R.string.gemini_image_key_required)));
                    return;
                }

                try {
                    ArrayList<String> models = new ArrayList<String>();
                    Set<String> seenModels = new HashSet<String>();
                    String pageToken = null;
                    int pageCount = 0;

                    do {
                        ApiRequest request = new ApiRequest(GEMINI_BASE_URL, "/models", "GET");
                        request.addHeader("x-goog-api-key", key);
                        request.addParam("pageSize", "1000");
                        if (pageToken != null && pageToken.length() > 0) {
                            request.addParam("pageToken", pageToken);
                        }
                        request.setReadTimeout(30000);

                        ApiResponse response = apiClient.execute(request);
                        String responseText = apiClient.readInputStreamToString(response.getBody());
                        if (!response.isSuccessful()) {
                            deliverError(callback, new ApiError(context.getString(R.string.gemini_image_models_failed,
                                    response.getStatusCode() + " " + extractErrorMessage(responseText))));
                            return;
                        }

                        JSONObject root = JSON.getObject(responseText);
                        JSONArray listedModels = root.getNullableArray("models");
                        if (listedModels != null) {
                            for (int i = 0; i < listedModels.size(); i++) {
                                JSONObject model = listedModels.getObject(i);
                                if (model == null) continue;
                                String modelName = model.getNullableString("name");
                                if (modelName == null || modelName.length() == 0) continue;
                                if (modelName.startsWith("models/")) {
                                    modelName = modelName.substring("models/".length());
                                }
                                if (isSupportedImageModel(modelName) && !seenModels.contains(modelName)) {
                                    seenModels.add(modelName);
                                    models.add(modelName);
                                }
                            }
                        }
                        pageToken = root.getNullableString("nextPageToken");
                        pageCount++;
                    } while (pageToken != null && pageToken.length() > 0 && pageCount < 10);

                    if (models.isEmpty()) {
                        deliverError(callback, new ApiError(context.getString(R.string.gemini_image_models_empty)));
                    } else {
                        deliverSuccess(callback, models);
                    }
                } catch (ApiError e) {
                    deliverError(callback, e);
                } catch (Exception e) {
                    deliverError(callback, new ApiError(context.getString(R.string.gemini_image_models_failed,
                            e.getMessage() == null ? "unknown error" : e.getMessage())));
                }
            }
        }).start();
    }

    private boolean isSupportedImageModel(String modelName) {
        String lowerName = modelName == null ? "" : modelName.toLowerCase();
        // The native Interactions API used by this app accepts Gemini's image family.
        // Do not include Imagen here: it uses a different API request format.
        return lowerName.startsWith("gemini-") && lowerName.endsWith("-image");
    }

    private JSONObject buildRequestBody(String prompt, List<String> inputImages) {
        JSONObject body = new JSONObject();
        body.put("model", config.getGeminiImageModel());
        // Do not retain private prompts or reference photos in Gemini's interaction history.
        body.put("store", false);

        JSONArray input = new JSONArray();
        JSONObject text = new JSONObject();
        text.put("type", "text");
        text.put("text", prompt == null ? "" : prompt);
        input.add(text);

        if (inputImages != null) {
            for (int i = 0; i < inputImages.size(); i++) {
                String base64 = ApiService.getBase64FromFilename(context, inputImages.get(i));
                if (base64 == null) continue;
                int comma = base64.indexOf(',');
                JSONObject image = new JSONObject();
                image.put("type", "image");
                image.put("mime_type", "image/jpeg");
                image.put("data", comma >= 0 ? base64.substring(comma + 1) : base64);
                input.add(image);
            }
        }
        body.put("input", input);

        // A 512px output avoids exhausting RAM or storage on older phones and watches.
        // Gemini's REST API accepts the literal value "512", not "0.5K".
        JSONObject format = new JSONObject();
        format.put("type", "image");
        format.put("mime_type", "image/jpeg");
        format.put("image_size", "512");
        body.put("response_format", format);
        return body;
    }

    private GeminiImageResult parseResult(String rawResponse) throws Exception {
        JSONObject response = JSON.getObject(rawResponse);
        JSONArray steps = response.getNullableArray("steps");
        if (steps == null) return null;

        String imageData = null;
        String mimeType = "image/jpeg";
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getObject(i);
            if (step == null || !"model_output".equals(step.getNullableString("type"))) continue;
            JSONArray content = step.getNullableArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.size(); j++) {
                JSONObject part = content.getObject(j);
                if (part == null) continue;
                String type = part.getNullableString("type");
                if ("image".equals(type)) {
                    String data = part.getNullableString("data");
                    if (data != null && data.length() > 0) {
                        imageData = data;
                        String returnedMime = part.getNullableString("mime_type");
                        if (returnedMime != null && returnedMime.length() > 0) mimeType = returnedMime;
                    }
                } else if ("text".equals(type)) {
                    String block = part.getNullableString("text");
                    if (block != null && block.length() > 0) {
                        if (text.length() > 0) text.append("\n");
                        text.append(block);
                    }
                }
            }
        }
        return imageData == null ? null : new GeminiImageResult(imageData, mimeType, text.toString());
    }

    private String extractErrorMessage(String rawResponse) {
        try {
            JSONObject root = JSON.getObject(rawResponse);
            JSONObject error = root.getObject("error");
            if (error != null) {
                String message = error.getNullableString("message");
                if (message != null && message.length() > 0) return message;
            }
        } catch (Exception ignored) {}
        return rawResponse == null || rawResponse.length() == 0 ? "no response" : rawResponse;
    }

    private <T> void deliverSuccess(final ApiCallback<T> callback, final T result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() { callback.onSuccess(result); }
        });
    }

    private <T> void deliverError(final ApiCallback<T> callback, final ApiError error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() { callback.onError(error); }
        });
    }
}
