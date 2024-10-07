package com.asierso.ochat

import android.util.Log
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.models.Conversation

class ChatOptimizer (private var conversation: Conversation) {
    //Constants for chat optimization (min chats and max characters threshold)
    private val startOptimizerChat = 3
    private val maxChars = 1000

    //Final optimized chat
    private var optimizedchat = arrayListOf<LlamaMessage>()


    init{
        //No optimize chat if less big than optimizer start threshold
        optimizedchat = if(conversation.chat.size <= startOptimizerChat)
            conversation.chat
        else {
            optimizeChat(startOptimizerChat) //Start optimize chat of base threshold
        }
    }

    private fun optimizeChat(maxChat: Int) : ArrayList<LlamaMessage> {
        var localoptimizedchat = arrayListOf<LlamaMessage>()
        var charCount = 0

        //Add message from threshold (maxChat) to end
        for (i in conversation.chat.size - maxChat until conversation.chat.size) {
            charCount+=conversation.chat[i].content.length
            localoptimizedchat.add(conversation.chat[i])
        }

        //Detect if context might be more (chats with small char count)
        if(charCount <= maxChars * .5 && conversation.chat.size > maxChat + 1)
            localoptimizedchat = optimizeChat(maxChat+1)

        //Detects if more optimization is needed and reduce chat
        while(charCount > maxChars && localoptimizedchat.size >= 3)
            charCount-=localoptimizedchat.removeAt(0).content.length

        return localoptimizedchat
    }

    fun getOptimizedChat() : ArrayList<LlamaMessage> {
        Log.d("dep",optimizedchat.size.toString())
        return optimizedchat
    }
}