package com.vmware.connectors.coupa.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.coupa.domain.ApprovalDetails;
import com.vmware.connectors.coupa.domain.RequisitionDetails;
import com.vmware.connectors.coupa.domain.UserDetails;
import com.vmware.connectors.coupa.util.HubCoupaUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class HubCoupaService {

	private static final Logger logger = LoggerFactory.getLogger(HubCoupaService.class);

	private final String systemToken;

	private final WebClient rest;
	private final CardTextAccessor cardTextAccessor;

	@Value("${coupa.url.webservice.userdetails}")
	private String userUrl;

	@Value("${coupa.url.webservice.pendingapprovals}")
	private String pendingApprovalUrl;

	@Value("${coupa.url.webservice.requisitions}")
	private String requistionUrl;

	@Value("${coupa.url.webservice.action}")
	private String actionUrl;

	@Autowired
	public HubCoupaService(WebClient rest, CardTextAccessor cardTextAccessor,

			@Value("${coupa.oauth-token}") final String systemToken) {
		this.rest = rest;
		this.cardTextAccessor = cardTextAccessor;
		this.systemToken = systemToken;
	}

	public Mono<Cards> getPendingApprovals(String user, String baseUrl, String routingPrefix,
			HttpServletRequest request, Locale locale) {

		logger.info("Getting user id of {}", user);
		String url = baseUrl + userUrl + user;

		return rest.get().uri(url).accept(MediaType.APPLICATION_JSON)
				.header(HubCoupaUtil.AUTHORIZATION_HEADER_NAME, systemToken).retrieve().bodyToFlux(UserDetails.class)
				.flatMap(u -> getApprovalDetails(systemToken, baseUrl, u.getId()))
				.map(req -> makeCards(routingPrefix, locale, req, request)).reduce(new Cards(), this::addCard);

	}

	private Flux<RequisitionDetails> getApprovalDetails(String auth, String baseUrl, String userId) {

		logger.info("Getting approval details for the user id :: {}", userId);
		String url = baseUrl + pendingApprovalUrl + userId + "&status=pending_approval";
		logger.info("Pending approvals url :: {}", url);

		return rest.get().uri(url).accept(MediaType.APPLICATION_JSON)
				.header(HubCoupaUtil.AUTHORIZATION_HEADER_NAME, auth).retrieve().bodyToFlux(ApprovalDetails.class)
				.flatMap(ad -> getRequisitionDetails(auth, baseUrl, ad));
	}

	private Flux<RequisitionDetails> getRequisitionDetails(String auth, String baseUrl, ApprovalDetails appr) {

		String url = baseUrl + requistionUrl + appr.getApprovableId() + "&status=pending_approval";
		logger.info("Requisition details Url :: {}", url);

		return rest.get().uri(url).accept(MediaType.APPLICATION_JSON)
				.header(HubCoupaUtil.AUTHORIZATION_HEADER_NAME, auth).retrieve().bodyToFlux(RequisitionDetails.class)
				.filter(requisition -> appr.getApprover().getId()
						.equals(requisition.getCurrentApproval().getApprover().getId()));

	}

	private Cards addCard(Cards cards, Card card) {
		cards.getCards().add(card);
		return cards;
	}

	/**
	 * Function to build the card response
	 * 
	 * @param routingPrefix
	 * @param locale
	 * @param expResponse
	 * @param request
	 * @return
	 */
	private Card makeCards(String routingPrefix, Locale locale, RequisitionDetails requestDetails,
			HttpServletRequest request) {

		String requestId = requestDetails.getCurrentApproval().getId();
		String reportName = requestDetails.getRequisitionLinesList().get(0).getDescription();
		String projectNumber = "1000"; // definition?

		logger.debug("makeCard called: routingPrefix={}, requestId={}, reportName={}", routingPrefix, requestId,
				reportName);

		Card.Builder builder = new Card.Builder().setName("Coupa")
				.setHeader(cardTextAccessor.getMessage("hub.coupa.header", locale, reportName), projectNumber)
				.setBody(new CardBody.Builder().addField(makeOrderDateField(locale, requestDetails))
						.addField(makeCostCenterField(locale, requestDetails))
						.addField(makeRequisitionNumberField(locale, requestDetails))
						.addField(makeRequisitionDescriptionField(locale, requestDetails))
						.addField(makeRequesterField(locale, requestDetails))
						.addField(makeTotalAmountField(locale, requestDetails))
						.addField(makeJustificationField(locale, requestDetails)).build())
				.addAction(makeApproveAction(locale, requestId, request))
				.addAction(makeDeclineAction(locale, requestId, request));

		CommonUtils.buildConnectorImageUrl(builder, request);

		return builder.build();
	}

	private CardAction makeApproveAction(Locale locale, String requestId, HttpServletRequest request) {
		return new CardAction.Builder().setActionKey(CardActionKey.USER_INPUT)
				.setLabel(cardTextAccessor.getMessage("hub.coupa.approve.label", locale))
				.setCompletedLabel(cardTextAccessor.getMessage("hub.coupa.approve.completedLabel", locale))
				.setPrimary(true).setMutuallyExclusiveSetId("approval-actions").setType(HttpMethod.POST)
				.setUrl(CommonUtils.buildConnectorUrl(request, null) + "/api/approve/" + requestId)
				.addUserInputField(new CardActionInputField.Builder().setFormat("textarea")
						.setId(HubCoupaUtil.COMMENT_KEY)
						.setLabel(cardTextAccessor.getMessage("hub.coupa.approve.comment.label", locale)).build())
				.build();
	}

	private CardAction makeDeclineAction(Locale locale, String requestId, HttpServletRequest request) {
		return new CardAction.Builder().setActionKey(CardActionKey.USER_INPUT)
				.setLabel(cardTextAccessor.getMessage("hub.coupa.decline.label", locale))
				.setCompletedLabel(cardTextAccessor.getMessage("hub.coupa.decline.completedLabel", locale))
				.setPrimary(false).setMutuallyExclusiveSetId("approval-actions").setType(HttpMethod.POST)
				.setUrl(CommonUtils.buildConnectorUrl(request, null) + "/api/decline/" + requestId)
				.addUserInputField(new CardActionInputField.Builder().setFormat("textarea")
						.setId(HubCoupaUtil.COMMENT_KEY)
						.setLabel(cardTextAccessor.getMessage("hub.coupa.decline.reason.label", locale)).build())
				.build();
	}

	private CardBodyField makeOrderDateField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.submissionDate", locale))
				.setDescription(requestDetails.getSubmittedAt()).build();
	}

	private CardBodyField makeCostCenterField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.costCenter", locale))
				.setDescription(requestDetails.getRequestorCostCenter()).build();
	}

	private CardBodyField makeRequisitionNumberField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.requestId", locale))
				.setDescription(requestDetails.getId()).build();
	}

	private CardBodyField makeRequisitionDescriptionField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.requestDescription", locale))
				.setDescription(requestDetails.getRequisitionDescription()).build();
	}

	private CardBodyField makeRequesterField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.requester", locale))
				.setDescription(HubCoupaUtil.getRequestorName(requestDetails)).build();
	}

	private CardBodyField makeTotalAmountField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.expenseAmount", locale))
				.setDescription(HubCoupaUtil.getFormattedAmount(requestDetails.getMobileTotal())).build();
	}

	private CardBodyField makeJustificationField(Locale locale, RequisitionDetails requestDetails) {
		return new CardBodyField.Builder().setType(CardBodyFieldType.GENERAL)
				.setTitle(cardTextAccessor.getMessage("hub.coupa.justification", locale))
				.setDescription(requestDetails.getJustification()).build();
	}

	public Mono<String> makeCoupaRequest(String reason, String baseUrl, String action, String id) throws IOException {

		String requestUrl = null;
		if (action.equalsIgnoreCase(HubCoupaUtil.APPROVE)) {
			requestUrl = baseUrl + actionUrl + id + "/approve?reason=" + reason;
		} else {
			requestUrl = baseUrl + actionUrl + id + "/reject?reason=" + reason;
		}

		logger.info("Request Url : {} ", requestUrl);

		return rest.put().uri(requestUrl).header(HubCoupaUtil.AUTHORIZATION_HEADER_NAME, systemToken)
				.accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class)

				.onErrorMap(WebClientResponseException.class, e -> handleForbiddenError(e));

	}

	private Throwable handleForbiddenError(WebClientResponseException e) {

		logger.info("Exception caught");
		if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
			return new WebClientResponseException(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), e.getStatusText(),
					e.getHeaders(), e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
		}

		return e;
	}

}
