package no.nav.bilag;

import jakarta.servlet.ServletException;
import no.nav.bilag.auth.LoginFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@AutoConfigureWebTestClient
@Disabled // Denne testklassa er work in progress, og fungerer ikkje per no grunna innloggingsflyten
@ExtendWith(MockitoExtension.class)
public class BilagITest {

	private static final String HENTDOKUMENT_URL = "/rest/hentdokument/";
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
	LoginFilter loginFilter;

	@BeforeEach
	void setUp() throws ServletException, IOException {
		stubAzure();
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

	private void stubAzure() {
		stubFor(post(urlMatching("/azure/authorize/.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(ACCESS_TOKEN_RESPONSE_BODY)));
	}

	private void stubBrevserver() {
		stubFor(get(urlPathMatching("/brevweb/hentdokumentnew"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody("anders")));
	}
}
