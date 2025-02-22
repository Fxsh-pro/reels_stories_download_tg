package com.instagram.proxy.service

import com.instagram.proxy.domain.model.TelegramMessage
import com.instagram.proxy.domain.model.TelegramUser
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import kotlin.system.measureTimeMillis

@Service
class TelegramBot(
    private val telegramHandlerService: TelegramHandlerService,
    @Value("\${telegram.bot.token}") private val botToken: String,
    @Value("\${telegram.bot.name}") private val botName: String,
) : TelegramLongPollingBot(botToken) {

    @PostConstruct
    fun t() {
        println("Token : $botToken")
    }

    private val LOG = LoggerFactory.getLogger(TelegramBot::class.java)

    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botName

    override fun onUpdateReceived(update: Update) {
        println("RECEIVED MESSAGE")
        GlobalScope.launch(Dispatchers.Default) {
            val tgChatId = update.message.chatId.toInt()
            val tgChat = update.message.chat

            val sender = TelegramUser(
                tgId = tgChatId,
                firstName = tgChat.firstName ?: "",
                lastName = tgChat.lastName ?: "",
                userName = tgChat.userName ?: "",
            )

            val telegramMessage = TelegramMessage(
                update.message.messageId,
                chatId = tgChatId,
                sender = sender,
                text = update.message.text,
                timestamp = update.message.date
            )
            try {
                var downloadedFiles: List<File>
                val time = measureTimeMillis {
                    downloadedFiles = telegramHandlerService.handleIncomingMessage(telegramMessage)
                }
                if (downloadedFiles.isEmpty()) {
                    sendMessage(tgChatId.toString(), "No media files found for the provided URL.")
                } else {
                    sendMediaToTelegram(tgChatId.toString(), downloadedFiles)
                }
            } catch (e: Exception) {
                LOG.error("Error processing the Telegram message: ${e.message}", e)
                sendMessage(tgChatId.toString(), "An error occurred while processing your request.")
            }
        }
    }

    private fun sendMediaToTelegram(chatId: String, files: List<File>) {
        try {
            val photos = files.filter { it.extension == "jpg" }
            val videos = files.filter { it.extension == "mp4" }

            if (photos.isNotEmpty()) {
                if (photos.size == 1) {
                    val inputFile = InputFile(photos[0])
                    val photoMessage = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(inputFile)
                        .build()
                    execute(photoMessage)
                } else {
                    val photoFileIds = mutableListOf<String>()
                    runBlocking {
                        val jobs = photos.mapIndexed { index, photo ->
                            GlobalScope.launch(Dispatchers.IO) {
                                try {
                                    val inputFile = InputFile(photo)
                                    val photoMessage = SendPhoto.builder()
                                        .chatId(6405427252)
                                        .photo(inputFile)
                                        .build()

                                    val sentMessage = execute(photoMessage)
                                    val fileId = sentMessage.photo?.last()?.fileId
                                    if (fileId != null) {
                                        synchronized(photoFileIds) {
                                            photoFileIds.add(fileId)
                                        }
                                    }

                                } catch (e: TelegramApiException) {
                                    LOG.error("Error sending image ${photo.name}: ${e.message}", e)
                                }
                            }
                        }

                        jobs.forEach { it.join() }
                    }
                    if (photoFileIds.isNotEmpty()) {
                        val mediaGroup = photoFileIds.map { fileId ->
                            InputMediaPhoto.builder()
                                .media(fileId)
                                .build()
                        }

                        val sendMediaGroupRequest = SendMediaGroup.builder()
                            .chatId(chatId)
                            .medias(mediaGroup)
                            .build()

                        execute(sendMediaGroupRequest)
                        LOG.info("Sent ${photos.size} photos as a media group.")
                    }
                }
            }
            videos.forEach { video ->
                val videoMessage = SendVideo.builder()
                    .chatId(chatId)
                    .video(InputFile(video))
                    .build()
                execute(videoMessage)
                LOG.info("Sent video: ${video.name}")
            }
        } catch (e: TelegramApiException) {
            LOG.error("Error sending media to Telegram: ${e.message}", e)
        } finally {
            files.forEach { file ->
                if (file.exists() && file.delete()) {
                } else {
                    LOG.warn("Failed to delete file: ${file.absolutePath}")
                }
            }
        }
    }

    private fun sendMessage(chatId: String, message: String) {
        val sendMessage = SendMessage.builder()
            .chatId(chatId)
            .text(message)
            .build()
        try {
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            LOG.error("Error sending message: ${e.message}", e)
        }
    }
}