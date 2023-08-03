package no.nav.bilag.auth;

import no.nav.bilag.exceptions.BilagTechnicalException;

public class AzureTokenException extends BilagTechnicalException {

	public AzureTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
