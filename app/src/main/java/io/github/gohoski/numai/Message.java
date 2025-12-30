package io.github.gohoski.numai;

import java.util.List;

/**
 * Created by Gleb on 21.08.2025.
 */

class Message {
    private Role role = Role.USER;
    private String content = "";
    private String llm;
    List<String> inputImages;
    private boolean isError = false;

    Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }
    Message(Role role, String content, String llm) {
        this.role = role;
        this.content = content;
        this.llm = llm;
    }
    Message(Role role, String content, List<String> inputImages, String llm) {
        this.role = role;
        this.content = content;
        this.llm = llm;
        this.inputImages = inputImages;
    }

    String getRole() {
        return role.toString();
    }

    String getContent() {
        return content;
    }

    String getLlm() {
        return llm;
    }

    List<String> getInputImages() {
        return inputImages;
    }

    // update content
    public void updateContent(String additionalContent) {
        this.content += additionalContent;
    }

    //set content completely
    void setContent(String newContent) {
        this.content = newContent;
    }

    boolean isSent() {
        return role == Role.USER;
    }

    void setAsError() {
        isError=true;
    }
}