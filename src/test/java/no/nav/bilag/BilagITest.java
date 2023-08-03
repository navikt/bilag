package no.nav.bilag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@Disabled
public class BilagITest {

	private static final String HENTDOKUMENT_URL = "/rest/hentdokument/";

	@Autowired
	public WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		stubBrevserver();
	}

	@Test
	void skalHenteDokument() {
		long dokId = 123L;

		webTestClient.get()
				.uri(HENTDOKUMENT_URL + dokId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();
	}

	@ParameterizedTest
	@ValueSource(longs = {-1, 0})
	void skalReturnereBadRequestForUgyldigDokId(Long dokId) {
		webTestClient.get()
				.uri(HENTDOKUMENT_URL + dokId)
				.exchange()
				.expectStatus().isBadRequest();
	}

	private void stubBrevserver() {
		stubFor(get(urlPathMatching("/brevweb/hentdokument"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody("anders")));
	}
}
