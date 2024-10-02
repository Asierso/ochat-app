package com.asierso.ochat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    enum class Side { User, IA }

    private lateinit var conversation: ArrayList<LlamaMessage>

    private var settings: ClientSettings? = null
    private lateinit var context: Context
    private lateinit var binding: ActivityMainBinding
    private lateinit var messageEdit: MessageEdit
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
        conversation = arrayListOf()

        //Button logic
        binding.btnSend.setOnClickListener {
            try {
                //Lock send button
                isSendEnabled(false)

                //Generate summary if is first conversation
                if (conversation.size == 0 && messageEdit.getText().length > 10)
                    generateSummary(messageEdit.getText())
                else if (conversation.size == 0)
                    flagNeedSummary = true

                //Send message and update view
                conversation.add(
                    LlamaMessage(
                        LlamaMessage.USER_ROLE,
                        messageEdit.getText()
                    )
                )

                //Create message balloon only if user wrote some text
                if (messageEdit.getText().isNotBlank()) {
                    val msgview = MessageCardView(context, Side.User)
                    msgview.getTextComponent().append(messageEdit.getText())
                    msgview.stopLoading()
                    findViewById<LinearLayout>(R.id.message_layout).addView(msgview.getView())
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

        binding.btnSettings.setOnClickListener {
            val settings = Intent(context, SettingsActivity::class.java)
            startActivity(settings)
        }

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
                        conversation.addAll(loadedConversation)
                        //Generate summary for the first message

                        generateSummary(conversation[0].content.toString())

                        for (balloonMessage in conversation)
                            withContext(Dispatchers.Main) {
                                //Create all message balloons
                                val msgview = MessageCardView(
                                    context,
                                    if (balloonMessage.role.equals("user")) Side.User else Side.IA
                                )
                                msgview.getTextComponent().text = balloonMessage.content.toString()
                                findViewById<LinearLayout>(R.id.message_layout).addView(msgview.getView())
                                msgview.stopLoading()
                            }
                        //Make autoscroll in another coroutine when load ends
                        withContext(Dispatchers.Main) {
                            scrollFinal()
                        }
                    }
                }
            }
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
            binding.messageLayout.removeAllViews()
            conversation.clear()

            if (binding.layoutTopBarComponents.size == 2)
                binding.layoutTopBarComponents.get(1).visibility = GONE

            //Try to charge if there is another saved conversation
            if (settings?.model != null && settings?.ip != null)
                loadConversation()
        }

        //Make autoscroll
        scrollFinal()
    }

    private fun isSendEnabled(status: Boolean) {
        val sendBtn = binding.btnSend
        sendBtn.isEnabled = status
        if (!status) {
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.spinner_shape
                )
            )
            sendBtn.animation = AnimationUtils.loadAnimation(context, R.anim.load_rotation_bouncing)
        } else {
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
        var summary: TextView

        if (binding.layoutTopBarComponents.size == 2) {
            summary = binding.layoutTopBarComponents.get(1) as TextView
            summary.text = ""
        }
        else {
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
                var promptBuilder = LlamaPromptsBuilder(llamaRequestBase)
                    .appendPrompt("summary in three words in the same language '${data}'")

                try {
                    llama.fetchRealtime(
                        promptBuilder.build()
                    ) { e ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                summary.visibility = VISIBLE
                                if(summary.text.length > 20 && summary.text[summary.text.length - 1] != '.')
                                    summary.append("...")
                                else if(summary.text.length < 20)
                                    summary.append(e.response.trim('"', '\n', '\r'))
                            }
                        }
                    }
                } catch (ignore: LlamaConnectionException) {

                }
            }
        }
    }

    private fun sendPrompt() {
        //Create message balloon
        val messageView = MessageCardView(context, Side.IA)
        binding.messageLayout.addView(messageView.getView())

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
                for (handle in conversation)
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
                        val hotCardView = (messageView.getView() as ViewGroup)
                        (hotCardView.parent as ViewGroup).removeView(hotCardView)
                        hotCardView.removeView(messageView.getView())
                    }
                }
                return@withContext llamaResponse
            }
            if (res != null) {
                //Add full text after stream load
                conversation.add(LlamaMessage(LlamaMessage.ASSISTANT_ROLE, res.message.content))

                if (flagNeedSummary) {
                    generateSummary(conversation[conversation.size - 1].content.toString())
                    flagNeedSummary = false
                }

                //Save conversation
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        FilesManager.saveConversation(
                            context,
                            "${settings!!.ip}_${settings!!.model}",
                            conversation.toTypedArray()
                        )
                    }
                }
            }

            //Allow user to send new messages
            isSendEnabled(true)
        }
    }
}