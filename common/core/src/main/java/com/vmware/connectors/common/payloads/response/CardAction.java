/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * This class represents a single action that can be taken by the user to whom the containing Card is displayed.
 * <p>
 * Instances of this class are unmodifiable once created. The CardAction class cannot be directly constructed;
 * use the CardAction.Builder class to create and populate a CardAction instance.
 * <p>
 * Created by Rob Worsnop on 9/13/16.
 */
@JsonInclude(NON_NULL)
public class CardAction {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("url")
    private Link url;

    @JsonProperty("type")
    private HttpMethod type;

    @JsonProperty("action_key")
    private String actionKey;

    @JsonProperty("request")
    private final Map<String, String> request;

    @JsonProperty("user_input")
    private final List<CardActionInputField> userInput;

    @JsonProperty("completed_label")
    private String completedLabel;

    // Don't instantiate directly - use the Builder class below
    private CardAction() {
        this.type = HttpMethod.GET;
        this.request = new HashMap<>();
        this.userInput = new ArrayList<>();
        this.id = UUID.randomUUID();
    }

    /**
     * Get the CardAction's id, a unique identifier for the action.
     *
     * @return the CardAction's id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the CardAction's label, a human-readable description of the action.
     *
     * @return the CardAction's label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get a Link containing the URL of the web service to which the client will send a request
     * if the user selects this action.
     *
     * @return The URL of the web service to be called
     */
    public Link getUrl() {
        return url;
    }

    /**
     * Get the HTTP method (GET, POST, etc) that will be used to call the web service
     * specified by <code>getUrl()</code>.
     *
     * @return The HTTP method to be used in the call to the web service
     */
    public HttpMethod getType() {
        return type;
    }

    /**
     * Get the CardAction's action key, which is an arbitrary string used by the client to distinguish this
     * action from other actions that may be sent.
     *
     * @return The action's key
     */
    public String getActionKey() {
        return actionKey;
    }

    /**
     * Returns a map of key/value pairs to be submitted to the value of {@link CardAction#getUrl()} using a form.
     * We would expect a template to convert these values to hidden input fields.
     * <p>
     * The Map returned by this method is <i>unmodifiable</i> - its contents cannot be changed once the
     * CardAction is created. Use CardAction.Builder.addRequestParam() to add request parameters to the Action.
     *
     * @return the request values
     */
    public Map<String, String> getRequest() {
        return Collections.unmodifiableMap(request);
    }

    /**
     * Returns a list of user input elements to be submitted to the value of {@link CardAction#getUrl()} using a form.
     * We would expect a template to convert these values to input fields, where the id of the
     * field matches the value in the set.
     * <p>
     * The List returned by this method is <i>unmodifiable</i> - its contents cannot be changed once the
     * CardAction is created. Use CardAction.Builder.addUserInput() to add user input fields to the Action.
     *
     * @return the user input field IDs, in the order they were added
     */
    public List<CardActionInputField> getUserInput() {
        return Collections.unmodifiableList(userInput);
    }

    /**
     * Returns the "completed label". This is the text to be displayed to the user on successful
     * completion of the action.
     *
     * @return the completed label
     */
    public String getCompletedLabel() {
        return completedLabel;
    }

    /**
     * This class allows the construction of CardAction objects. To use, create a Builder instance, call its methods
     * to populate the CardAction, and call build() to receive the completed CardAction and reset the builder.
     * <p>
     * A CardAction can be discarded during creation, returning the Builder to its initial state, by calling reset().
     * The build() method calls reset() internally.
     */
    public static class Builder {

        private CardAction action;

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            action = new CardAction();
        }

        /**
         * Discard the CardAction currently under construction and return the Builder to its initial state.
         */
        public void reset() {
            action = new CardAction();
        }

        /**
         * Set the id of the CardAction under construction.
         *
         * @param id the CardAction's id
         * @return this Builder instance, for method chaining
         */

        public Builder setId(UUID id) {
            action.id = id;
            return this;
        }

        /**
         * Set the label of the CardAction under construction.
         *
         * @param label the CardAction's label
         * @return this Builder instance, for method chaining
         */
        public Builder setLabel(String label) {
            action.label = label;
            return this;
        }

        /**
         * Set the URL of the endpoint of the web service to which this CardAction will send a request.
         *
         * @param href The URL of the service endpoint for this Action.
         * @return this Builder instance, for method chaining
         */
        public Builder setUrl(String href) {
            action.url = new Link(href);
            return this;
        }

        /**
         * Set the HTTP method (GET, POST, etc) of the CardAction under construction. Defaults to HttpMethod.GET if
         * no value is set here.
         *
         * @param type the HTTP method of the CardAction
         * @return this Builder instance, for method chaining
         */
        public Builder setType(HttpMethod type) {
            action.type = type;
            return this;
        }

        /**
         * Set a key for the CardAction under construction, to be used by the client in rendering the Card.
         *
         * @param key the CardAction's key
         * @return this Builder instance, for method chaining
         */
        public Builder setActionKey(String key) {
            action.actionKey = key;
            return this;
        }

        /**
         * Set a key for the CardAction under construction, to be used by the client in rendering the Card.
         *
         * @param key the CardAction's key
         * @return this Builder instance, for method chaining
         */
        public Builder setActionKey(CardActionKey key) {
            action.actionKey = key.name();
            return this;
        }

        /**
         * Add a name/value pair to be sent with the request generated by the CardAction.
         * The key/value pairs are <i>not</i> stored in insertion order.
         *
         * @param key   The name of the parameter to be added
         * @param value The value of the parameter to be added
         * @return this Builder instance, for method chaining
         */
        public Builder addRequestParam(String key, String value) {
            action.request.put(key, value);
            return this;
        }

        /**
         * Add a specification for a parameter to be supplied by the user and sent with the request generated by
         * the CardAction. input fields are stored in insertion order.
         *
         * @param inputField The field for which the user will be prompted.
         * @return this Builder instance, for method chaining
         */
        public Builder addUserInputField(CardActionInputField inputField) {
            action.userInput.add(inputField);
            return this;
        }

        /**
         * Sets the "completed label". This is the text to be displayed to the user on successful
         * completion of the action
         *
         * @param completedLabel the completed label
         * @return this Builder instance, for method chaining
         */
        public Builder setCompletedLabel(String completedLabel) {
            action.completedLabel = completedLabel;
            return this;
        }

        /**
         * Return the CardAction under construction and reset the Builder to its initial state.
         *
         * @return The completed CardAction
         */
        @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
        public CardAction build() {
            CardAction completedAction = this.action;
            reset();
            return completedAction;
        }
    }
}
