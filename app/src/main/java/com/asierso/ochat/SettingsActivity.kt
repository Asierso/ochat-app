package com.asierso.ochat

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
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
    private lateinit var context: Context
    private var settings: ClientSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        context = this

        //Allow all downloads
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveConfig()
        }

        findViewById<Button>(R.id.btn_delete_all).setOnClickListener {
            FilesManager.removeAllChats(context)
        }

        loadConfig()

        findViewById<TextInputEditText>(R.id.lbl_llamaip).setOnFocusChangeListener { _, _ ->
            tryGetModels()
        }

        findViewById<TextInputEditText>(R.id.lbl_llamaport).setOnFocusChangeListener { _, _ ->
            tryGetModels()
        }
    }

    private fun tryGetModels() {
        //Get IP and port from view
        val ip = findViewById<TextInputEditText>(R.id.lbl_llamaip).text.toString()
        val port = findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString().trim()

        //Try to fetch models if data is valid
        if (ip.isNotBlank() && port.isNotBlank()) {
            if (settings == null)
                settings = ClientSettings()

            //Update settings with view data
            settings!!.ip = ip
            settings!!.port = port.toInt()
            settings!!.isSsl = findViewById<RadioButton>(R.id.radio_https).isChecked
            fetchModels(settings!!)
        }
    }

    private fun fetchModels(settings: ClientSettings) {
        //Create connection url
        val url = Global.bakeUrl(settings)
        url ?: return

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val models = LlamaConnection(url).avaiableModelsArrayList
                    withContext(Dispatchers.Main) {
                        //Crete adapter for spinner
                        val adapter =
                            ArrayAdapter(context, android.R.layout.simple_spinner_item, models!!)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        findViewById<Spinner>(R.id.spinner_llamamodel).adapter = adapter

                        //Load last selected model and update view
                        if (adapter.getPosition(settings.model) >= 0)
                            findViewById<Spinner>(R.id.spinner_llamamodel).setSelection(
                                adapter.getPosition(
                                    settings.model
                                )
                            )
                    }
                } catch (ignore: Exception) {
                }
            }

        }
    }

    private fun loadConfig() {
        settings = FilesManager.loadSettings(context) ?: return

        //Llama model
        fetchModels(settings!!)

        //Connection IP port
        findViewById<TextInputEditText>(R.id.lbl_llamaip).setText(settings!!.ip)
        findViewById<TextInputEditText>(R.id.lbl_llamaport).setText(settings!!.port.toString())

        //Protocol
        findViewById<RadioButton>(R.id.radio_https).isChecked = (settings!!.isSsl)
        findViewById<RadioButton>(R.id.radio_http).isChecked = (!settings!!.isSsl)
    }

    private fun saveConfig() {
        //Validate data
        if ((findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString()).toInt() !in 0..65535) {
            Toast.makeText(context, "Error, port should be between 0 and 65535", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //Save settings
        val settings = ClientSettings().apply {
            ip = findViewById<TextInputEditText>(R.id.lbl_llamaip).text.toString().trim()
            port =
                (findViewById<TextInputEditText>(R.id.lbl_llamaport).text.toString().trim()).toInt()
            model = findViewById<Spinner>(R.id.spinner_llamamodel).selectedItem?.toString()
            isSsl = findViewById<RadioButton>(R.id.radio_https).isChecked
        }

        FilesManager.saveSettings(context, settings)
        finish()
    }
}