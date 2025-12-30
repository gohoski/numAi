package io.github.gohoski.numai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;

/**
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://www.wtfpl.net/ for more details.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_CODE_PICK_IMAGE = 1;

    private ApiService apiService;
    private ConfigManager config;

    private ListView msgList;
    private EditText input;
    private MessageAdapter adapter;
    private ImageButton sendBtn;
    private ToggleButton thinkingToggle;
    private ProgressBar progressBar;
    private TextView imgCount;
    private ImageButton attachBtn;
    private boolean autoScroll = true;
    private boolean isGenerating = false;
    private boolean isThinkingState = false;
    private InputStream currentStream;
    private volatile boolean isCancelled = false;
    int UPDATE_DELAY_MS = 250;

    // Stream buffers
    private final StringBuilder thinkBuffer = new StringBuilder();
    private final StringBuilder contentBuffer = new StringBuilder();
    private final List<String> inputImages = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SSLDisabler.disableSSLCertificateChecking();

        config = ConfigManager.getInstance(this);
        if (config.getConfig().getApiKey().length() == 0) {
            startActivity(new Intent(this, FirstTimeActivity.class));
            finish();
            return;
        }
        UPDATE_DELAY_MS = config.getConfig().getUpdateDelay();

        apiService = new ApiService(this);
        msgList = (ListView) findViewById(R.id.messages_list);
        input = (EditText) findViewById(R.id.message_input);
        sendBtn = (ImageButton) findViewById(R.id.send_button);
        attachBtn = (ImageButton) findViewById(R.id.attach_button);
        thinkingToggle = (ToggleButton) findViewById(R.id.thinking);
        progressBar = (ProgressBar) findViewById(R.id.waiting);
        imgCount = (TextView) findViewById(R.id.img_count);

        sendBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (isGenerating) {
                    stopGeneration();
                } else {
                    sendMessage();
                }
            }
        });

        attachBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select picture"), REQUEST_CODE_PICK_IMAGE);
            }
        });

        MessageManager.getInstance().clearMessages();
        adapter = new MessageAdapter(this, MessageManager.getInstance().getMessages());
        msgList.setAdapter(adapter);

        if (config.getConfig().getShrinkThink()) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) thinkingToggle.getLayoutParams();
            params.bottomMargin = (int) (3 * getResources().getDisplayMetrics().density + 0.5f);
            thinkingToggle.setLayoutParams(params);
        }

        msgList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                //If the user touches the screen or flings the list
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING) {
                    autoScroll = false;
                }
            }
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
        final Context ctx = this;
        msgList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(((TextView) view.findViewById(R.id.message_text)).getText());
                Toast.makeText(ctx, R.string.text_copied, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            case R.id.about:
                Toast.makeText(this, "numAi " + BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ") ▶\ngithub.com/gohoski/numAi", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            processSelectedImage(data.getData());
        }
    }

    private void processSelectedImage(Uri uri) {
        try {
            Bitmap bitmap = decodeSampledBitmap(this, uri, 1080, 1080);
            if (bitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] bytes = baos.toByteArray();
                bitmap.recycle();

                inputImages.add("data:image/jpeg;base64," + Base64.encode(bytes));
                imgCount.setVisibility(View.VISIBLE);
                imgCount.setText(String.valueOf(inputImages.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        if (!autoScroll) return;
        msgList.post(new Runnable() {
            public void run() {
                msgList.post(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                        msgList.setSelection(adapter.getCount() - 1);
                    }
                });
            }
        });
    }

    private void stopGeneration() {
        isCancelled = true;

//        if (currentStream != null) {
//            try {
//                currentStream.close();
//            } catch (IOException e) {e.printStackTrace();}
//        }

        //Remove the messages from the data source
        List<Message> msgs = MessageManager.getInstance().getMessages();
        int size = msgs.size();
        if (size > 0 && msgs.get(size - 1).getRole().equals(Role.ASSISTANT.toString())) {
            msgs.remove(size - 1);
            size--;
        }
        if (size > 0 && msgs.get(size - 1).getRole().equals(Role.USER.toString())) {
            msgs.remove(size - 1);
        }
        resetUIState();

        // Force adapter update
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (text.length() == 0 && inputImages.isEmpty()) return;
        autoScroll = true;
        isCancelled = false;
        currentStream = null;
        MessageManager.getInstance().addMessage(new Message(Role.USER, text, new ArrayList<String>(inputImages), null));
        input.setText("");
        sendBtn.setImageResource(R.drawable.ic_action_stop);
        input.setEnabled(false);
        attachBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        inputImages.clear();
        imgCount.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        scrollToBottom();

        thinkBuffer.setLength(0);
        contentBuffer.setLength(0);
        isThinkingState = false;
        isGenerating = true;
        final boolean thinkingEnabled = thinkingToggle.isChecked();
        apiService.chatCompletion(MessageManager.getInstance().getMessages(), thinkingEnabled, new ApiCallback<ApiResult>() {
            @Override
            public void onSuccess(final ApiResult apiResult) {
                if (isCancelled)return;
                runOnUiThread(new Runnable() {
                    public void run() {
                        startResponseStream(apiResult.getResult(), apiResult.getModel(), thinkingEnabled);
                    }
                });
            }
            @Override
            public void onError(final ApiError error) {
                if (isCancelled)return;
                runOnUiThread(new Runnable() {
                    public void run() {
                        handleStreamError(error.getMessage());
                    }
                });
            }
        });
    }

    private void startResponseStream(final InputStream stream, String model, final boolean thinkingEnabled) {
        this.currentStream = stream;
        progressBar.setVisibility(View.GONE);
        final Message msg = new Message(Role.ASSISTANT, "", model);
        MessageManager.getInstance().addMessage(msg);
        adapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            public void run() {
                try {
                    readStream(stream, msg, thinkingEnabled);
                } catch (Exception e) {
                    if (isGenerating && !isCancelled) {
                        final String err = e.getMessage();
                        runOnUiThread(new Runnable() { public void run() { handleStreamError(err); } });
                    }
                }
            }
        }).start();
    }

    private void handleStreamError(String errorMsg) {
        progressBar.setVisibility(View.GONE);
        Message error = new Message(Role.ASSISTANT, errorMsg, getString(R.string.error)); // Assuming error string res exists
        error.setAsError();
        MessageManager.getInstance().addMessage(error);
        resetUIState();
    }

    private void resetUIState() {
        isGenerating = false;
        adapter.notifyDataSetChanged();
        autoScroll = true;
        scrollToBottom();
        sendBtn.setImageResource(android.R.drawable.ic_menu_send);
        input.setEnabled(true);
        attachBtn.setEnabled(true);
        progressBar.setVisibility(View.GONE);
    }

    private void readStream(InputStream inputStream, final Message msg, final boolean thinkingEnabled) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line;
        long lastUpdateTime = 0;
        while (!isCancelled && (line = reader.readLine()) != null) {
            if (!line.startsWith("data: ")) continue;
            String jsonData = line.substring(6).trim();
            if ("[DONE]".equals(jsonData)) break;
            try {
                JSONObject delta = JSON.getObject(jsonData).getArray("choices").getObject(0).getObject("delta");
                String contentStr = null;
                try {
                    contentStr = delta.getString("content");
                } catch(JSONException _) {}
                String reasoningStr = extractJSONReasoning(delta);
                boolean hasUpdates = false;
                if (thinkingEnabled) {
                    if (reasoningStr != null && reasoningStr.length() > 0) {
                        thinkBuffer.append(reasoningStr);
                        hasUpdates = true;
                    }
                    if (contentStr != null && contentStr.length() > 0) {
                        processContentWithTags(contentStr);
                        hasUpdates = true;
                    }
                } else {
                    if (reasoningStr != null) contentBuffer.append(reasoningStr);
                    if (contentStr != null) contentBuffer.append(contentStr);
                    hasUpdates = (reasoningStr != null || contentStr != null);
                }
                long currentTime = System.currentTimeMillis();
                if (hasUpdates && (currentTime - lastUpdateTime >= UPDATE_DELAY_MS)) {
                    lastUpdateTime = currentTime;
                    updateStreamUI(msg, thinkingEnabled, false);
                }
            } catch (Exception e) {
                Log.e("readStream", "error parsing chunk " + jsonData, e);
            }
        }
        if (isCancelled) return;
        runOnUiThread(new Runnable() {
            public void run() {
                updateStreamUI(msg, thinkingEnabled, true);
                resetUIState();
            }
        });
    }

    private void processContentWithTags(String token) {
        int cursor = 0;
        while (cursor < token.length()) {
            if (isThinkingState) {
                int endTag = token.indexOf("</think>", cursor);
                if (endTag != -1) {
                    thinkBuffer.append(token.substring(cursor, endTag));
                    isThinkingState = false;
                    cursor = endTag + 8; // </think>
                } else {
                    thinkBuffer.append(token.substring(cursor));
                    break;
                }
            } else {
                int startTag = token.indexOf("<think>", cursor);
                if (startTag != -1) {
                    contentBuffer.append(token.substring(cursor, startTag));
                    isThinkingState = true;
                    cursor = startTag + 7; // <think>
                } else {
                    contentBuffer.append(token.substring(cursor));
                    break;
                }
            }
        }
    }

    private void updateStreamUI(final Message msg, final boolean thinkingEnabled, final boolean isFinal) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    updateStreamUI(msg, thinkingEnabled, isFinal);
                }
            });
            return;
        }
        String displayContent = contentBuffer.toString();
        String displayThink = thinkBuffer.toString();
        msg.setContent(displayContent);
        int firstVis = msgList.getFirstVisiblePosition();
        int lastVis = msgList.getLastVisiblePosition();
        int count = adapter.getCount();
        int targetIndex = count - 1;
        if (targetIndex >= firstVis && targetIndex <= lastVis) {
            View view = msgList.getChildAt(targetIndex - firstVis);
            if (view != null) {
                TextView tvText = (TextView) view.findViewById(R.id.message_text);
                LinearLayout thinkLayout = (LinearLayout) view.findViewById(R.id.thinkingLayout);
                TextView tvThink = (TextView) view.findViewById(R.id.thinkingProcess);
                View vResponse = view.findViewById(R.id.response);
                if (tvText != null) tvText.setText(displayContent);
                if (thinkingEnabled) {
                    boolean hasThinkContent = displayThink.length() > 0;
                    if (hasThinkContent) {
                        thinkLayout.setVisibility(View.VISIBLE);
                        view.findViewById(R.id.noThinking).setVisibility(View.GONE);
                        tvThink.setText(displayThink);
                        vResponse.setVisibility((displayContent.length() > 0 || isFinal) ? View.VISIBLE : View.GONE);
                    } else {
                        thinkLayout.setVisibility(View.VISIBLE);
                        view.findViewById(R.id.noThinking).setVisibility(View.VISIBLE);
                        vResponse.setVisibility(View.GONE);
                    }

                    if (isFinal && !hasThinkContent && displayContent.length() > 0) {
                        thinkLayout.setVisibility(View.GONE);
                        vResponse.setVisibility(View.VISIBLE);
                    }
                } else {
                    vResponse.setVisibility(View.VISIBLE);
                    if (thinkLayout != null) thinkLayout.setVisibility(View.GONE);
                }
            }
        }

        if (autoScroll) scrollToBottom();
    }

    public static Bitmap decodeSampledBitmap(Context ctx, Uri uri, int reqW, int reqH) throws IOException {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, reqW, reqH);
        options.inJustDecodeBounds = false;

        is = ctx.getContentResolver().openInputStream(uri);
        Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
        is.close();
        return bmp;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqW, int reqH) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqH || width > reqW) {
            if (width > height) {
                while ((width / inSampleSize) > reqW) {
                    inSampleSize *= 2;
                }
            }else {
                while ((height / inSampleSize) > reqH) {
                    inSampleSize *= 2;
                }
            }
        }
        return inSampleSize;
    }

    private String extractJSONReasoning(JSONObject delta) {
        try {
            return delta.getString("reasoning");
        } catch (JSONException e) {
            try {
                return delta.getArray("reasoning_content").getObject(0).getString("thinking");
            } catch(JSONException _) {
                return null;
            }
        }
    }
}