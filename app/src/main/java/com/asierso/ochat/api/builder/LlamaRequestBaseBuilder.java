package com.asierso.ochat.api.builder;

import com.asierso.ochat.api.handlers.LlamaRequestBase;
import com.asierso.ochat.api.models.LlamaRequest;

public class LlamaRequestBaseBuilder {
    private final LlamaRequestBase req;

    public LlamaRequestBaseBuilder() {
        req = new LlamaRequest();
        req.setStream(true);
    }

    public LlamaRequestBaseBuilder useModel(String model){
        req.setModel(model);
        return this;
    }

    public LlamaRequestBaseBuilder withStream(boolean stream){
        req.setStream(stream);
        return this;
    }

    public LlamaRequestBase build(){
        return req;
    }
}
