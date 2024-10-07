package com.asierso.ochat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.LinearLayout.GONE
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.get
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.*
import com.asierso.ochat.api.builder.LlamaDialogsBuilder
import com.asierso.ochat.api.builder.LlamaPromptsBuilder
import com.asierso.ochat.api.builder.LlamaRequestBaseBuilder
import com.asierso.ochat.api.handlers.LlamaConnectionException
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.api.models.LlamaResponse
import com.asierso.ochat.components.MessageCardView
import com.asierso.ochat.components.MessageEdit
import com.asierso.ochat.databinding.ActivityMainBinding
import com.asierso.ochat.models.ClientSettings
import com.asierso.ochat.models.Conversation
import com.asierso.ochat.utils.Global
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    enum class Side { User, IA }

    private lateinit var conversation: Conversation
    private var settings: ClientSettings? = null
    private lateinit var context: Context
    private lateinit var binding: ActivityMainBinding
    private lateinit var messageEdit: MessageEdit
    private var lastIAMessage : MessageCardView? = null
    private var flagNeedSummary = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this

        //Crete messages bar
        messageEdit = MessageEdit(context)
        binding.layoutMessagesBarComponents.addView(messageEdit.getMessageEditCard(), 0)

        //Init messages stack
        conversation = Conversation()

        //Button logic
        binding.btnSend.setOnClickListener {
            try {
                //Lock send button
                isSendEnabled(false)

                //Generate summary if is first conversation
                if (conversation.chat.size == 0 && messageEdit.getText().length > 10 && conversation.description.isBlank())
                    generateSummary(messageEdit.getText())
                else if (conversation.chat.size == 0 && conversation.description.isBlank())
                    flagNeedSummary = true

                //Send message and update view
                conversation.chat.add(
                    LlamaMessage(
                        LlamaMessage.USER_ROLE,
                        messageEdit.getText()
                    )
                )

                //Create message balloon only if user wrote some text
                if (messageEdit.getText().isNotBlank()) {
                    val messageView = MessageCardView(context, Side.User)
                    messageView.getTextComponent().append(messageEdit.getText())
                    messageView.stopLoading()
                    findViewById<LinearLayout>(R.id.message_layout).addView(messageView.getView())
                }

                //Close virtual keyboard
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(messageEdit.getMessageEditCard().windowToken, 0)

                //Send message and clear text
                sendPrompt()
                messageEdit.clear()
                scrollFinal()
            } catch (e: Exception) {
                Toast.makeText(this, "Error at connecting ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        //Open settings menu
        binding.btnSettings.setOnClickListener {
            val settings = Intent(context, SettingsActivity::class.java)
            startActivity(settings)
        }

        //Load settings and last conversation opened if can
        settings = FilesManager.loadSettings(this)
        loadConversation()

    }

    private fun scrollFinal() {
        binding.scrLayoutMessageScreen
            .smoothScrollTo(0, binding.messageLayout.height + binding.layoutMessagesBar.height)
    }

    private fun loadConversation() {
        if (settings != null && settings!!.model != null && settings!!.model.isNotBlank())
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {

                    //Load conversation using ip and model names
                    val loadedConversation = FilesManager.loadConversation(
                        context,
                        "${settings!!.ip}_${settings!!.model}"
                    )

                    //Check if loaded conversation is valid
                    if (loadedConversation != null) {
                        conversation.chat = loadedConversation.chat
                        conversation.description = loadedConversation.description

                        //Generate summary for the first message
                        generateSummary(conversation.chat[0].content.toString())

                        //Count for message (regeneration button purposes)
                        var i = 0

                        for (balloonMessage in conversation.chat)
                            withContext(Dispatchers.Main) {
                                //Create all message balloons
                                val messageView = MessageCardView(
                                    context,
                                    if (balloonMessage.role.equals("user")) Side.User else Side.IA
                                )

                                //Config message and add it to layout
                                messageView.getTextComponent().text = balloonMessage.content.toString()
                                findViewById<LinearLayout>(R.id.message_layout).addView(messageView.getView())
                                messageView.stopLoading()

                                //Load regeneration
                                if(i==conversation.chat.size -1) {
                                    messageView.setRegenerate(true)
                                    //Assign regeneration click to event
                                    messageView.getRegenerateButton().setOnClickListener {
                                        onRegenerate(messageView)
                                    }
                                    lastIAMessage = messageView
                                }
                                i++
                            }
                        //Make autoscroll in another coroutine when load ends
                        withContext(Dispatchers.Main) {
                            scrollFinal()
                        }
                    }else{
                        //Hide description gap if there's no chat
                        withContext(Dispatchers.Main) {
                            if (binding.layoutTopBarComponents.size == 2)
                                binding.layoutTopBarComponents[1].visibility = GONE
                        }
                    }
                }
            }
    }

    private fun onRegenerate(message: MessageCardView){
        binding.messageLayout.removeView(message.getView())
        conversation.chat.removeAt(conversation.chat.size - 1)
        sendPrompt()
        scrollFinal()
    }

    override fun onResume() {
        super.onResume()

        //Apply settings
        val prevTmp = settings
        settings = FilesManager.loadSettings(this)

        //Avoid null pointer exception
        if (settings == null)
            return

        //Detects critical changes in settings and clear chats if is it or clear chat if is removed
        if (settings.hashCode() != prevTmp.hashCode() || !FilesManager.chatExists(
                this,
                "${settings!!.ip}_${settings!!.model}"
            )
        ) {
            //Creates new chat
            binding.messageLayout.removeAllViews()
            conversation = Conversation()

            //Hide description textview (no chat loaded or new one)
            if (binding.layoutTopBarComponents.size >= 2)
                binding.layoutTopBarComponents[1].visibility = GONE

            //Try to charge if there is another saved conversation
            if (settings?.model != null && settings?.ip != null)
                loadConversation()
        }

        //Enable or disable descriptions
        if (binding.layoutTopBarComponents.size == 2)
            binding.layoutTopBarComponents[1].visibility =
                if (settings!!.isUseDescriptions) VISIBLE else GONE

        //Make autoscroll
        scrollFinal()
    }

    private fun isSendEnabled(status: Boolean) {
        val sendBtn = binding.btnSend
        sendBtn.isEnabled = status
        if (!status) {
            //Send button is loading (loading IA response)
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.spinner_shape
                )
            )
            sendBtn.animation = AnimationUtils.loadAnimation(context, R.anim.load_rotation_bouncing)
        } else {
            //Send button is prepared to click and send new prompt
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.baseline_send_24
                )
            )
            sendBtn.animation = null
        }
    }

    private fun generateSummary(data: String) {
        //Check if use descriptors is enabled
        if (settings == null)
            return
        if (!settings!!.isUseDescriptions)
            return

        val summary: TextView

        //Summary text view is added (recycle it)
        if (binding.layoutTopBarComponents.size == 2) {
            summary = binding.layoutTopBarComponents[1] as TextView
            summary.text = ""
        } else { //Create new summary text view
            summary = TextView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
                visibility = GONE
                textSize = Global.getPixels(context, 4).toFloat()
            }
            binding.layoutTopBarComponents.addView(summary)
        }

        //Load description if summary was generated in this chat before
        if (conversation.description.isNotBlank()) {
            summary.text = conversation.description
            summary.visibility = VISIBLE
            return
        }

        //Make fetch to get summary text
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                //Get llama api url
                val llama = LlamaConnection(Global.bakeUrl(settings) ?: "")

                //Build llama connection
                val llamaRequestBase = LlamaRequestBaseBuilder()
                    .useModel(settings?.model.toString())
                    .withStream(true)
                    .build()

                //Build summary ask
                val promptBuilder = LlamaPromptsBuilder(llamaRequestBase)
                    .appendPrompt("summary in three words in the same language '${data}'")

                try {
                    llama.fetchRealtime(
                        promptBuilder.build()
                    ) { e ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                //Update view. Make summary text visible and append text
                                summary.visibility = VISIBLE
                                if (summary.text.length > 20 && summary.text[summary.text.length - 1] != '.')
                                    summary.append("...")
                                else if (summary.text.length < 20)
                                    summary.append(e.response.trim('"', '\n', '\r'))

                                //Set description
                                conversation.description = summary.text.toString()
                            }
                        }
                    }

                    //Saved generated description
                    withContext(Dispatchers.IO){
                        saveConversation()
                    }
                } catch (ignore: LlamaConnectionException) {
                    //Connection error (no need to handle)
                }
            }
        }
    }

    private fun sendPrompt() {
        //Disable message regeneration of last message
        lastIAMessage?.setRegenerate(false)

        //Create message balloon
        val messageView = MessageCardView(context, Side.IA)
        binding.messageLayout.addView(messageView.getView())

        //Save message reference to clear regeneration next time
        lastIAMessage = messageView

        //Make autoscroll
        scrollFinal()

        //Check if settings are correct
        if (settings == null) {
            Toast.makeText(this, "Error, specify valid settings", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                //Get llama api url
                val llama = LlamaConnection(Global.bakeUrl(settings) ?: "")

                //Build llama connection
                val llamaRequestBase = LlamaRequestBaseBuilder()
                    .useModel(settings?.model.toString())
                    .withStream(true)
                    .build()

                //Build dialogs
                val dialogBuilder = LlamaDialogsBuilder(llamaRequestBase)
                for (handle in if(settings!!.isOptimizeModels) ChatOptimizer(conversation).getOptimizedChat() else conversation.chat)
                    dialogBuilder.createDialog(handle.role, handle.content)

                var llamaResponse: LlamaResponse?
                try {
                    llamaResponse = llama.fetchRealtime(
                        dialogBuilder.build()
                    ) { e ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                messageView.stopLoading()
                                messageView.getTextComponent().append(e.message.content.toString())

                                //Make autoscroll
                                scrollFinal()
                            }
                        }
                    }
                } catch (e: LlamaConnectionException) {
                    //Error at some point of connection
                    llamaResponse = null
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.d("Error", e.message + " " + e.stackTraceToString())

                        //Remove message
                        binding.messageLayout.removeView(messageView.getView())
                    }
                }
                return@withContext llamaResponse
            }
            if (res != null) {
                //Add full text after stream load
                conversation.chat.add(
                    LlamaMessage(
                        LlamaMessage.ASSISTANT_ROLE,
                        res.message.content
                    )
                )

                if (flagNeedSummary) {
                    generateSummary(conversation.chat[conversation.chat.size - 1].content.toString())
                    flagNeedSummary = false
                }



                //Save conversation
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        saveConversation()
                    }
                }
            }

            //Allow user to send new messages
            isSendEnabled(true)
            messageView.setRegenerate(true)
            messageView.getRegenerateButton().setOnClickListener {
                onRegenerate(messageView)
            }
        }
    }

    private fun saveConversation() {
        FilesManager.saveConversation(
            context,
            "${settings!!.ip}_${settings!!.model}",
            conversation
        )
    }
}