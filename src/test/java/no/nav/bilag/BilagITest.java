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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableWireMock
@ActiveProfiles("itest")
public class BilagITest {

	private static final String BREVSERVER_HENTDOKUMENT_ENDEPUNKT = "/brevweb/rest/hentdokument/%s";
	private static final String AZURE_ENDEPUNKT = "/azure/token";
	private static final String ACCESS_TOKEN_RESPONSE_BODY = """
			{
			  "access_token":"dummy-token",
			  "token_type":"Bearer",
			  "expires_in":3600,
			  "refresh_token":"dummy-refresh-token",
			  "example_parameter":"example_value"
			}
			""";

	@Autowired
	public WebTestClient webTestClient;

	@MockitoBean
	public OauthService oauthService; // Mock av oauthService for å unngå loginFilter

	@BeforeEach
	void setUp() {
		stubAzureObo();
	}

	@Test
	void skalReturnereTemporaryDirectVedManglendeAccessToken() {
		webTestClient.get()
				.uri("/hent/123")
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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
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
