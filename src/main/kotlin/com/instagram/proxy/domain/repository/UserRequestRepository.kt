package com.instagram.proxy.domain.repository

import com.instagram.proxy.domain.db.Tables.USER_REQUESTS
import com.instagram.proxy.domain.model.TelegramMessage
import com.instagram.proxy.domain.model.TelegramUser
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class UserRequestRepository(
    val db: DSLContext
) {

    fun createRequest(user: TelegramUser, message: TelegramMessage) {
        val event = db.newRecord(USER_REQUESTS)

        event.messageId = message.messageId
        event.chatId = message.chatId
        event.senderId = user.tgId
        event.firstName = user.firstName
        event.lastName = user.lastName
        event.userName = user.userName
        event.messageText = message.text
        event.createdTs = message.timestamp.toLong()

        event.store()
    }
}