package no.nav.bilag.exceptions;

public abstract class BilagTechnicalException extends RuntimeException {

	public BilagTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
