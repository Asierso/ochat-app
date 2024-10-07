package com.asierso.ochat

import android.content.Context
import android.util.Log
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.models.ClientSettings
import com.asierso.ochat.models.Conversation
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class FilesManager {
    companion object {
        fun saveSettings(context: Context, clientSettings : ClientSettings){
            //Save config
            BufferedWriter(FileWriter("${context.filesDir}con_settings.json")).use{
                it.write(Gson().toJson(clientSettings))
            }
        }
        fun loadSettings(context: Context) : ClientSettings?{
            //Detects if previous config file exists and if not, create new one with default settings
            if(!File("${context.filesDir}con_settings.json").exists()){
                val defaultSettings = ClientSettings().apply {
                    isUseDescriptions = true
                }
                saveSettings(context,defaultSettings)
                return defaultSettings
            }

            //Loads config
            var clientSettings : ClientSettings
            BufferedReader(FileReader("${context.filesDir}con_settings.json")).use{
                clientSettings = Gson().fromJson(it.readText(),ClientSettings::class.java)
            }
            return clientSettings
        }

        fun saveConversation(context: Context, chatName: String, messages: Conversation){
            //Create chats dir if not exists
            if(!File("${context.filesDir}/chats").exists())
                File("${context.filesDir}/chats").mkdir()

            Log.d("deb","${context.filesDir}/chats/chat_${chatName.replace("-", "_").hashCode()}.json")

            BufferedWriter(FileWriter("${context.filesDir}/chats/chat_${chatName.replace("-","_").hashCode()}.json")).use{
                it.write(Gson().toJson(messages))
            }
        }

        fun loadConversation(context: Context, chatName : String ) : Conversation?{
            //Detects if previous config file exists
            if(!File("${context.filesDir}/chats/chat_${chatName.replace("-","_").hashCode()}.json").exists())
                return null

            //Loads config
            var messages: Conversation
            BufferedReader(FileReader("${context.filesDir}/chats/chat_${chatName.replace("-","_").hashCode()}.json")).use{
                messages = Gson().fromJson(it.readText(),Conversation::class.java)
            }
            return messages
        }

        fun removeAllChats(context: Context){
            if(File("${context.filesDir}/chats").exists())
                File("${context.filesDir}/chats").deleteRecursively()
        }

        fun chatExists(context: Context, chatName: String) : Boolean{
            return File("${context.filesDir}/chats/chat_${chatName.replace("-","_").hashCode()}.json").exists()
        }

    }
}