package com.asierso.ochat.api.builder;

import com.asierso.ochat.api.handlers.LlamaRequestBase;
import com.asierso.ochat.api.models.LlamaRequest;

public class LlamaPromptsBuilder {
    private final LlamaRequest req;
    private final StringBuilder prompt;
    public LlamaPromptsBuilder(LlamaRequestBase data){
        this.req = new LlamaRequest(data);
        this.prompt = new StringBuilder();
    }

    public LlamaPromptsBuilder appendPrompt(String content){
        prompt.append(content).append("\n");
        return this;
    }

    public LlamaRequest build() {
        req.setPrompt(prompt.toString());
        return req;
    }
}
