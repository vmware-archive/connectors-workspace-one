/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const uuid = require('uuid/v4');



const httpMethods = Object.freeze({
    GET: 'GET',
    HEAD: 'HEAD',
    POST: 'POST',
    PUT: 'PUT',
    PATCH: 'PATCH',
    DELETE: 'DELETE',
    OPTIONS: 'OPTIONS',
    TRACE: 'TRACE'
});

// http://json-schema.org/latest/json-schema-validation.html#rfc.section.7
const inputFieldFormats = Object.freeze({
    date_time: 'date-time',
    date: 'date',
    time: 'time',
    email: 'email',
    idn_email: 'idn-email',
    hostname: 'hostname',
    idn_hostname: 'idn-hostname',
    ipv4: 'ipv4',
    ipv6: 'ipv6',
    uri: 'uri',
    uri_reference: 'uri-reference',
    iri: 'iri',
    iri_reference: 'iri-reference',
    uri_template: 'uri-template',
    json_pointer: 'json-pointer',
    relative_json_pointer: 'relative-json-pointer',
    regex: 'regex'
});

const cardFieldTypes = Object.freeze({
    GENERAL: 'GENERAL',
    COMMENT: 'COMMENT',
    ATTACHMENT: 'ATTACHMENT',
    TRIPINFO: 'TRIPINFO'
});

const cardActionKeys = Object.freeze({
    DIRECT: 'DIRECT',
    DISMISS: 'DISMISS',
    USER_INPUT: 'USER_INPUT',
    OPEN_IN: 'OPEN_IN',
    INSTALL_APP: 'INSTALL_APP'
});



function copy(obj) {
    /*
     * Copy the objects before returning it so builders can potentially be
     * reused after building (sometimes this is useful in unit tests).
     */
    return JSON.parse(JSON.stringify(obj));
}

function processDateInput(date) {
    if (typeof date === 'number' || date instanceof Number) {
        return new Date(date).toISOString();
    } else if (date instanceof Date) {
        return date.toISOString();
    }
    /*
     * Hopefully, they passed in something that toStrings to an ISO-8601
     * formatted date string.
     */
    return creationDate.toString();
}



function CardBuilder() {
    if (!this instanceof CardBuilder) {
        return new CardBuilder();
    }

    const builder = this;

    // the defaults
    const card = {
        id: uuid(),
        creation_date: new Date().toISOString(),
        actions: []
    };

    function setId(id) {
        card.id = id;
        return builder;
    }

    // TODO - put in setName?  probably not b/c we want to phase it out?

    function setTemplate(templateUrl) {
        card.template = {
            href: templateUrl
        };
        return builder;
    }

    function setHeader(title, subtitle) {
        card.header = {
            title: title
        };
        if (subtitle !== undefined) {
            card.header.subtitle = subtitle;
        }
        return builder;
    }

    function setCreationDate(creationDate) {
        card.creation_date = processDateInput(creationDate);
        return builder;
    }

    function setExpirationDate(expirationDate) {
        card.expiration_date = processDateInput(expirationDate);
        return builder;
    }

    function setBody(body) {
        card.body = body;
        return builder;
    }

    function addAction(action) {
        card.actions.push(action);
        return builder;
    }

    function build() {
        return copy(card);
    }

    // the exposed methods
    builder.setId = setId;
    builder.setTemplate = setTemplate;
    builder.setHeader = setHeader;
    builder.setCreationDate = setCreationDate;
    builder.setExpirationDate = setExpirationDate;
    builder.setBody = setBody;
    builder.addAction = addAction;
    builder.build = build;

    return builder;
}

function CardBodyBuilder() {
    if (!this instanceof CardBodyBuilder) {
        return new CardBodyBuilder();
    }

    const builder = this;

    // the defaults
    const body = {
        description: '',
        fields: []
    };

    function setDescription(description) {
        body.description = description;
        return builder;
    }

    function addField(field) {
        body.fields.push(field);
        return builder;
    }

    function build() {
        return copy(body);
    }

    // the exposed methods
    builder.setDescription = setDescription;
    builder.addField = addField;
    builder.build = build;

    return builder;
}

function CardFieldBuilder() {
    if (!this instanceof CardFieldBuilder) {
        return new CardFieldBuilder();
    }

    const builder = {};

    // the defaults
    const field = {
        content: []
    };

    function setType(type) {
        field.type = type;
        return builder;
    }

    function setTitle(title) {
        field.title = title;
        return builder;
    }

    function setDescription(description) {
        field.description = description;
        return builder;
    }

    function addContent(content) {
        field.content.push(content);
        return builder;
    }

    function addTextContent(content) {
        field.content.push({
            text: content
        });
        return builder;
    }

    function build() {
        return copy(field);
    }

    // the exposed methods
    builder.setType = setType;
    builder.setTitle = setTitle;
    builder.setDescription = setDescription;
    builder.addContent = addContent;
    builder.addTextContent = addTextContent;
    builder.build = build;

    return builder;
}

function CardActionBuilder() {
    if (!this instanceof CardActionBuilder) {
        return new CardActionBuilder();
    }

    const builder = this;

    // the defaults
    const action = {
        id: uuid(),
        type: httpMethods.GET,
        request: {},
        user_input: []
    };

    function setId(id) {
        action.id = id;
        return builder;
    }

    function setType(type) {
        action.type = type;
        return builder;
    }

    function setUrl(url) {
        action.url = {
            href: url
        };
        return builder;
    }

    function setLabel(label) {
        action.label = label;
        return builder;
    }

    function setCompletedLabel(label) {
        action.completed_label = label;
        return builder;
    }

    function setActionKey(actionKey) {
        action.action_key = actionKey;
        return builder;
    }

    function addRequestParam(key, value) {
        action.request[key] = value;
        return builder;
    }

    function addUserInputField(userInputField) {
        action.user_input.push(userInputField);
        return builder;
    }

    function build() {
        return copy(action);
    }

    // the exposed methods
    builder.setId = setId;
    builder.setType = setType;
    builder.setUrl = setUrl;
    builder.setLabel = setLabel;
    builder.setCompletedLabel = setCompletedLabel;
    builder.setActionKey = setActionKey;
    builder.addRequestParam = addRequestParam;
    builder.addUserInputField = addUserInputField;
    builder.build = build;

    return builder;
}

function CardActionInputFieldBuilder() {
    if (!this instanceof CardActionInputFieldBuilder) {
        return new CardActionInputFieldBuilder();
    }

    const builder = this;

    // the defaults
    const inputField = {
        options: {}
    };

    function setId(id) {
        inputField.id = id;
        return builder;
    }

    function setLabel(label) {
        inputField.label = label;
        return builder;
    }

    function setFormat(format) {
        inputField.format = format;
        return builder;
    }

    function setMinLength(minLength) {
        inputField.min_length = minLength;
        return builder;
    }

    function setMaxLength(maxLength) {
        inputField.max_length = maxLength;
        return builder;
    }

    function addOption(label, value) {
        inputField.options[label] = value;
        return builder;
    }

    function build() {
        return copy(inputField);
    }

    // the exposed methods
    builder.setId = setId;
    builder.setLabel = setLabel;
    builder.setFormat = setFormat;
    builder.setMinLength = setMinLength;
    builder.setMaxLength = setMaxLength;
    builder.addOption = addOption;
    builder.build = build;

    return builder;
}



// the exposed constructors and constants

exports.httpMethods = httpMethods;
exports.inputFieldFormats = inputFieldFormats;
exports.cardFieldTypes = cardFieldTypes;
exports.cardActionKeys = cardActionKeys;

exports.CardBuilder = CardBuilder;
exports.CardBodyBuilder = CardBodyBuilder;
exports.CardFieldBuilder = CardFieldBuilder;
exports.CardActionBuilder = CardActionBuilder;
exports.CardActionInputFieldBuilder = CardActionInputFieldBuilder;
