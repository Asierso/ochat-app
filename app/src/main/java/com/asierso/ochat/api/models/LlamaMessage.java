package com.asierso.ochat.api.models;

public class LlamaMessage {
    public static final String USER_ROLE = "user";
    public static final String ASSISTANT_ROLE = "assistant";
    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LlamaMessage(String role,String content) {
        this.role = role;
        this.content = content;
    }

    public LlamaMessage(){

    }
}
