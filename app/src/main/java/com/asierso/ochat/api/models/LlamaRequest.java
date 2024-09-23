package com.asierso.ochat.api.models;

import com.asierso.ochat.api.handlers.LlamaRequestBase;

import java.util.ArrayList;

public class LlamaRequest implements LlamaRequestBase {
    private String model;
    private String prompt;
    private boolean stream;
    private ArrayList<LlamaMessage> messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public ArrayList<LlamaMessage> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<LlamaMessage> messages) {
        this.messages = messages;
    }

    public LlamaRequest(){

    }

    public LlamaRequest(LlamaRequestBase data) {
        this.model = data.getModel();
        this.stream = data.isStream();
    }
}
