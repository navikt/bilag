package no.nav.bilag.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
		@JsonProperty(value = "access_token", required = true)
		String accessToken
){}
