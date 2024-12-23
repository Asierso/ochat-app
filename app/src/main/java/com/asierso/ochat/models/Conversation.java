package com.asierso.ochat.models;

import androidx.annotation.NonNull;

import com.asierso.ochat.api.models.LlamaMessage;

import java.util.ArrayList;

public class Conversation implements Cloneable {
    private String description;
    private ArrayList<LlamaMessage> chat;

    public Conversation(){
        this.description = "";
        this.chat = new ArrayList<>();
    }

    public Conversation(String description, ArrayList<LlamaMessage> chat) {
        this.description = description;
        this.chat = chat;
    }

    public void setChat(ArrayList<LlamaMessage> chat) {
        this.chat = chat;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<LlamaMessage> getChat() {
        return chat;
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
