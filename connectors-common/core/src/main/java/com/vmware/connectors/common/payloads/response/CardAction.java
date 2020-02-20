/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * This class represents a single action that can be taken by the user to whom the containing Card is displayed.
 * <p>
 * Instances of this class are unmodifiable once created. The CardAction class cannot be directly constructed;
 * use the CardAction.Builder class to create and populate a CardAction instance.
 * <p>
 * Created by Rob Worsnop on 9/13/16.
 */
@JsonInclude(NON_NULL)
@SuppressWarnings("PMD.LinguisticNaming")
public class CardAction {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("primary")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean primary;

    @JsonProperty("label")
    private String label;

    @JsonProperty("url")
    private Link url;

    @JsonProperty("type")
    private HttpMethod type;

    @JsonProperty("action_key")
    private String actionKey;

    @JsonProperty("remove_card_on_completion")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean removeCardOnCompletion;

    @JsonProperty("request")
    private final Map<String, String> request;

    @JsonProperty("user_input")
    private final List<CardActionInputField> userInput;

    @JsonProperty("completed_label")
    private String completedLabel;

    @JsonProperty("allow_repeated")
    private boolean allowRepeated;

    @JsonProperty("mutually_exclusive_set_id")
    private String mutuallyExclusiveSetId;

    // Don't instantiate directly - use the Builder class below
    private CardAction() {
        this.type = HttpMethod.GET;
        this.request = new HashMap<>();
        this.userInput = new ArrayList<>();
        this.id = UUID.randomUUID();
        this.completedLabel = "Completed";
    }

    /**
     * Get the {@link CardAction}'s id, a unique identifier for the action.
     *
     * @return the {@link CardAction}'s id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the {@link CardAction}'s primary flag, a boolean indicating whether the action is a primary action or not.
     *
     * @return the {@link CardAction}'s primary flag
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Get the {@link CardAction}'s label, a human-readable description of the action.
     *
     * @return the {@link CardAction}'s label
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
     * Get the {@link CardAction}'s action key, which is an arbitrary string used by the client to distinguish this
     * action from other actions that may be sent.
     *
     * @return The action's key
     */
    public String getActionKey() {
        return actionKey;
    }

    /**
     * Get the {@link CardAction}'s removeCardOnCompletion flag, which specifies whether or not the client
     * should remove the card when this action completes.
     *
     * @return The action's removeCardOnCompletion flag
     */
    public boolean isRemoveCardOnCompletion() {
        return removeCardOnCompletion;
    }

    /**
     * Returns a map of key/value pairs to be submitted to the value of {@link CardAction#getUrl()} using a form.
     * We would expect a template to convert these values to hidden input fields.
     * <p>
     * The Map returned by this method is <i>unmodifiable</i> - its contents cannot be changed once the
     * {@link CardAction} is created. Use {@link CardAction.Builder#addRequestParam(String, String)} to add request parameters to the Action.
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
     * CardAction is created. Use {@link CardAction.Builder#addUserInputField(CardActionInputField)} to add user input fields to the Action.
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
     * Returns "allow repeated" value. If set to true, then the client can enable
     * any card action always irrespective of "completed label".
     *
     * @return the allow repeated.
     */
    public boolean isAllowRepeated() {
        return allowRepeated;
    }

    /**
     * Returns "mutuallyExclusiveSetId" property. If set, performing a card action
     * will disable other card actions.
     *
     * @return the mutually exclusive id.
     */
    public String getMutuallyExclusiveSetId() {
        return mutuallyExclusiveSetId;
    }

    /**
     * This class allows the construction of {@link CardAction} objects. To use, create a Builder instance, call its methods
     * to populate the {@link CardAction}, and call build() to receive the completed {@link CardAction} and reset the builder.
     * <p>
     * A {@link CardAction} can be discarded during creation, returning the Builder to its initial state, by calling reset().
     * The build() method calls reset() internally.
     */
    public static class Builder {

        private CardAction action;

        /**
         * Creates a new Builder instance with properties set for a dismiss action.
         */
        public static Builder dismissAction() {
            return new Builder()
                    // currently, we have to provide GET and a url to get the client to handle the dismiss action properly
                    .setType(HttpMethod.GET)
                    .setUrl("/dismiss")
                    .setRemoveCardOnCompletion(true)
                    .setActionKey(CardActionKey.DISMISS);
        }

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            action = new CardAction();
        }

        /**
         * Discard the {@link CardAction} currently under construction and return the Builder to its initial state.
         */
        public void reset() {
            action = new CardAction();
        }

        /**
         * Set the id of the {@link CardAction} under construction.
         *
         * @param id the {@link CardAction}'s id
         * @return this Builder instance, for method chaining
         */

        public Builder setId(UUID id) {
            action.id = id;
            return this;
        }

        /**
         * Set the primary flag of the {@link CardAction} under construction.
         *
         * @param primary the {@link CardAction}'s primary flag
         * @return the Builder instance, for method chaining
         */
        public Builder setPrimary(boolean primary) {
            action.primary = primary;
            return this;
        }

        /**
         * Set the label of the {@link CardAction} under construction.
         *
         * @param label the {@link CardAction}'s label
         * @return this Builder instance, for method chaining
         */
        public Builder setLabel(String label) {
            action.label = label;
            return this;
        }

        /**
         * Set the URL of the endpoint of the web service to which this {@link CardAction} will send a request.
         *
         * @param href The URL of the service endpoint for this Action.
         * @return this Builder instance, for method chaining
         */
        public Builder setUrl(String href) {
            action.url = new Link(href);
            return this;
        }

        /**
         * Set the HTTP method (GET, POST, etc) of the {@link CardAction} under construction. Defaults to HttpMethod.GET if
         * no value is set here.
         *
         * @param type the HTTP method of the {@link CardAction}
         * @return this Builder instance, for method chaining
         */
        public Builder setType(HttpMethod type) {
            action.type = type;
            return this;
        }

        /**
         * Set a key for the {@link CardAction} under construction, to be used by the client in rendering the Card.
         *
         * @param key the {@link CardAction}'s key
         * @return this Builder instance, for method chaining
         */
        public Builder setActionKey(String key) {
            action.actionKey = key;
            return this;
        }

        /**
         * Set a key for the {@link CardAction} under construction, to be used by the client in rendering the Card.
         *
         * @param key the {@link CardAction}'s key
         * @return this Builder instance, for method chaining
         */
        public Builder setActionKey(CardActionKey key) {
            action.actionKey = key.name();
            return this;
        }

        /**
         * Set the removeCardOnCompletion flag for the {@link CardAction} under construction, to be used
         * by the client for determining whether or not to remove the card when the action completes.
         *
         * @param removeCardOnCompletion the {@link CardAction}'s removeCardOnCompletion flag
         * @return this Builder instance, for method chaining
         */
        public Builder setRemoveCardOnCompletion(boolean removeCardOnCompletion) {
            action.removeCardOnCompletion = removeCardOnCompletion;
            return this;
        }

        /**
         * Add a name/value pair to be sent with the request generated by the {@link CardAction}.
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
         * the {@link CardAction}. input fields are stored in insertion order.
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
         * Set "allow repeated" value. If set to true, then the client can enable any card action
         * always irrespective of "completed label" value has been set or not.
         *
         * @param allowRepeated the allowRepeated flag.
         * @return this Builder instance, for method chaining.
         */
        public Builder setAllowRepeated(final boolean allowRepeated) {
            action.allowRepeated = allowRepeated;
            return this;
        }

        /**
         * Set "mutuallyExclusiveSetId" value. If set, performing a card action will disable other card actions.
         * <p>
         * Only one card action can be performed at any point of time.
         *
         * @param mutuallyExclusiveSetId
         * @return
         */
        public Builder setMutuallyExclusiveSetId(final String mutuallyExclusiveSetId) {
            action.mutuallyExclusiveSetId = mutuallyExclusiveSetId;
            return this;
        }

        /**
         * Return the {@link CardAction} under construction and reset the Builder to its initial state.
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

    public String hash() {
        final String url = this.url == null ? null : this.url.getHref();

        final List<String> userInputHashList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userInput)) {
            userInput.forEach(actionInput ->
                    userInputHashList.add(actionInput == null ? StringUtils.SPACE : actionInput.hash())
            );
        }

        return HashUtil.hash(
                "primary: ", this.primary,
                "label: ", this.label,
                "url: ", url,
                "type: ", this.type.name(),
                "action_key: ", this.actionKey,
                "remove_card_on_completion: ", this.removeCardOnCompletion,
                "request: ", HashUtil.hashMap(this.request),
                "user_input: ", HashUtil.hashList(userInputHashList),
                "completed_label: ", this.completedLabel,
                "allow_repeated: ", this.allowRepeated,
                "mutually_exclusive_set_id: ", this.mutuallyExclusiveSetId
        );
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
