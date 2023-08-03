package no.nav.bilag.auth;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import no.nav.bilag.BilagProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static com.nimbusds.oauth2.sdk.ResponseType.CODE;
import static com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod.S256;
import static no.nav.bilag.auth.OauthController.OAUTH_CALLBACK_PATH;

@Slf4j
@Service
public class OauthService {

	public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
	private static final String LOGIN_NONCE = "LOGIN_NONCE";
	private static final String LOGIN_STATE = "LOGIN_STATE";

	private final BilagProperties bilagProperties;
	private final AzureProperties azureProperties;

	public OauthService(BilagProperties bilagProperties,
						AzureProperties azureProperties) {
		this.bilagProperties = bilagProperties;
		this.azureProperties = azureProperties;
	}

	public Optional<AccessToken> getOAuth2AuthorizationFromSession(HttpSession session) {
		String rawAccessToken = (String) session.getAttribute(ACCESS_TOKEN);
		if (rawAccessToken == null) {
			return Optional.empty();
		}

		try {
			var accessToken = BearerAccessToken.parse(new JSONObject(JSONObjectUtils.parse(rawAccessToken)));

			if (validateAccessToken(accessToken)) {
				return Optional.of(accessToken);
			} else {
				return refreshAccessToken(session);
			}
		} catch (java.text.ParseException | ParseException e) {
			log.error("Encountered exception when validating and parsing AccessToken", e);
			return Optional.empty();
		}
	}

	private Optional<AccessToken> refreshAccessToken(HttpSession session) {
		String refreshToken = (String) session.getAttribute(REFRESH_TOKEN);
		if (refreshToken == null) {
			return Optional.empty();
		}

		TokenRequest tokenRequest = new TokenRequest(
				URI.create(azureProperties.openidConfigTokenEndpoint()),
				new ClientSecretBasic(new ClientID(), new Secret()),
				new RefreshTokenGrant(new RefreshToken(refreshToken))
		);

		try {
			TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

			if (!tokenResponse.indicatesSuccess()) {
				var tokenErrorResponse = tokenResponse.toErrorResponse();
				log.error("Acquiring a new access-token from existing refresh-token failed: {}", tokenErrorResponse.getErrorObject().getDescription());
				return Optional.empty();
			}

			AccessTokenResponse accessTokenResponse = tokenResponse.toSuccessResponse();

			AccessToken newAccessToken = accessTokenResponse.getTokens().getAccessToken();
			session.setAttribute(ACCESS_TOKEN, newAccessToken.toJSONString());

			return Optional.of(newAccessToken);
		} catch (IOException | ParseException e) {
			return Optional.empty();
		}
	}

	private boolean validateAccessToken(AccessToken accesstoken) {
		try {
			SignedJWT jwt = SignedJWT.parse(accesstoken.getValue());
			return jwt.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now()));
		} catch (java.text.ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public Optional<NavJwtClaimSet> getJwtClaimsSet(HttpSession session) {
		return getOAuth2AuthorizationFromSession(session)
				.map(AccessToken::getValue)
				.map(s -> {
					try {
						return SignedJWT.parse(s).getJWTClaimsSet();
					} catch (java.text.ParseException e) {
						return null;
					}
				})
				.map(NavJwtClaimSet::new);
	}

	URI createAuthorizationUri(HttpSession httpSession) {
		ClientID clientID = new ClientID(azureProperties.appClientId());
		CodeVerifier codeVerifier = new CodeVerifier();
		Scope scope = new Scope(bilagProperties.getAzureScope());
		State state = new State();
		URI redirectEndpoint = URI.create(bilagProperties.getBaseUrl() + OAUTH_CALLBACK_PATH);
		URI oauthEndpoint = URI.create(azureProperties.getLoginEndpoint());

		httpSession.setAttribute(LOGIN_STATE, state.getValue());
		httpSession.setAttribute(LOGIN_NONCE, codeVerifier.getValue());

		return new AuthorizationRequest.Builder(CODE, clientID)
				.scope(scope)
				.state(state)
				.redirectionURI(redirectEndpoint)
				.endpointURI(oauthEndpoint)
				.codeChallenge(codeVerifier, S256)
				.build()
				.toURI();
	}

	AuthorizationGrant handleAuthorizationCallback(HttpServletRequest incomingRequest) throws UserAuthorizationException {
		var requestWithQuery = incomingRequest.getRequestURI() + "?" + incomingRequest.getQueryString();
		var session = incomingRequest.getSession();

		try {
			AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(URI.create(requestWithQuery));

			String originalNonce = (String) session.getAttribute(LOGIN_NONCE);
			CodeVerifier codeVerifier = new CodeVerifier(originalNonce);

			String originalState = (String) session.getAttribute(LOGIN_STATE);
			session.removeAttribute(LOGIN_STATE);
			State state = new State(originalState);
			if (!state.equals(authorizationResponse.getState())) {
				throw new UserAuthorizationException("Nonce does not match for session");
			}

			if (!authorizationResponse.indicatesSuccess()) {
				var tokenErrorResponse = authorizationResponse.toErrorResponse();
				throw new UserAuthorizationException(tokenErrorResponse.getErrorObject().getDescription());
			}

			AuthorizationSuccessResponse authorizationSuccessResponse = authorizationResponse.toSuccessResponse();
			URI redirectEndpoint = URI.create(bilagProperties.getBaseUrl() + OAUTH_CALLBACK_PATH);

			return new AuthorizationCodeGrant(authorizationSuccessResponse.getAuthorizationCode(), redirectEndpoint, codeVerifier);
		} catch (ParseException e) {
			throw new UserAuthorizationException("Error parsing token", e);
		}
	}

	void getTokensFromAuthorizationGrant(HttpSession session, AuthorizationGrant authorizationGrant) throws TokenAcquisitionException {
		TokenRequest tokenRequest = new TokenRequest(
				URI.create(azureProperties.openidConfigTokenEndpoint()),
				new ClientSecretBasic(new ClientID(azureProperties.appClientId()), new Secret(azureProperties.appClientSecret())),
				authorizationGrant,
				new Scope(bilagProperties.getAzureScope())
		);
		HTTPRequest httpRequest = tokenRequest.toHTTPRequest();

		try {
			HTTPResponse httpResponse = httpRequest.send();
			TokenResponse tokenResponse = TokenResponse.parse(httpResponse);

			if (!tokenResponse.indicatesSuccess()) {
				var tokenErrorResponse = tokenResponse.toErrorResponse();
				throw new TokenAcquisitionException(tokenErrorResponse.getErrorObject().getDescription());
			}

			AccessTokenResponse accessTokenResponse = tokenResponse.toSuccessResponse();

			Optional.ofNullable(accessTokenResponse.getTokens().getAccessToken())
					.map(Identifier::toJSONString)
					.ifPresent(token -> session.setAttribute(ACCESS_TOKEN, token));
			Optional.ofNullable(accessTokenResponse.getTokens().getRefreshToken())
					.map(Identifier::toJSONString)
					.ifPresent(token -> session.setAttribute(REFRESH_TOKEN, token));
		} catch (IOException | ParseException e) {
			throw new TokenAcquisitionException("Technical error when attempting to acquire tokens", e);
		}
	}
}
