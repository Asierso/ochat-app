package com.asierso.ochat.api.handlers;

public class LlamaConnectionException extends Exception{
    public LlamaConnectionException(int status,String text){
        super(status + ": " + text);
    }
}
