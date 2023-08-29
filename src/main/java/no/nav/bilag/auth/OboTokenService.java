package no.nav.bilag.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.BilagProperties;
import no.nav.bilag.exceptions.OboTokenException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@Slf4j
@Service
public class OboTokenService {

	private static final String JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";
	private static final String ON_BEHALF_OF = "on_behalf_of";

	private final AzureProperties azureProperties;
	private final BilagProperties bilagProperties;
	private final ObjectMapper objectMapper;
	private final WebClient webClient;

	public OboTokenService(AzureProperties azureProperties,
						   BilagProperties bilagProperties,
						   ObjectMapper objectMapper,
						   WebClient webClient) {
		this.azureProperties = azureProperties;
		this.bilagProperties = bilagProperties;
		this.objectMapper = objectMapper;
		this.webClient = webClient.mutate()
				.baseUrl(azureProperties.openidConfigTokenEndpoint())
				.defaultHeader(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
				.build();
	}

	public String fetchAccessToken(String accessToken) {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("client_id", azureProperties.appClientId());
		formData.add("client_secret", azureProperties.appClientSecret());
		formData.add("scope", bilagProperties.getEndpoints().getBrevserver().getScope());
		formData.add("requested_token_use", ON_BEHALF_OF);
		formData.add("grant_type", JWT_BEARER);
		formData.add("assertion", accessToken);

		String responseJson = webClient
				.post()
				.body(BodyInserters.fromFormData(formData))
				.retrieve()
				.bodyToMono(String.class)
				.doOnError(this::handleError)
				.block();

		try {
			return objectMapper.readValue(responseJson, TokenResponse.class).accessToken();
		} catch (JsonProcessingException e) {
			throw new OboTokenException("Klarte ikke parse token fra Azure. Feilmelding=" + e.getMessage(), e);
		}
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {

			// TODO: Når gruppetilgang blir strammet inn må en håndtere feilmelding her dersom gruppetilgang mangler
			String feilmelding = format("Klarte ikke hente token fra Azure. Kall mot Microsoft feilet funksjonelt med status=%s, feilmelding=%s, response=%s",
					response.getStatusCode(),
					response.getMessage(),
					response.getResponseBodyAsString());

			throw new OboTokenException(feilmelding, error);
		} else {
			throw new OboTokenException(format("Kall mot Azure feilet teknisk med feilmelding=%s", error.getMessage()), error);
		}
	}

}
