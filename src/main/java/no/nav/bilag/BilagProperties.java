package no.nav.bilag;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("bilag")
public class BilagProperties {

	private final Endpoints endpoints = new Endpoints();

	@NotEmpty
	private String baseUrl;

	@NotEmpty
	private String azureScope;

	public String[] getAzureScope() {
		return azureScope.split(",");
	}

	@Data
	@Validated
	public static class Endpoints {
		@NotNull
		private AppEndpoint brevserver;
	}

	@Data
	@Validated
	public static class AppEndpoint {
		@NotEmpty
		private String url;

		@NotEmpty
		private String scope;
	}

}
