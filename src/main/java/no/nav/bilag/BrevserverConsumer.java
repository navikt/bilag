package no.nav.bilag;

import lombok.extern.slf4j.Slf4j;
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

	private final WebClient webClient;
	private final OboTokenService oboTokenService;

	public BrevserverConsumer(BilagProperties bilagProperties,
							  OboTokenService oboTokenService,
							  WebClient webClient,
							  CodecProperties codecProperties) {
		this.oboTokenService = oboTokenService;
		this.webClient = webClient.mutate()
				.baseUrl(bilagProperties.getEndpoints().getBrevserver().getUrl())
				.filter(new NavHeadersFilter())
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer
								.defaultCodecs().maxInMemorySize((int) codecProperties.getMaxInMemorySize().toBytes()))
						.build())
				.build();
	}

	private void setAuthorization(HttpHeaders headers, String accessToken) {
		headers.setBearerAuth(oboTokenService.fetchAccessToken(accessToken));
	}

	@Retryable(retryFor = BrevserverFunctionalException.class)
	public byte[] hentDokument(Long dokId, String accessToken) {
		log.info("hentDokument henter dokument med dokId={} fra brevserver", dokId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{dokId}")
						.build(dokId))
				.headers(headers -> setAuthorization(headers, accessToken))
				.retrieve()
				.bodyToMono(byte[].class)
				.doOnError(this::handleError)
				.block();

		log.info("hentDokument har hentet dokument med dokId={} fra brevserver", dokId);

		return response;
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
				var feil = "Fant ikke dokument";
				throw new DokumentIkkeFunnetException(feil, error);
			}
			throw new BrevserverFunctionalException(feilmelding, error);
		} else {
			throw new BrevserverTechnicalException(feilmelding, error);
		}
	}

}
