package no.nav.bilag.auth;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureProperties(
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret,
		@NotEmpty String openidConfigTokenEndpoint
) {

	public String getLoginEndpoint() {
		return openidConfigTokenEndpoint().replace("/token", "/authorize");
	}

}