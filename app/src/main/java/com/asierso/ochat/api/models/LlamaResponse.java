package com.asierso.ochat.api.models;

public class LlamaResponse {
    private String model;
    private String response;
    private LlamaMessage message;
    private boolean done;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public LlamaMessage getMessage() {
        return message;
    }

    public void setMessage(LlamaMessage message) {
        this.message = message;
    }

    public LlamaResponse(){

    }

    public LlamaResponse(String model, String response, boolean done, LlamaMessage message) {
        this.model = model;
        this.response = response;
        this.done = done;
        this.message = message;
    }
}
