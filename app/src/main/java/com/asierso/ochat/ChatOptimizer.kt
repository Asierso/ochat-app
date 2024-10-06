package com.asierso.ochat

import android.util.Log
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.models.Conversation

class ChatOptimizer (var conversation: Conversation) {
    //Constants for chat optimization
    private val startOptimizerChat = 3
    private val maxChars = 1000
    private var optimizedchat = arrayListOf<LlamaMessage>()
    init{
        if(conversation.chat.size <= startOptimizerChat)
            optimizedchat = conversation.chat
        else {
            optimizedchat = optimizeChat(startOptimizerChat)
        }
    }

    private fun optimizeChat(maxChat: Int) : ArrayList<LlamaMessage> {
        var localoptimizedchat = arrayListOf<LlamaMessage>()
        var charCount = 0
        for (i in conversation.chat.size - maxChat until conversation.chat.size) {
            charCount+=conversation.chat[i].content.length
            localoptimizedchat.add(conversation.chat[i])
        }

        if(charCount <= maxChars * .5 && conversation.chat.size > maxChat + 1)
            localoptimizedchat = optimizeChat(maxChat+1)

        //Detects if optimization is needed and optimize chat
        while(charCount > maxChars && localoptimizedchat.size >= 3)
            charCount-=localoptimizedchat.removeAt(0).content.length

        return localoptimizedchat
    }

    fun getOptimizedChat() : ArrayList<LlamaMessage> {
        Log.d("dep",optimizedchat.size.toString())
        return optimizedchat
    }
}