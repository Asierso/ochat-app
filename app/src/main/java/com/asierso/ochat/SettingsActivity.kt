package com.asierso.ochat

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.LlamaConnection
import com.asierso.ochat.models.ClientSettings
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    lateinit var context : Context
    lateinit var settings : ClientSettings
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        context = this

        findViewById<ImageView>(R.id.btn_back).setOnClickListener{
            finish()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener{
            saveConfig()
        }

        findViewById<Button>(R.id.btn_delete_all).setOnClickListener{
            FilesManager.removeAllChats(context)
        }

        loadConfig()

        findViewById<TextInputEditText>(R.id.lbl_llamaip).setOnFocusChangeListener() { view,_ ->
            settings.ip = findViewById<TextInputEditText>(R.id.lbl_llamaip).text.toString()
            fetchModels(settings)
        }

        findViewById<TextInputEditText>(R.id.lbl_llamaport).setOnFocusChangeListener { _,_ ->
            settings.port = (findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString().trim()).toInt()
            fetchModels(settings)
        }
    }

    private fun fetchModels(settings: ClientSettings){
        val url = Global.bakeUrl(settings)
        if(url==null) return

        lifecycleScope.launch {
            withContext(Dispatchers.IO){
                var models : ArrayList<String>? = null
                try {
                    models = LlamaConnection(url).avaiableModelsArrayList
                    withContext(Dispatchers.Main){
                        //Adjust dropdown
                        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, models!!)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        findViewById<Spinner>(R.id.spinner_llamamodel).adapter = adapter

                        if(adapter.getPosition(settings.model) >= 0)
                            findViewById<Spinner>(R.id.spinner_llamamodel).setSelection(adapter.getPosition(settings.model))
                    }
                } catch(ignore : Exception){
                }
            }

        }
    }

    private fun loadConfig(){
        settings = FilesManager.loadSettings(context) ?: return

        //Llama model
        fetchModels(settings)

        //Connection IP port
        findViewById<TextInputEditText>(R.id.lbl_llamaip).setText(settings.ip)
        findViewById<TextInputEditText>(R.id.lbl_llamaport).setText(settings.port.toString())

        //Protocol
        findViewById<RadioButton>(R.id.radio_https).isChecked = (settings.isSsl)
        findViewById<RadioButton>(R.id.radio_http).isChecked = (!settings.isSsl)
    }

    private fun saveConfig(){
        //Validate data
        if((findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString()).toInt() !in 0..65535){
            Toast.makeText(context,"Error, port should be between 0 and 65535",Toast.LENGTH_SHORT).show()
            return
        }

        //Save settings
        val settings = ClientSettings().apply {
            ip = findViewById<TextInputEditText>(R.id.lbl_llamaip).text.toString().trim()
            port = (findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString().trim()).toInt()
            model = findViewById<Spinner>(R.id.spinner_llamamodel).selectedItem.toString()
            isSsl = findViewById<RadioButton>(R.id.radio_https).isChecked
        }

        FilesManager.saveSettings(context,settings)
        finish()
    }
}