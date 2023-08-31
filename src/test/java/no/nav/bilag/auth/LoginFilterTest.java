package no.nav.bilag.auth;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginFilterTest {


	private final String HENTDOKUMENT_PATH = "/hent/123";
	private final String CALLBACK_PATH = "/oauth2/callback";
	private final String READINESS_PATH = "/actuator/health/readiness";
	private final String LIVENESS_PATH = "/actuator/health/liveness";

	@Mock
	HttpServletRequest httpServletRequest;

	@Mock
	HttpServletResponse httpServletResponse;

	@Mock
	FilterChain filterChain;

	@Mock
	OauthService oauthService;

	@InjectMocks
	LoginFilter loginFilter;

	@Test
	void skalGiRedirect() throws ServletException, IOException {
		when(httpServletRequest.getRequestURI()).thenReturn(HENTDOKUMENT_PATH);
		when(oauthService.getOAuth2AuthorizationFromSession(any())).thenReturn(Optional.of(new BearerAccessToken()));

		loginFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(oauthService).getOAuth2AuthorizationFromSession(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {CALLBACK_PATH, READINESS_PATH, LIVENESS_PATH})
	void skalIkkeGiRedirectForSpesifikkeEndepunkt(String path) throws ServletException, IOException {
		when(httpServletRequest.getRequestURI()).thenReturn(path);

		loginFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verifyNoInteractions(oauthService);
	}
}
