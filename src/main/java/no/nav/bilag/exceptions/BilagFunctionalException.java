package no.nav.bilag.exceptions;

public abstract class BilagFunctionalException extends RuntimeException {

	public BilagFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
