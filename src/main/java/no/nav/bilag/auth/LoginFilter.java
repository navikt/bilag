package no.nav.bilag.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.List;

import static jakarta.servlet.http.HttpServletResponse.SC_TEMPORARY_REDIRECT;

@Slf4j
@Component
public class LoginFilter extends GenericFilterBean {

	public static final String ORIGINAL_URI = "ORIGINAL_URI";

	private static final String CALLBACK_URI = "/oauth2/callback";
	private static final String READINESS_URI = "/actuator/health/readiness";
	private static final String LIVENESS_URI = "/actuator/health/liveness";
	private static final List<String> URIS_WITHOUT_REDIRECT = List.of(CALLBACK_URI, READINESS_URI, LIVENESS_URI);

	OauthService oauthService;

	public LoginFilter(OauthService oauthService) {
		this.oauthService = oauthService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest servletRequest = (HttpServletRequest) request;
		HttpServletResponse servletResponse = (HttpServletResponse) response;

		if (URIS_WITHOUT_REDIRECT.contains(servletRequest.getRequestURI())) {
			log.debug("Ingen redirect for uri=" + servletRequest.getRequestURI());
		} else {
			var accessToken = oauthService.getOAuth2AuthorizationFromSession(servletRequest.getSession());

			if (accessToken.isEmpty()) {
				servletRequest.getSession().setAttribute(ORIGINAL_URI, servletRequest.getRequestURI());

				String encodedRedirectUrl = ((HttpServletResponse) response).encodeRedirectURL(String.valueOf(oauthService.createAuthorizationUri(servletRequest.getSession())));
				servletResponse.setStatus(SC_TEMPORARY_REDIRECT);
				servletResponse.setHeader("Location", encodedRedirectUrl);

				return;
			}
		}

		chain.doFilter(servletRequest, servletResponse);
	}
}
