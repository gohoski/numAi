package io.github.gohoski.numai.api;

/** Result of one native Gemini image request. */
public class GeminiImageResult {
    private final String imageData;
    private final String mimeType;
    private final String text;

    public GeminiImageResult(String imageData, String mimeType, String text) {
        this.imageData = imageData;
        this.mimeType = mimeType;
        this.text = text;
    }

    public String getImageData() { return imageData; }
    public String getMimeType() { return mimeType; }
    public String getText() { return text; }
}
