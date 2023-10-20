package no.nav.bilag;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import net.minidev.json.JSONObject;
import no.nav.bilag.auth.OauthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class BilagITest {

	private static final String BREVSERVER_HENTDOKUMENT_ENDEPUNKT = "/brevweb/rest/hentdokument/%s";
	private static final String AZURE_ENDEPUNKT = "/azure/token";
	private static final String ACCESS_TOKEN_RESPONSE_BODY = """
			{
			  "access_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9.eyJhdWQiOiJiNTc5ZjM3OC1kMGZmLTRjZGUtOWVlYy0zZTNlODFlNzQ3NGUiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE2NzMyNzMxMzUsIm5iZiI6MTY3MzI3MzEzNSwiZXhwIjoxNjczMjc4MTE3LCJhaW8iOiJBVFFBeS84VEFBQUF1NU5EaXpKMDBSSS9zZkpiYUJod1VOSm5uQktzY2JoRGN5K204TWNqY1ExYVFYZXJyN3FvMU5sUGxuajVBb1lpIiwiYXpwIjoiYjU3OWYzNzgtZDBmZi00Y2RlLTllZWMtM2UzZTgxZTc0NzRlIiwiYXpwYWNyIjoiMSIsImdyb3VwcyI6WyJkZWMzZWU1MC1iNjgzLTQ2NDQtOTUwNy01MjBlOGYwNTRhYzIiXSwibmFtZSI6IkZfWjk5NDA1OSBFX1o5OTQwNTkiLCJvaWQiOiI3ZDA4YzczMS1mNGYwLTRhNzQtYTNjOS03NWRkYTZlMjg5NWEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJGX1o5OTQwNTkuRV9aOTk0MDU5QHRyeWdkZWV0YXRlbi5ubyIsInJoIjoiMC5BVWNBY3NWcWxyZjF2a3VxaU1ka0djRDRVWGp6ZWJYXzBONU1udXctUG9IblIwNUhBUDAuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6InpZR0xFbndvVFNfdmtRclBxY0VsaEJ3SkpBU3d4eFB0cllNSjEtX1h6UTgiLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiJNV1A1UW1pbklrYTZZNjFuSjFrM0FnIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJaOTk0MDU5IiwiYXpwX25hbWUiOiJkZXYtZnNzOnRlYW1kb2t1bWVudGhhbmR0ZXJpbmc6ZG9rbWV0In0.NhFn9sHpdprRl_3GNBQplQEQIZ4RvWC4oYQdQ_7Q0vTey9tE7pZaNW3kGLnZYqO-LeegZJ1AAM1ddwivLOivhomL5lNyzM3nQORy4vKuZ9UXLpb3L-RXqyVs2KW4mPvhNQ1xPmNzFGEm1jOmuBFcJDkP8wbwXMXTJtS53oBBqOLK7jrcv6qnS0TATMHMdm6oHA4rXZcUlGfX__se1D9PY4g90QHkmpt6BcQyYdXkp7R5h21BVSM6VZ2AMA0f3DuudllvcgB_RyoJ9Bc1QUiArHiDVjFsIumWUCGryUKyTLS9NFBM0tFSTuJP7G8KGidQafLa5s8ZXD1sWaK_yWzsbQ",
			  "token_type":"Bearer",
			  "expires_in":3600,
			  "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
			  "example_parameter":"example_value"
			}
			""";

	@Autowired
	public WebTestClient webTestClient;

	@MockBean
	public OauthService oauthService; // Mock av oauthService for å unngå loginFilter

	@BeforeEach
	void setUp() {
		stubAzureObo();
	}

	@Test
	void skalReturnereTemporaryDirectVedManglendeAccessToken() {
		long dokId = 123L;

		webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().isTemporaryRedirect();
	}

	@ParameterizedTest
	@ValueSource(strings = {"0123", "2345"})
	void skalHenteDokument(String dokId) {
		mockGetTokenFromSession();
		stubOkFraBrevserver(dokId);

		var response = webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(response).isEqualTo("bilagsdokument");
	}

	@Test
	void skalReturnereNotFoundHvisBrevserverIkkeFinnerDokumentet() {
		mockGetTokenFromSession();

		String dokId = "456";
		stubResponsFraBrevserver(NOT_FOUND.value(), dokId);

		var body = webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(body).contains("Fant ikke dokumentet");
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", "-1", "123a", "100000000000000000000000000000000"})
	void skalReturnereBadRequestForUgyldigDokId(String dokId) {
		mockGetTokenFromSession();

		var body = webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(body).contains("DokId er ikke en gyldig dokumentId");
	}

	@Test
	void skalReturnereFunksjonellFeilFor4xxStatuskoderUlik404FraBrevserver() {
		mockGetTokenFromSession();

		String dokId = "123";
		stubResponsFraBrevserver(PAYLOAD_TOO_LARGE.value(), dokId);

		var body = webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(body).contains("Funksjonell feil");
	}

	@Test
	void skalReturnereTekniskFeilForInternalServerErrorFraBrevserver() {
		mockGetTokenFromSession();

		String dokId = "123";
		stubResponsFraBrevserver(INTERNAL_SERVER_ERROR.value(), dokId);

		var body = webTestClient.get()
				.uri("/hent/" + dokId)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(body).contains("Teknisk feil");
	}

	private void mockGetTokenFromSession() {
		try {
			when(oauthService.getOAuth2AuthorizationFromSession(any()))
					.thenReturn(Optional.of(BearerAccessToken.parse(new JSONObject(JSONObjectUtils.parse(ACCESS_TOKEN_RESPONSE_BODY)))));
		} catch (ParseException | java.text.ParseException e) {
			fail("Klarte ikke parse stubbed token", e);
		}
	}

	private void stubAzureObo() {
		stubFor(post(urlMatching(AZURE_ENDEPUNKT))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(ACCESS_TOKEN_RESPONSE_BODY)));
	}

	private void stubOkFraBrevserver(String dokId) {
		stubFor(get(urlPathMatching(format(BREVSERVER_HENTDOKUMENT_ENDEPUNKT, dokId)))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody("bilagsdokument")));
	}

	private void stubResponsFraBrevserver(int httpStatus, String dokId) {
		stubFor(get(urlPathMatching(format(BREVSERVER_HENTDOKUMENT_ENDEPUNKT, dokId)))
				.willReturn(aResponse()
						.withStatus(httpStatus)));
	}
}
