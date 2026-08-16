package io.github.gohoski.numai.data;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import io.github.gohoski.numai.model.Chat;
import io.github.gohoski.numai.model.Message;

public class ChatManager {
    private static final String INDEX_FILE = "chats.json";

    private static ChatManager instance;
    private final List<Chat> chats;
    private Chat currentChat;

    private ChatManager() {
        chats = new ArrayList<Chat>();
        startNewChat();
    }

    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public Chat getCurrentChat() {
        if (currentChat == null) {
            startNewChat();
        }
        return currentChat;
    }

    public void startNewChat() {
        currentChat = new Chat(UUID.randomUUID().toString(), "", System.currentTimeMillis());
    }

    public void setCurrentChat(Context context, Chat chat) {
        this.currentChat = chat;
        if (chat != null && chat.getId() != null && chat.getMessages().size() == 0) {
            loadMessages(context, chat);
        }
    }

    public List<Chat> getSortedChats() {
        List<Chat> sorted = new ArrayList<Chat>(chats);
        Collections.sort(sorted, new Comparator<Chat>() {
            @Override
            public int compare(Chat c1, Chat c2) {
                if (c2.getUpdatedAt() > c1.getUpdatedAt()) return 1;
                if (c2.getUpdatedAt() < c1.getUpdatedAt()) return -1;
                return 0;
            }
        });
        return sorted;
    }

    public void onMessageAdded(Context context) {
        if (currentChat == null) return;

        if (currentChat.getTitle() == null || currentChat.getTitle().length() == 0) {
            for (Message msg : currentChat.getMessages()) {
                if (msg.isSent() && msg.getContent() != null && msg.getContent().trim().length() > 0) {
                    currentChat.setTitle(generateTitle(msg.getContent()));
                    break;
                }
            }
        }

        if (!chats.contains(currentChat) && currentChat.getMessages().size() > 0) {
            chats.add(currentChat);
        }

        currentChat.setUpdatedAt(System.currentTimeMillis());
        saveChat(context, currentChat);
        saveChats(context);
    }

    public void deleteChat(Context context, Chat chat) {
        chats.remove(chat);
        if (currentChat == chat) {
            startNewChat();
        }

        // Delete associated image files from internal storage
        if (chat.getMessages() != null) {
            for (Message msg : chat.getMessages()) {
                List<String> images = msg.getInputImages();
                if (images != null) {
                    for (String fileName : images) {
                        if (!fileName.startsWith("data:image")) {
                            context.deleteFile(fileName);
                        }
                    }
                }
                String generatedImage = msg.getOutputImage();
                if (generatedImage != null && !generatedImage.startsWith("data:image")) {
                    context.deleteFile(generatedImage);
                }
            }
        }

        if (chat.getId() != null) {
            context.deleteFile(chatFileName(chat));
        }
        saveChats(context);
    }

    public void cleanupOrphanedImages(Context context) {
        final Context ctx = context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.util.Set<String> referencedFiles = new java.util.HashSet<String>();

                // Gather all image filenames across all chats
                for (Chat chat : chats) {
                    for (Message msg : chat.getMessages()) {
                        if (msg.getInputImages() != null) {
                            for (String img : msg.getInputImages()) {
                                if (!img.startsWith("data:image")) {
                                    referencedFiles.add(img);
                                }
                            }
                        }
                        String generatedImage = msg.getOutputImage();
                        if (generatedImage != null && !generatedImage.startsWith("data:image")) {
                            referencedFiles.add(generatedImage);
                        }
                    }
                }

                // List all files in internal directory and delete unreferenced images
                String[] files = ctx.fileList();
                if (files != null) {
                    for (String file : files) {
                        if ((file.startsWith("img_") || file.startsWith("gemini_img_")) &&
                                (file.endsWith(".jpg") || file.endsWith(".png"))) {
                            if (!referencedFiles.contains(file)) {
                                ctx.deleteFile(file);
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void saveChat(Context context, Chat chat) {
        if (chat == null || chat.getId() == null) return;
        String finalFileName = chatFileName(chat);
        String tempFileName = finalFileName + ".tmp";
        FileOutputStream fos;
        try {
            // Write to temporary file
            fos = context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
            fos.write(chat.toJSONObject().build().getBytes("UTF-8"));
            fos.flush();
            fos.close();
            // Safely replace original file only if write was 100% successful
            java.io.File tempFile = context.getFileStreamPath(tempFileName);
            java.io.File finalFile = context.getFileStreamPath(finalFileName);
            if (tempFile.exists()) {
                tempFile.renameTo(finalFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Delete temp file if write failed due to full disk
            context.deleteFile(tempFileName);
        }
    }

    public void saveChats(Context context) {
        try {
            JSONArray array = new JSONArray();
            for (Chat c : chats) {
                array.add(c.toIndexJSONObject());
            }
            FileOutputStream fos = context.openFileOutput(INDEX_FILE, Context.MODE_PRIVATE);
            fos.write(array.build().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadChats(Context context) {
        chats.clear();
        try {
            FileInputStream fis = context.openFileInput(INDEX_FILE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            fis.close();
            String jsonStr = baos.toString("UTF-8");
            if (jsonStr != null && jsonStr.trim().length() > 0) {
                JSONArray array = JSON.getArray(jsonStr);
                boolean migrated = false;
                for (int i = 0; i < array.size(); i++) {
                    JSONObject obj = array.getObject(i);
                    Chat chat = Chat.fromIndexJSONObject(obj);
                    JSONArray msgArr = obj.getNullableArray("messages");
                    if (msgArr != null && msgArr.size() > 0) {
                        chat = Chat.fromJSONObject(obj);
                        saveChat(context, chat);
                        migrated = true;
                    }
                    chats.add(chat);
                }
                if (migrated) {
                    saveChats(context);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void loadMessages(Context context, Chat chat) {
        try {
            FileInputStream fis = context.openFileInput(chatFileName(chat));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            fis.close();
            String jsonStr = baos.toString("UTF-8");
            if (jsonStr != null && jsonStr.trim().length() > 0) {
                Chat loaded = Chat.fromJSONObject(JSON.getObject(jsonStr));
                chat.getMessages().clear();
                chat.getMessages().addAll(loaded.getMessages());
            }
        } catch (Exception ignored) {
        }
    }

    private static String chatFileName(Chat chat) {
        return "chat_" + chat.getId() + ".json";
    }

    private static String generateTitle(String firstUserMessage) {
        if (firstUserMessage == null) return "New Chat";
        String clean = firstUserMessage.trim().replace('\n', ' ').replace('\r', ' ');
        if (clean.length() <= 35) {
            return clean;
        }
        return clean.substring(0, 35) + "\u2026";
    }
}
