package no.nav.bilag.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.auth.OauthService;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static java.util.UUID.randomUUID;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
@Slf4j
public class MDCInterceptor implements HandlerInterceptor {

	private static final String USER_ID = "UserId";
	public static final String MDC_CALL_ID = "callId";
	public static final String MDC_USER_ID = "userId";

	private final OauthService oauthService;

	public MDCInterceptor(OauthService oauthService) {
		this.oauthService = oauthService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String callId = request.getHeader(MDC_CALL_ID);
		if (isEmpty(callId)) {
			callId = randomUUID().toString();
		}
		MDC.put(MDC_CALL_ID, callId);
		response.addHeader(MDC_CALL_ID, callId);

		HttpSession session = request.getSession();
		if (session.getAttribute(USER_ID) == null) {
			oauthService.getJwtClaimsSet(session)
					.ifPresent(claims -> session.setAttribute(USER_ID, claims.getNavIdent()));
		}

		if (session.getAttribute(USER_ID) != null) {
			MDC.put(MDC_USER_ID, (String) session.getAttribute(USER_ID));
		} else {
			MDC.put(MDC_USER_ID, "Ikke innlogget");
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		MDC.clear();
	}

	public static String getCallId() {
		final String callId = MDC.get(MDC_CALL_ID);
		return isBlank(callId) ? randomUUID().toString() : callId;
	}

	public static String getUserId() {
		return MDC.get(MDC_USER_ID);
	}

}
