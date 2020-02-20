package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.common.payloads.response.CardActionInputField
import com.vmware.connectors.common.payloads.response.CardBodyField
import com.vmware.connectors.common.payloads.response.CardBodyFieldItem
import com.vmware.connectors.common.payloads.response.CardBodyFieldType
import com.vmware.connectors.common.utils.CardTextAccessor
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.util.*

@Component
class CardUtils(
        @Autowired val cardTextAccessor: CardTextAccessor
) {

    /**
     * Card Action Input Field Builder
     *
     * @param id: action user input field id
     * @param format: action user input field format. Example values are text, textarea, date, etc..
     * @param labelKey: field literal key
     * @param locale: User Locale
     * @return CardActionInputField
     */
    fun buildUserInputField(id: String, format: String, labelKey: String, locale: Locale?): CardActionInputField {
        return CardActionInputField
                .Builder()
                .setId(id)
                .setFormat(format)
                .setLabel(cardTextAccessor.getMessage(labelKey, locale))
                .build()
    }

    /**
     * Builder for CardBodyField
     *
     * @param titleMessageKey: prefix of field literal key
     * @param content: field value
     * @param locale: User locale
     * @return CardBodyField
     */
    fun buildGeneralBodyField(titleMessageKey: String, content: String, locale: Locale?): CardBodyField? {
        return if (StringUtils.isBlank(content)) {
            null
        } else CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("$titleMessageKey.title", locale))
                .setDescription(cardTextAccessor.getMessage("$titleMessageKey.content", locale, content))
                .setType(CardBodyFieldType.GENERAL)
                .build()
    }

    /**
     * Builder for CardBodyField
     *
     * @param titleMessageKey: prefix of field literal key
     * @param attachments: Attachments List
     * @param locale: User locale
     * @return CardBodyField
     */
    fun buildAttachmentBodyField(
            titleMessageKey: String,
            attachments: List<Pair<String, String>>,
            locale: Locale?
    ): CardBodyField? {
        return if (attachments.isEmpty()) {
            null
        } else {
            val attachmentField = CardBodyField.Builder()
                    .setTitle(cardTextAccessor.getMessage("$titleMessageKey.title", locale))
                    .setType(CardBodyFieldType.SECTION)
            attachments.map { (attachmentUrl, fileName) ->
                val fieldItem = buildAttachmentBodyFieldItem(fileName, attachmentUrl)
                attachmentField.addItem(fieldItem)
            }
            return attachmentField.build()
        }
    }

    /**
     * Builder for CardBodyField
     *
     * @param fileName: Name of the Attachment
     * @param attachmentUrl: Attachment Url
     * @return CardBodyFieldItem
     */
    private fun buildAttachmentBodyFieldItem(
            fileName: String,
            attachmentUrl: String
    ): CardBodyFieldItem {
        val decodedUrl = URLDecoding.decodeValue(attachmentUrl)
        return CardBodyFieldItem.Builder()
                .setAttachmentName(fileName)
                .setTitle(fileName)
                .setAttachmentMethod(HttpMethod.GET)
                .setAttachmentUrl(decodedUrl)
                .setType(CardBodyFieldType.ATTACHMENT_URL)
                .setAttachmentContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build()
    }

}