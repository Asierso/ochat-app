package com.asierso.ochat

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asierso.ochat.api.LlamaConnection
import com.asierso.ochat.databinding.ActivitySettingsBinding
import com.asierso.ochat.models.ClientSettings
import com.asierso.ochat.utils.Global
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SettingsActivity : AppCompatActivity() {
    private lateinit var context: Context
    private var settings: ClientSettings? = null
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this

        //Allow all downloads
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveConfig()
        }

        binding.btnDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle("Warning")
                .setMessage("Do you want to continue? All chats will be delete")
                .setNegativeButton("No") { dialogInterface, i ->

                }
                .setPositiveButton("Yes") { dialogInterface, i ->
                    FilesManager.removeAllChats(context)
                }.show()

        }

        binding.lblLlamaport.setOnKeyListener { view, i, keyEvent ->
            if(binding.lblLlamaport.text.toString().isNotBlank() && binding.lblLlamaport.text.toString().toInt() in 0..65535){
                binding.lblLlamaport.error = null
            }else{
                binding.lblLlamaport.error = "Port should be between 0 and 65535"
            }
            return@setOnKeyListener false
        }

        loadConfig()

        binding.btnModelsRefresh.setOnClickListener{
            binding.btnModelsRefresh.startAnimation(AnimationUtils.loadAnimation(context, R.anim.load_single_rotation_lineal))
            tryGetModels()
        }

        binding.switchOptimizeModels.setOnCheckedChangeListener { compoundButton, b ->
            if(b==true)
            MaterialAlertDialogBuilder(context)
                .setTitle("Warning")
                .setMessage("Optimization improve models reducing the chat context. This could generate lack of previous context but more accurated answers. Do you want to continue?")
                .setNegativeButton("No") { dialogInterface, i ->
                    binding.switchOptimizeModels.isChecked = false
                }
                .setPositiveButton("Yes") { dialogInterface, i ->
                    binding.switchOptimizeModels.isChecked = true
                }.show()
        }
    }

    private fun tryGetModels() {
        //Get IP and port from view
        val ip = binding.lblLlamaip.text.toString()
        val port = binding.lblLlamaport.text.toString().trim()

        //Try to fetch models if data is valid
        if (ip.isNotBlank() && port.isNotBlank()) {
            if (settings == null)
                settings = ClientSettings()

            //Update basic settings with view data
            settings!!.ip = ip
            settings!!.port = if(port.isBlank()) 0 else port.toInt()
            settings!!.isSsl = binding.radioHttps.isChecked
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
                        binding.spinnerLlamamodel.adapter = adapter

                        //Load last selected model and update view
                        if (adapter.getPosition(settings.model) >= 0)
                            binding.spinnerLlamamodel.setSelection(
                                adapter.getPosition(
                                    settings.model
                                )
                            )
                    }
                } catch (ignore: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.spinnerLlamamodel.adapter = null
                        Snackbar.make(binding.root,"Error at connecting with provided Ollama server",Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    private fun loadConfig() {
        settings = FilesManager.loadSettings(context) ?: return

        //Llama model
        fetchModels(settings!!)

        //Connection IP port
        binding.lblLlamaip.setText(settings!!.ip)
        binding.lblLlamaport.setText(settings!!.port.toString())

        //Protocol
        binding.radioHttps.isChecked = (settings!!.isSsl)
        binding.radioHttp.isChecked = (!settings!!.isSsl)

        //Use descriptions
        binding.switchUseDescriptions.isChecked = settings!!.isUseDescriptions

        //Optimize models
        binding.switchOptimizeModels.isChecked = settings!!.isOptimizeModels

        //Enable notify agent
        binding.switchNotifyAgent.isChecked = settings!!.isNotifyAgent

    }

    private fun saveConfig() {
        //Validate data
        if (binding.lblLlamaport.text.toString().isNotBlank() && binding.lblLlamaport.text.toString().toInt() !in 0..65535) {
            Toast.makeText(context, "Error, port should be between 0 and 65535", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //Save settings
        val settings = ClientSettings().apply {
            ip = binding.lblLlamaip.text.toString().trim()
            port = if(binding.lblLlamaport.text.toString().trim().isBlank()) 0 else binding.lblLlamaport.text.toString().trim().toInt()
            model = binding.spinnerLlamamodel.selectedItem?.toString()
            isSsl = binding.radioHttps.isChecked
            isUseDescriptions = binding.switchUseDescriptions.isChecked
            isOptimizeModels = binding.switchOptimizeModels.isChecked
            isNotifyAgent = binding.switchNotifyAgent.isChecked
        }

        FilesManager.saveSettings(context, settings)
        finish()
    }
}