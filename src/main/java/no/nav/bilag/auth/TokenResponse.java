package no.nav.bilag.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(
		@JsonProperty(value = "access_token", required = true)
		String accessToken
){}
