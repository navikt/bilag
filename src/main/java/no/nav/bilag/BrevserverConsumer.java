package no.nav.bilag;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.auth.OauthService;
import no.nav.bilag.auth.OboTokenService;
import no.nav.bilag.webclient.NavHeadersFilter;
import no.nav.bilag.exceptions.BrevserverFunctionalException;
import no.nav.bilag.exceptions.BrevserverTechnicalException;
import no.nav.bilag.exceptions.DokumentIkkeFunnetException;
import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;

@Slf4j
@Component
public class BrevserverConsumer {

	private static final String BREVSERVER_HENTDOKUMENT_URI = "/rest/hentdokument/{dokId}";

	private final WebClient webClient;
	private final OauthService oauthService;
	private final OboTokenService oboTokenService;

	public BrevserverConsumer(BilagProperties bilagProperties,
							  OboTokenService oboTokenService,
							  WebClient webClient,
							  CodecProperties codecProperties, OauthService oauthService) {
		this.oboTokenService = oboTokenService;
		this.oauthService = oauthService;
		this.webClient = webClient.mutate()
				.baseUrl(bilagProperties.getEndpoints().getBrevserver().getUrl())
				.filter(new NavHeadersFilter())
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer
								.defaultCodecs().maxInMemorySize((int) codecProperties.getMaxInMemorySize().toBytes()))
						.build())
				.build();
	}

	@Retryable(retryFor = BrevserverFunctionalException.class)
	public byte[] hentDokument(Long dokId, HttpSession session) {

		String bearerToken = hentBearerTokenFraSession(session);

		log.info("hentDokument henter dokument med dokId={} fra brevserver", dokId);

		var response = webClient.get()
				.uri(BREVSERVER_HENTDOKUMENT_URI, dokId)
				.headers(headers -> setAuthorization(headers, bearerToken))
				.retrieve()
				.bodyToMono(byte[].class)
				.doOnError(this::handleError)
				.block();

		log.info("hentDokument har hentet dokument med dokId={} fra brevserver", dokId);

		return response;
	}

	private String hentBearerTokenFraSession(HttpSession session) {
		AccessToken rawAccessToken = oauthService.getOAuth2AuthorizationFromSession(session).get();
		return rawAccessToken.getValue();
	}

	private void setAuthorization(HttpHeaders headers, String accessToken) {
		headers.setBearerAuth(oboTokenService.fetchAccessToken(accessToken));
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot brevserver feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new BrevserverTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot brevserver feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			if (response.getStatusCode().value() == 404) {
				throw new DokumentIkkeFunnetException("Fant ikke dokument", error);
			}
			throw new BrevserverFunctionalException(feilmelding, error);
		} else {
			throw new BrevserverTechnicalException(feilmelding, error);
		}
	}

}
