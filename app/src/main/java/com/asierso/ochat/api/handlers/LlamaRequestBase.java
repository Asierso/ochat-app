package com.asierso.ochat.api.handlers;

public interface LlamaRequestBase {
    String getModel();
    void setModel(String model);
    boolean isStream();
    void setStream(boolean stream);
}
