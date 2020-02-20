package com.vmware.connectors.ms.graph.utils

import com.vmware.connectors.common.payloads.response.CardActionInputField
import com.vmware.connectors.common.payloads.response.CardBodyField
import com.vmware.connectors.common.payloads.response.CardBodyFieldType
import com.vmware.connectors.common.utils.CardTextAccessor
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
 * card utils object
 *
 * @property cardTextAccessor CardTextAccessor: Internationalization module that is used while preparing cards
 */
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
    fun buildUserInputField(
            id: String,
            format: String,
            labelKey: String,
            locale: Locale?
    ): CardActionInputField {
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
    fun buildGeneralBodyField(
            titleMessageKey: String,
            content: String,
            locale: Locale?
    ): CardBodyField? {
        return if (StringUtils.isBlank(content)) {
            null
        } else CardBodyField.Builder()
            .setTitle(cardTextAccessor.getMessage("$titleMessageKey.title", locale))
            .setDescription(cardTextAccessor.getMessage("$titleMessageKey.content", locale, content))
            .setType(CardBodyFieldType.GENERAL)
            .build()
    }

//    /**
//     * Card Action Open Url Action Builder
//     *
//     * @param actionLabelKey
//     * @param url: url to open
//     * @param locale: User Locale
//     * @param allowRepeated: if true, user can make this action more than 1 time
//     * @param removeCardOnCompletion: if true, card is removed once user takes action
//     * @return CardAction.Builder
//     */
//    fun buildOpenActionBuilder(actionLabelKey: String, url: String, locale: Locale?, allowRepeated: Boolean = true, removeCardOnCompletion: Boolean = false): CardAction.Builder {
//        return CardAction.Builder()
//            .setLabel(cardTextAccessor.getActionLabel(actionLabelKey, locale))
//            .setActionKey("OPEN_IN")
//            .setUrl(url)
//            .setType(HttpMethod.GET)
//            .setAllowRepeated(allowRepeated)
//            .setRemoveCardOnCompletion(removeCardOnCompletion)
//    }
}


