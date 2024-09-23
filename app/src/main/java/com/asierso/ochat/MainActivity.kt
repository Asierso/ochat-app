package com.asierso.ochat

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.LayoutDirection
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.*
import com.asierso.ochat.api.builder.LlamaDialogsBuilder
import com.asierso.ochat.api.builder.LlamaPromptsBuilder
import com.asierso.ochat.api.builder.LlamaRequestBaseBuilder
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.api.models.LlamaRequest
import com.asierso.ochat.api.models.LlamaResponse
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    enum class Side {User, IA}
    private lateinit var conversation : ArrayList<LlamaMessage>

    private var url : String = "http://192.168.1.72:11434"
    private var model : String = "llama3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Rescale UI
        rescale()

        //Init messages stack
        conversation = arrayListOf()

        //Button logic
        findViewById<ImageView>(R.id.btn_send).setOnClickListener {
            val msg = findViewById<TextInputEditText>(R.id.tf_message)
            try {
                changeSendAble(false)
                conversation.add(LlamaMessage(LlamaMessage.USER_ROLE,msg.text.toString()))
                renderMessageView(Side.User).append(msg.text.toString())
                sendPrompt(msg.text.toString())
                msg.setText("")
            } catch (e: Exception) {
                Toast.makeText(this, "Error at connecting " + e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        //URL Change
        findViewById<Button>(R.id.btn_url_config).setOnClickListener{
            url = findViewById<EditText>(R.id.tf_url_config).text.toString().trim()
            model = findViewById<EditText>(R.id.tf_model_config).text.toString().trim()
            conversation = arrayListOf()
            findViewById<LinearLayout>(R.id.message_layout).removeAllViewsInLayout()
            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
        }
    }

    private fun changeSendAble(status : Boolean){
        findViewById<ImageView>(R.id.btn_send).isEnabled = status
    }

    private fun rescale() {
        val text = findViewById<TextInputLayout>(R.id.tf_message_layout)

        //Adjust text layout
        var layout = text.layoutParams
        layout.width = resources.displayMetrics.widthPixels - 230
        text.layoutParams = layout
    }

    private fun renderMessageView(side : Side) : TextView{
        val ctx : Context = this

        //Create card
        val card = MaterialCardView(this).apply{
            radius = 27f
            elevation = 10f
            minimumHeight = Global.getPixels(ctx,70)
            setContentPadding(Global.getPixels(ctx,10),Global.getPixels(ctx,10),Global.getPixels(ctx,10),Global.getPixels(ctx,10))
            setCardBackgroundColor(getColor(if(side == Side.User) R.color.user_wrote else R.color.ia_wrote))
            layoutParams = LinearLayout.LayoutParams(
                Global.getPixels(ctx,200),
                LayoutParams.WRAP_CONTENT
            ).apply{
                setMargins(0, Global.getPixels(ctx,10),0,Global.getPixels(ctx,10 ))
                gravity = if(side == Side.User) Gravity.END else Gravity.START
            }
        }

        //Create text
        val txt = TextView(this).apply {
            setText("")
            textSize = 16f
            setTextColor(getColor(if(side == Side.User) R.color.user_wrote_fore else R.color.ia_wrote_fore))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        card.addView(txt)
        findViewById<LinearLayout>(R.id.message_layout).addView(card)
        return txt
    }

    private fun sendPrompt(msg : String) {
        val updateView : TextView = renderMessageView(Side.IA)

        lifecycleScope.launch {
            val res = withContext(Dispatchers.IO) {
                val llama = LlamaConnection(url)

                val llbb = LlamaRequestBaseBuilder()
                    .useModel(model)
                    .withStream(true)
                    .build()

                /*
                llama.fetchRealtime(
                    LlamaPromptsBuilder(llbb).appendPrompt(msg).build()
                ) { e ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            updateView.append(e.response.toString())
                        }
                    }
                }*/

                val dialogbuilder = LlamaDialogsBuilder(llbb)
                for(handle in conversation)
                    dialogbuilder.createDialog(handle.role,handle.content)

                /*
                llama.fetchRealtime(
                    LlamaPromptsBuilder(llbb).appendPrompt(msg).build()
                ) { e ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            updateView.append(e.response.toString())
                        }
                    }
                }*/

                llama.fetchRealtime(
                    dialogbuilder.build()
                ) { e ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            updateView.append(e.message.content.toString())
                        }
                    }
                }
            }
            conversation.add(LlamaMessage(LlamaMessage.ASSISTANT_ROLE,res.message.content))
            changeSendAble(true)
        }
    }
}