package com.vmware.connectors.coupa.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;

public final class HubCoupaUtil {

	public static final String BEARER = "Bearer ";

	public static final String COMMENT_KEY = "comment";
	public static final String REASON_KEY = "reason";

	public static final String APPROVE = "APPROVE";
	public static final String REJECT = "REJECT";

	public static final String AUTHORIZATION_HEADER_NAME = "X-COUPA-API-KEY";

	private HubCoupaUtil() {
	}

	public static String parseUsernameFromPrincipal(String principal) {
		return StringUtils.substringBeforeLast(principal, "@");

	}

	public static String getRequestorName(
			com.vmware.connectors.coupa.domain.RequisitionDetails requisitionDetailsClientResponse) {
		String requestorName = "";
		if (requisitionDetailsClientResponse.getRequestedBy() != null
				&& StringUtils.isNotEmpty(requisitionDetailsClientResponse.getRequestedBy().getFirstName())) {
			requestorName = requisitionDetailsClientResponse.getRequestedBy().getFirstName() + " "
					+ requisitionDetailsClientResponse.getRequestedBy().getLastName();
		}
		return requestorName;
	}

	public static String getFormattedAmount(String amount) {
		String a = "";
		if (amount != null) {
			BigDecimal amt = new BigDecimal(amount);
			DecimalFormat formatter = new DecimalFormat("#,###.00");

			a = formatter.format(amt);
		}

		return a;
	}

}
