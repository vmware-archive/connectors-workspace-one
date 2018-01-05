/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.socialcast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.vmware.connectors.common.model.Message;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.model.UserRecord;
import com.vmware.connectors.common.utils.Async;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.HttpServerErrorException;
import rx.Observable;
import rx.Single;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This class encapsulates interactions with the Socialcast API.
 * <p>
 * 1. Perform a search to see which participants in the email thread are already Socialcast users:
 * <pre>
 *
 * // Join multiple addresses with " or "?
 * GET /api/users/search.json?q=&lt;<i>email address</i>[ or <i>email address</i>]&gt;
 * </pre>
 * <p>
 * 2. Create a new Group:
 * <pre>
 * POST /api/groups.json
 * {
 * "group": {
 * "name": &lt;<i>subject line of first email in the thread</i>&gt;,
 * "permission_mode": "external_contributor"
 * }
 * }
 * </pre>
 * <p>
 * 3. Add all senders/recipients to the Group:
 * <pre>
 * POST /api/groups/&lt;<i>group_id</i>&gt;/memberships/add_members.json
 * {
 * "group_memberships": [
 * {
 * // For existing Socialcast users (i.e. ones we found an ID for), use their user ID:
 * "user_id": &lt;<i>id of user</i>&gt;,
 * "role": &lt;<i>"admin" for the sender of the first message in the thread, "member" otherwise</i>&gt;
 * },
 * {
 * // For users without an ID, use their email address, and trigger an invitation to be sent by Socialcast
 * "email": &lt;<i>id of user</i>&gt;,
 * "role": "member",
 * "invite": "true"
 * },
 * &lt;<i>one block for each sender/recipient</i>&gt;
 * ]
 * }
 * </pre>
 * <p>
 * 4. Post each email to the Group as a new message:
 * <pre>
 * POST /api/messages.json
 * {
 * "message": {
 * "user": &lt;<i>user ID of sender of this email</i>&gt;,
 * "body": &lt;<i>message body, not including quoted replies</i>&gt;,
 * "attachment": &lt;<i>attachment, if any, as inline base64</i>&gt;
 * }
 * }
 * </pre>
 */
@RestController
@SuppressWarnings("PMD.UseConcurrentHashMap")
public class SocialcastController {

    // The name of the incoming request header carrying our Socialcast authorization token
    private static final String SOCIALCAST_AUTH_HEADER = "x-socialcast-authorization";

    // The name of the incoming request header carrying the base URL of the user's Socialcast server
    private static final String SOCIALCAST_BASE_URL_HEADER = "x-socialcast-base-url";

    // The URL path on which this app listens for incoming requests
    private static final String CREATE_CONVERSATION_PATH = "/conversations";

    // A sink for log messages, because System.err.println() is *so* 1990...
    private final static Logger logger = LoggerFactory.getLogger(SocialcastController.class);

    // Our engine for making asynchronous outgoing HTTP requests
    private final AsyncRestOperations rest;

    private final SocialcastMessageFormatter formatter;

    @Autowired
    public SocialcastController(AsyncRestOperations rest, SocialcastMessageFormatter formatter) {
        this.rest = rest;
        this.formatter = formatter;
    }

    // This is the entry point for requests to post an email thread as a new Socialcast group.
    // The expected body is a JSON document containing the relevant data from all emails in the thread;
    // see "src/test/resources/requests/normalRequest.json" for an example of the expected format.
    // TODO: convert to the schema.org EmailMessage schema
    @PostMapping(path = CREATE_CONVERSATION_PATH, consumes = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<String>> postThreadAsConversation(
            @RequestHeader(name = SOCIALCAST_AUTH_HEADER) String scAuth,
            @RequestHeader(name = SOCIALCAST_BASE_URL_HEADER) String baseUrl,
            @RequestBody String json) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, scAuth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        SocialcastRequestContext ctx = new SocialcastRequestContext(baseUrl, headers);

        // Parse the request body, which should be JSON representing a MessageThread
        ctx.setMessageThread(MessageThread.parse(json));

        // Get emails of all the participants (senders and receivers) in the thread,
        // and look for their Socialcast IDs to see if they're already Socialcast Users
        Single<HttpStatus> userRecordsStep = getExistingUserIds(ctx);

        // Create a new Group for the email thread
        Single<HttpStatus> groupIdStep = createGroup(ctx);

        // Add all participants to the Group, inviting those who are not yet Socialcast Users,
        // and then post each email in the thread, in order, as a Message in the Group
        return Single.zip(userRecordsStep, groupIdStep, (httpStatus, httpStatus2) -> null)
                .flatMap(stat -> addUsersToGroup(ctx))
                .flatMap(stat -> postMessages(ctx))
                .map(resp -> respondWithSummary(resp, ctx));
    }


    //////////////////////
    // Step 1: get IDs of existing users
    // Socialcast API doc: https://socialcast.github.io/socialcast/apidoc/users/search.html
    //////////////////////

    // Check if an email maps to a registered user
    private Single<HttpStatus> getExistingUserIds(SocialcastRequestContext requestContext) {

        MessageThread messageThread = requestContext.getMessageThread();

        // It's not documented in the Socialcast API docs, but one query can search for multiple
        // email addresses if they are concatenated with " or "
        String queryString = messageThread.allUsers().stream()
                .map(UserRecord::getEmailAddress)
                .collect(Collectors.joining(" or "));

        queryString = requestContext.getScBaseUrl() + "/api/users/search.json?q=" + queryString;

        ListenableFuture<ResponseEntity<String>> future =
                rest.exchange(queryString, HttpMethod.GET, new HttpEntity(requestContext.getHeaders()), String.class);

        return Async.toSingle(future)
                .map(result -> parseGetUserResponse(result, requestContext));
    }


    // Parse the response from the user-search query and update UserRecords for those users
    // who are found to have Socialicast user ID's
    private HttpStatus parseGetUserResponse(ResponseEntity<String> result, SocialcastRequestContext ctx) {

        setScastId(ctx.getMessageThread().allUsersByEmail(), result);

        addUserFoundStatus(ctx);

        ctx.addResponseCodeForStep("User query", result.getStatusCode().toString());

        return result.getStatusCode();
    }

    private void setScastId(Map<String, UserRecord> userRecordMap, ResponseEntity<String> result) {
        for (Object queryResult : JsonPath.parse(result.getBody()).read("$.users", List.class)) {

            ReadContext userReadContext = JsonPath.parse(queryResult);
            String addr = userReadContext.read("$.contact_info.email");
            String castId = userReadContext.read("$.id", String.class);

            if (StringUtils.isNotBlank(castId)) {
                UserRecord rec = userRecordMap.get(addr);
                if (rec != null) {
                    rec.setScastId(castId);
                }
            }
        }
    }

    private void addUserFoundStatus(SocialcastRequestContext ctx) {
        for (UserRecord user : ctx.getMessageThread().allUsers()) {
            final String scastId = user.getScastId();

            if (scastId == null) {
                logger.debug("Found no Socialcast ID for email <<{}>>", user.getEmailAddress());
                ctx.addUserNotFound(user.getEmailAddress());

            } else {
                logger.debug("Found Socialcast ID <<{}>> for email <<{}>>", scastId, user.getEmailAddress());
                ctx.addUserFound(user.getEmailAddress(), scastId);
            }
        }
    }

    //////////////////////
    // Step 2: create group
    // Socialcast API doc: https://socialcast.github.io/socialcast/apidoc/groups/create.html
    //////////////////////

    private Single<HttpStatus> createGroup(SocialcastRequestContext ctx) {

        final Map<String, String> groupMap = new HashMap<>();
        groupMap.put("name", formatter.makeGroupName(ctx.getMessageThread()));
        groupMap.put("description", formatter.makeGroupDescription(ctx.getMessageThread()));

        final Map<String, Map<String, String>> bodyMap = Collections.singletonMap("group", groupMap);
        final ListenableFuture<ResponseEntity<String>> future =
                rest.exchange(ctx.getScBaseUrl() + "/api/groups.json",
                        HttpMethod.POST, new HttpEntity<>(bodyMap, ctx.getHeaders()), String.class);

        return Async.toSingle(future)
                .map(entity -> parseGroupCreationResult(entity, ctx));
    }

    // Get the ID and URI of the just-created Group and add them to the request context
    private HttpStatus parseGroupCreationResult(ResponseEntity<String> entity, SocialcastRequestContext ctx) {
        ReadContext jsonContext = JsonPath.parse(entity.getBody());
        String groupId = jsonContext.read("$.group.id").toString();
        String groupUri = jsonContext.read("$.group.url").toString();

        ctx.addResponseCodeForStep("Group creation", entity.getStatusCode().toString());

        if (StringUtils.isBlank(groupId) || StringUtils.isEmpty(groupUri)) {
            throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY);
        } else {
            ctx.setGroupUri(groupUri);
            ctx.setGroupId(groupId);
        }

        return entity.getStatusCode();
    }


    //////////////////////
    // Step 3: add users to group
    // Socialcast API doc: https://socialcast.github.io/socialcast/apidoc/group_memberships/add_members.html
    //////////////////////

    private Single<HttpStatus> addUsersToGroup(SocialcastRequestContext ctx) {
        List<Map<String, String>> memberships = new ArrayList<>();

        for (UserRecord user : ctx.getMessageThread().allUsers()) {
            memberships.add(getMembership(user, ctx));
        }

        Map<String, Object> bodyMap = Collections.singletonMap("group_memberships", memberships);

        ListenableFuture<ResponseEntity<String>> future =
                rest.exchange(ctx.getScBaseUrl() + "/api/groups/" + ctx.getGroupId() + "/memberships/add_members.json",
                        HttpMethod.POST, new HttpEntity<>(bodyMap, ctx.getHeaders()), String.class);

        return Async.toSingle(future)
                .map(entity -> parseUserAdditionResponse(entity, ctx));
    }

    private Map<String, String> getMembership(final UserRecord user, final SocialcastRequestContext requestContext) {
        final Map<String, String> userMap = new HashMap<>();

        if (StringUtils.isNotBlank(user.getScastId())) {
            // If we have an ID for a user, they're already on Socialcast, so we just have to add them to the group
            final String role = user.equals(requestContext.getMessageThread().getFirstSender()) ? "admin" : "member";
            userMap.put("user_id", user.getScastId());
            userMap.put("role", role);

        } else {
            // If they don't have an ID, we tell Socialcast to invite them
            userMap.put("email", user.getEmailAddress());
            userMap.put("invite", "true");
            userMap.put("role", "member");
        }

        return userMap;
    }

    // Write status report to the request context
    private HttpStatus parseUserAdditionResponse(ResponseEntity<String> entity, SocialcastRequestContext ctx) {
        HttpStatus status = entity.getStatusCode();
        ctx.addResponseCodeForStep("Adding users", status.toString());
        return status;
    }


    //////////////////////
    // Step 4: post messages
    // Socialcast API doc: https://socialcast.github.io/socialcast/apidoc/messages/create.html
    //////////////////////

    private Single<ResponseEntity<Void>> postMessages(SocialcastRequestContext ctx) {

        List<Observable<HttpStatus>> postRequests =
                ctx.getMessageThread().getMessages().stream()
                        .map(msg -> postMessage(msg, ctx))
                        .collect(Collectors.toList());

        Observable<HttpStatus> concatenatedRequests = Observable.empty();
        for (Observable<HttpStatus> req : postRequests) {
            concatenatedRequests = concatenatedRequests.concatWith(req);
        }

        return concatenatedRequests
                .all(httpStatus -> httpStatus == HttpStatus.CREATED)
                .map(success -> success ? new ResponseEntity<Void>(HttpStatus.CREATED)
                        : new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR))
                .toSingle();
    }

    // Post a single message
    private Observable<HttpStatus> postMessage(final Message message, final SocialcastRequestContext ctx) {
        final Map<String, String> msgMap = new HashMap<>();
        msgMap.put("user", message.getSender().getScastId());
        msgMap.put("group_id", ctx.getGroupId());
        msgMap.put("body", formatter.formatMessageForDisplay(message));

        // TODO: add attachment here
        final Map<String, Object> bodyMap = Collections.singletonMap("message", msgMap);

        return Single.defer(() -> Async.toSingle(
                rest.exchange(ctx.getScBaseUrl() + "/api/messages.json", HttpMethod.POST,
                        new HttpEntity<>(bodyMap, ctx.getHeaders()), String.class))
                .map(entity -> reportStatus(entity, ctx, message)))
                .toObservable();
    }

    // Write status and message permalink URL to the request context
    private HttpStatus reportStatus(ResponseEntity<String> entity, SocialcastRequestContext ctx, Message message) {
        HttpStatus status = entity.getStatusCode();
        ctx.addResponseCodeForStep("Posting message " + message.getId(), status.toString());

        String msgUri = JsonPath.parse(entity.getBody()).read("$.message.permalink_url");
        ctx.addMessagePosted(message.getId(), msgUri);

        return status;
    }


    private ResponseEntity<String> respondWithSummary(ResponseEntity<Void> messagesResponse, SocialcastRequestContext ctx) {
        try {
            return new ResponseEntity<>(ctx.getResultJson(), messagesResponse.getHeaders(), messagesResponse.getStatusCode());
        } catch (JsonProcessingException e) {
            logger.error("Exception serializing JSON response data", e);
            return new ResponseEntity<>("Exception serializing JSON response data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
