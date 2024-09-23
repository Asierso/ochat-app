package com.asierso.ochat.api.handlers;

import com.asierso.ochat.api.models.LlamaResponse;

public interface RealtimeResponseCallback {
    void run(LlamaResponse lineResponse);
}
