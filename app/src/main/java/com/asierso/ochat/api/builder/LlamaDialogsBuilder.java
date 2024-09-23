package com.asierso.ochat.api.builder;

import com.asierso.ochat.api.models.LlamaMessage;
import com.asierso.ochat.api.models.LlamaRequest;
import com.asierso.ochat.api.handlers.LlamaRequestBase;

import java.util.ArrayList;

public class LlamaDialogsBuilder {
    private final LlamaRequest req;
    public LlamaDialogsBuilder(LlamaRequestBase data){
        this.req = new LlamaRequest(data);
        req.setMessages(new ArrayList<>());
    }

    public LlamaDialogsBuilder createDialog(String role, String content){
        req.getMessages().add(new LlamaMessage(role,content));
        return this;
    }

    public LlamaRequest build() {
        return req;
    }
}
