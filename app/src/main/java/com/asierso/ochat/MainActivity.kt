package com.asierso.ochat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.*
import com.asierso.ochat.api.builder.LlamaDialogsBuilder
import com.asierso.ochat.api.builder.LlamaRequestBaseBuilder
import com.asierso.ochat.api.handlers.LlamaConnectionException
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.api.models.LlamaResponse
import com.asierso.ochat.components.MessageCardView
import com.asierso.ochat.models.ClientSettings
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    enum class Side { User, IA }

    private lateinit var conversation: ArrayList<LlamaMessage>

    private var settings: ClientSettings? = null
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        context = this

        //Rescale UI
        rescale()

        //Init messages stack
        conversation = arrayListOf()

        //Button logic
        findViewById<ImageView>(R.id.btn_send).setOnClickListener {
            val msg = findViewById<TextInputEditText>(R.id.tf_message)
            try {
                //Lock send button
                isSendEnabled(false)

                //Send message and update view
                conversation.add(LlamaMessage(LlamaMessage.USER_ROLE, msg.text.toString()))

                //Create message balloon only if user wrote some text
                if (msg.text.toString().isNotBlank()) {
                    val msgview = MessageCardView(context, Side.User)
                    msgview.getTextComponent().append(msg.text.toString())
                    msgview.stopLoading()
                    findViewById<LinearLayout>(R.id.message_layout).addView(msgview.getView())
                }

                //Close virtual keyboard
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(msg.windowToken,0)

                //Send message and clear text
                sendPrompt()
                msg.setText("")
            } catch (e: Exception) {
                Toast.makeText(this, "Error at connecting ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<ImageView>(R.id.btn_settings).setOnClickListener {
            val settings = Intent(context, SettingsActivity::class.java)
            startActivity(settings)
        }

        findViewById<TextInputEditText>(R.id.tf_message).doOnTextChanged { text, start, before, count ->
            var lines = findViewById<TextInputEditText>(R.id.tf_message).lineCount
            lines = if (lines > 10) 10 else lines

            findViewById<TextInputLayout>(R.id.tf_message_layout).layoutParams = TableRow.LayoutParams(
                resources.displayMetrics.widthPixels - 230,
                Global.getPixels(this,60) + ((lines-1) * Global.getPixels(this,19))
            )
        }

        settings = FilesManager.loadSettings(this)
        loadConversation()

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
                        for (balloonMessage in conversation)
                            withContext(Dispatchers.Main) {
                                //Create all message balloons
                                val msgview = MessageCardView(context,if (balloonMessage.role.equals("user")) Side.User else Side.IA)
                                msgview.getTextComponent().text = balloonMessage.content.toString()
                                findViewById<LinearLayout>(R.id.message_layout).addView(msgview.getView())
                                msgview.stopLoading()
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
        if(settings == null)
            return

        //Detects changes in settings and clear chats if is it or clear chat if is removed
        if (settings.hashCode() != prevTmp.hashCode() || !FilesManager.chatExists(this,"${settings!!.ip}_${settings!!.model}")) {
            findViewById<LinearLayout>(R.id.message_layout).removeAllViews()
            conversation.clear()

            //Try to charge if there is another saved conversation
            if (settings?.model != null && settings?.ip != null)
                loadConversation()
        }
    }

    private fun isSendEnabled(status: Boolean) {
        val sendBtn = findViewById<ImageView>(R.id.btn_send)
        sendBtn.isEnabled = status
        if (!status) {
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.spinner_shape
                )
            )
            sendBtn.animation =AnimationUtils.loadAnimation(context, R.anim.load_rotation_lineal)
        }
        else {
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.baseline_send_24
                )
            )
            sendBtn.animation = null
        }
    }

    private fun rescale() {
        val text = findViewById<TextInputLayout>(R.id.tf_message_layout)

        //Adjust text layout
        val layout = text.layoutParams
        layout.width = resources.displayMetrics.widthPixels - 230
        text.layoutParams = layout
    }

    private fun sendPrompt() {
        //Create message balloon
        val messageView = MessageCardView(context,Side.IA)
        findViewById<LinearLayout>(R.id.message_layout).addView(messageView.getView())

        //Make autoscroll
        findViewById<ScrollView>(R.id.scr_layout_message_screen)
            .smoothScrollTo(0,findViewById<LinearLayout>(R.id.message_layout).height)

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
                                findViewById<ScrollView>(R.id.scr_layout_message_screen)
                                    .smoothScrollTo(0,findViewById<LinearLayout>(R.id.message_layout).height)
                            }
                        }
                    }
                } catch (e: LlamaConnectionException) {
                    //Error at some point of connection
                    llamaResponse = null
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error ${e.message}", Toast.LENGTH_SHORT).show()

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