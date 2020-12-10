/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@ContextConfiguration()
public class ConnectorTextAccessorTest {

    private static final String INVALID_MESSAGE = "invalidMessage";
    private static final String BOT_OBJECT = "bot.object";
    private static final String BOT_ACTION = "bot.action";
    private static final String KEY = "key";
    private static final String BASE_NAME = "cards/text";
    private static final String BOT_OBJECT_TITLE = "BotObjectTitle";
    private static final String BOT_OBJECT_DESCRIPTION = "BotObjectDescription";
    private static final String BOT_ACTION_ID_USER_INPUT_KEY_LABEL = "BotActionIdUserInputKeyLabel";
    private static final String BOT_ACTION_ID_LABEL = "BotActionIdLabel";
    private static final String BOT_ACTION_COMPLETED_LABEL = "BotActionCompletedLabel";
    private static final String TEST_HEADER = "testHeader";
    private static final String TEST_BODY = "testBody";
    private static final String BLANK_MESSAGE = "";
    private static final Object NULL_MESSAGE = null;

    @InjectMocks
    ConnectorTextAccessor connectorTextAccessor = new ConnectorTextAccessor(messageSource());


    private static Stream<Arguments> invalidInputsForConnectorTextAccessor() {
        return new ArgumentsStreamBuilder()
                .add(NULL_MESSAGE)
                .add(BLANK_MESSAGE)
                .add(INVALID_MESSAGE)
                .build();
    }

    @Test
    public void connectorTextAccessorGetTitleTest(){
        String actualString = connectorTextAccessor.getTitle(BOT_OBJECT, null);
        assertThat(actualString).isEqualTo(BOT_OBJECT_TITLE);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForConnectorTextAccessor")
    public void connectorTextAccessorGetTitleErrorStateTest(final String invalidInput){
        assertThatExceptionOfType(NoSuchMessageException.class)
                .isThrownBy(() -> connectorTextAccessor.getTitle(invalidInput, null));
    }

    @Test
    public void connectorTextAccessorGetDescriptionTest(){
        String actualString = connectorTextAccessor.getDescription(BOT_OBJECT, null);
        assertThat(actualString).isEqualTo(BOT_OBJECT_DESCRIPTION);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForConnectorTextAccessor")
    public void connectorTextAccessorGetDescriptionErrorStateTest(final String invalidInput){
        assertThatExceptionOfType(NoSuchMessageException.class)
                .isThrownBy(() -> connectorTextAccessor.getDescription(invalidInput, null));
    }

    @Test
    public void connectorTextAccessorGetActionUserInputLabelTest(){
        String actualString = connectorTextAccessor.getActionUserInputLabel(BOT_ACTION, KEY, null);
        assertThat(actualString).isEqualTo(BOT_ACTION_ID_USER_INPUT_KEY_LABEL);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForConnectorTextAccessor")
    public void connectorTextAccessorGetActionUserInputLabelErrorStateTest(final String invalidInput){
        assertThatExceptionOfType(NoSuchMessageException.class)
                .isThrownBy(() -> connectorTextAccessor.getActionUserInputLabel(invalidInput, KEY, null));
    }

    @Test
    public void connectorTextAccessorGetActionLabelTest(){
        String actualString = connectorTextAccessor.getActionLabel(BOT_ACTION, null);
        assertThat(actualString).isEqualTo(BOT_ACTION_ID_LABEL);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForConnectorTextAccessor")
    public void connectorTextAccessorGetActionLabelErrorStateTest(final String invalidInput){
        assertThatExceptionOfType(NoSuchMessageException.class)
                .isThrownBy(() -> connectorTextAccessor.getActionLabel(invalidInput, null));
    }

    @Test
    public void connectorTextAccessorGetActionCompletedLabelTest(){
        String actualString = connectorTextAccessor.getActionCompletedLabel(BOT_ACTION, null);
        assertThat(actualString).isEqualTo(BOT_ACTION_COMPLETED_LABEL);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForConnectorTextAccessor")
    public void connectorTextAccessorGetActionCompletedLabelErrorStateTest(final String invalidInput){
        assertThatExceptionOfType(NoSuchMessageException.class)
                .isThrownBy(() -> connectorTextAccessor.getActionCompletedLabel(invalidInput, null));
    }

    @Test
    public void connectorTextAccessorGetHeaderTest(){
        String actualString = connectorTextAccessor.getHeader(null);
        assertThat(actualString).isEqualTo(TEST_HEADER);
    }

    @Test
    public void connectorTextAccessorGetBodyTest(){
        String actualString = connectorTextAccessor.getBody(null);
        assertThat(actualString).isEqualTo(TEST_BODY);
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setBasename(BASE_NAME);
        return messageSource;
    }

}
