package com.asierso.ochat.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.asierso.ochat.utils.ChatOptimizer
import com.asierso.ochat.FilesManager
import com.asierso.ochat.api.LlamaConnection
import com.asierso.ochat.api.builder.LlamaDialogsBuilder
import com.asierso.ochat.api.builder.LlamaRequestBaseBuilder
import com.asierso.ochat.api.handlers.LlamaConnectionException
import com.asierso.ochat.api.models.LlamaMessage
import com.asierso.ochat.utils.Global
import com.asierso.ochat.utils.ForegroundListener
import com.asierso.ochat.utils.NotificationManagerSystem
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class NotifyAgentWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        //Detects if app isn't in foreground
        while (ForegroundListener.isForeground) {
            //Wait until app is closed or secondary
        }

        TimeUnit.MINUTES.sleep(Random.nextInt(5, 20).toLong())

        //If app activity is resumed, avoid to generate notification with user inside
        if (ForegroundListener.isForeground)
            return Result.failure()

        val settings = FilesManager.loadSettings(applicationContext)

        if (settings != null && settings.isNotifyAgent && settings.model != null && settings.model.isNotBlank()) {
            //Load previous conversation
            val conversation = FilesManager.loadConversation(
                applicationContext,
                "${settings.ip}_${settings.model}"
            )

            if (conversation != null) {
                //Get llama api url
                val llama = LlamaConnection(Global.bakeUrl(settings) ?: "")

                //Build llama connection
                val llamaRequestBase = LlamaRequestBaseBuilder()
                    .useModel(settings.model.toString())
                    .withStream(false)
                    .build()

                //Build dialogs
                val dialogBuilder = LlamaDialogsBuilder(llamaRequestBase)
                for (handle in if (settings.isOptimizeModels) ChatOptimizer(conversation).getOptimizedChat() else conversation.chat)
                    dialogBuilder.createDialog(handle.role, handle.content)

                dialogBuilder.createDialog(LlamaMessage.ASSISTANT_ROLE, "")

                try {
                    //Try to fetch IA to receive new response
                    var llamaResponse = llama.fetch(
                        dialogBuilder.build()
                    )

                    //Check if there was problems generating response and cancel worker
                    if (llamaResponse.message.content.toString().trim().isBlank())
                        return Result.failure()

                    //Add new response and save it
                    conversation.chat.add(llamaResponse.message)
                    FilesManager.saveConversation(
                        applicationContext,
                        "${settings.ip}_${settings.model}",
                        conversation
                    )

                    //Show response as notification
                    var iaName =
                        if (settings.model.toString().contains(":")) settings.model.toString()
                            .split(":")[0] else settings.model.toString()
                    NotificationManagerSystem.getInstance(applicationContext)!!
                        .sendNotification(
                            applicationContext,
                            iaName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            llamaResponse.message.content.toString()
                        )

                } catch (e: LlamaConnectionException) {
                    return Result.failure()
                }
            }
        }
        return Result.success()
    }
}