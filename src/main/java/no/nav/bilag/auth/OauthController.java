package no.nav.bilag.auth;

import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.exceptions.TokenAcquisitionException;
import no.nav.bilag.exceptions.UserAuthorizationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static no.nav.bilag.auth.LoginFilter.ORIGINAL_URI;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TEMPORARY_REDIRECT;

@Slf4j
@RestController
public class OauthController {

	public static final String OAUTH_CALLBACK_PATH = "/oauth2/callback";
	private static final String ME_PATH = "/oauth2/me";

	private final OauthService oauthService;

	public OauthController(OauthService oauthService) {
		this.oauthService = oauthService;
	}

	@GetMapping(path = ME_PATH)
	public ResponseEntity<String> whoami(HttpSession session) {
		return oauthService.getJwtClaimsSet(session)
				.map(jwtClaimsSet -> "{" +
									 "\"NAVident\":\"" + jwtClaimsSet.getNavIdent() + "\"," +
									 "\"name\":\"" + jwtClaimsSet.getName() + "\"" +
									 "}")
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.ok("{}"));
	}

	@GetMapping(path = OAUTH_CALLBACK_PATH)
	public ResponseEntity<String> handleOauthCallback(HttpServletRequest incomingRequest) {
		try {
			AuthorizationGrant authorizationGrant = oauthService.handleAuthorizationCallback(incomingRequest);

			var session = incomingRequest.getSession();
			oauthService.getTokensFromAuthorizationGrant(session, authorizationGrant);

			return ResponseEntity
					.status(TEMPORARY_REDIRECT)
					.location(URI.create((String) session.getAttribute(ORIGINAL_URI)))
					.build();
		} catch (UserAuthorizationException e) {
			log.error("Something went wrong when authenticating user with Microsoft: {}", e.getMessage());
			return ResponseEntity.status(BAD_REQUEST).build();
		} catch (TokenAcquisitionException e) {
			log.error("Something went wrong when acquiring access-token for authenticated user: {}", e.getMessage());
			return ResponseEntity.status(BAD_GATEWAY).build();
		}
	}
}
