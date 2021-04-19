package com.vmware.connectors.concur;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.LinguisticNaming")
public class ActionFailureResponse {
    @JsonProperty("title")
    private String title;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("actions")
    private final List<OpenInAction> actions;

    // Don't instantiate directly, use the builder instead.
    private ActionFailureResponse() {
        this.actions = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<OpenInAction> getActions() {
        return actions;
    }

    public static class Builder {
        private ActionFailureResponse responseObject;

        // Create a builder instance.
        public Builder() {
            responseObject = new ActionFailureResponse();
        }

        public void reset() {
            this.responseObject = new ActionFailureResponse();
        }

        public ActionFailureResponse.Builder setTitle(String title) {
            this.responseObject.title = title;
            return this;
        }

        public ActionFailureResponse.Builder setErrorMessage(String errorMessage) {
            this.responseObject.errorMessage = errorMessage;
            return this;
        }

        public ActionFailureResponse.Builder addAction(OpenInAction action) {
            this.responseObject.actions.add(action);
            return this;
        }

        public ActionFailureResponse build() {
            ActionFailureResponse actionFailureResponse = this.responseObject;
            this.reset();
            return actionFailureResponse;
        }
    }

    public static class OpenInAction {
        @JsonProperty("label")
        private String label;

        @JsonProperty("primary")
        private Boolean primary;

        @JsonProperty("remove_card_on_completion")
        private Boolean removeCardOnCompletion;

        @JsonProperty("url")
        private Map<String, String> url;

        // Don't instantiate directly, use the builder instead.
        private OpenInAction() {

        }

        public String getLabel() {
            return label;
        }

        public Boolean isPrimary() {
            return primary;
        }

        public Boolean shouldRemoveCardOnCompletion() {
            return removeCardOnCompletion;
        }

        public Map<String, String> getUrl() {
            return url;
        }

        public static class Builder {
            private OpenInAction action;

            // Create a builder instance
            public Builder() {
                action = new OpenInAction();
            }

            public void reset() {
                this.action = new OpenInAction();
            }

            public OpenInAction.Builder setLabel(String label) {
                this.action.label = label;
                return this;
            }

            public OpenInAction.Builder setPrimary(boolean primary) {
                this.action.primary = primary;
                return this;
            }

            public OpenInAction.Builder setRemoveCardOnCompletion(boolean removeCardOnCompletion) {
                this.action.removeCardOnCompletion = removeCardOnCompletion;
                return this;
            }

            public OpenInAction.Builder setUrl(String url) {
                this.action.url = Map.of("href", url);
                return this;
            }

            public OpenInAction build() {
                OpenInAction openInAction = action;
                reset();
                return openInAction;
            }
        }
    }
}
