package no.nav.bilag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class BilagITest {

	private static final String HENTDOKUMENT_URL = "/rest/hentdokument/";

	@Autowired
	public WebTestClient webTestClient;


	@Test
	void skalHenteDokument() {
		long dokId = 123L;

		var response = webTestClient.get()
				.uri(HENTDOKUMENT_URL + dokId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();

		assertThat(response).isEqualTo("success");
	}

	@ParameterizedTest
	@ValueSource(longs = {-1, 0})
	void skalReturnereBadRequestForUgyldigDokId(Long dokId) {
		webTestClient.get()
				.uri(HENTDOKUMENT_URL + dokId)
				.exchange()
				.expectStatus().isBadRequest();
	}
}
