package com.asierso.ochat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.*
import com.asierso.ochat.api.builder.LlamaDialogsBuilder
import com.asierso.ochat.api.builder.LlamaRequestBaseBuilder
import com.asierso.ochat.api.handlers.LlamaConnectionException
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.api.models.LlamaResponse
import com.asierso.ochat.models.ClientSettings
import com.google.android.material.card.MaterialCardView
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
                changeSendAble(false)


                //Send message and update view
                conversation.add(LlamaMessage(LlamaMessage.USER_ROLE, msg.text.toString()))
                if (msg.text.toString().isNotBlank())
                    renderMessageView(Side.User).append(msg.text.toString())

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

        settings = FilesManager.loadSettings(this)
        loadConversation()

    }

    private fun loadConversation() {
        if (settings != null && settings!!.model != null && settings!!.model.isNotBlank())
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val loadedConversation = FilesManager.loadConversation(
                        context,
                        "${settings!!.ip}_${settings!!.model}"
                    )
                    if (loadedConversation != null) {
                        conversation.addAll(loadedConversation)
                        for (balloonMessage in conversation)
                            withContext(Dispatchers.Main) {
                                renderMessageView(if (balloonMessage.role.equals("user")) Side.User else Side.IA)
                                    .text = balloonMessage.content.toString()
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

    private fun changeSendAble(status: Boolean) {
        val sendBtn = findViewById<ImageView>(R.id.btn_send)
        sendBtn.isEnabled = status
        if (!status)
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.spinner_rotate
                )
            )
        else
            sendBtn.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.baseline_send_24
                )
            )
    }

    private fun rescale() {
        val text = findViewById<TextInputLayout>(R.id.tf_message_layout)

        //Adjust text layout
        val layout = text.layoutParams
        layout.width = resources.displayMetrics.widthPixels - 230
        text.layoutParams = layout
    }

    private fun renderMessageView(side: Side): TextView {
        //Create card
        val card = MaterialCardView(this).apply {
            elevation = 20f

            //Set card resolution
            minimumHeight = Global.getPixels(context, 70)
            setContentPadding(
                Global.getPixels(context, 10),
                Global.getPixels(context, 10),
                Global.getPixels(context, 10),
                Global.getPixels(context, 10)
            )

            //Set card design
            if (side == Side.User)
                setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.user_message_balloon
                    )
                )
            else
                setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.ia_message_balloon
                    )
                )

            //Adjust layout
            layoutParams = LinearLayout.LayoutParams(
                Global.getPixels(context, 250),
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, Global.getPixels(context, 10), 0, Global.getPixels(context, 10))
                gravity = if (side == Side.User) Gravity.END else Gravity.START
            }
        }

        //Create text
        val txt = TextView(this).apply {
            text = ""
            textSize = 16f
            setTextColor(getColor(if (side == Side.User) R.color.user_wrote_fore else R.color.ia_wrote_fore))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        card.addView(txt)
        findViewById<LinearLayout>(R.id.message_layout).addView(card)
        return txt
    }

    private fun sendPrompt() {
        val updateView: TextView = renderMessageView(Side.IA)
        if (settings == null) {
            Toast.makeText(this, "Error, specify valid settings", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                val llama = LlamaConnection(Global.bakeUrl(settings) ?: "")

                val llamaRequestBase = LlamaRequestBaseBuilder()
                    .useModel(settings?.model.toString())
                    .withStream(true)
                    .build()

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
                                updateView.append(e.message.content.toString())
                                val scroll =
                                    findViewById<ScrollView>(R.id.scr_layout_message_screen)
                                scroll.scrollTo(
                                    0,
                                    scroll.bottom + (resources.displayMetrics.densityDpi * 10)
                                )
                            }
                        }
                    }
                } catch (e: LlamaConnectionException) {
                    llamaResponse = null
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error ${e.message}", Toast.LENGTH_SHORT).show()

                        val hotCardView = (updateView.parent as ViewGroup)
                        (hotCardView.parent as ViewGroup).removeView(hotCardView)
                        hotCardView.removeView(updateView)

                    }
                }
                return@withContext llamaResponse
            }
            if (res != null) {
                conversation.add(LlamaMessage(LlamaMessage.ASSISTANT_ROLE, res.message.content))
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
            changeSendAble(true)
        }
    }
}