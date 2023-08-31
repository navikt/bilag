package no.nav.bilag.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@AutoConfigureWebTestClient(timeout = "60000")
class LoginFilterITest {

	private static final String LIVENESS_URL = "/actuator/health/liveness";
	private static final String READINESS_URL = "/actuator/health/readiness";
	private static final String CALLBACK_URL = "/oauth2/callback";
	private static final String HENTDOKUMENT_URL = "/hentdokument/%s";

	private static final String READINESS_JSON = """
			{
			  "status":"UP"
			}
			""";

	@Autowired
	public WebTestClient webTestClient;

	@Test
	void skalGiRedirectForHentDokument() {
		var headers = webTestClient.get()
				.uri(format(HENTDOKUMENT_URL, "123"))
				.exchange()
				.expectStatus().isTemporaryRedirect()
				.returnResult(String.class)
				.getResponseHeaders();

		assertThat(headers.getLocation())
				.isNotNull()
				.asString()
				.contains("azure/authorize");
	}

	@Disabled("fungerer ikkje som den skal")
	@ParameterizedTest
	@ValueSource(strings = {READINESS_URL})
	void skalIkkeGiRedirectForSpesifikkeEndepunkt(String endepunkt) {
		webTestClient.get()
				.uri(endepunkt)
				.exchange()
				.expectStatus().isOk();
	}

}