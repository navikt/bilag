package no.nav.bilag.auth;

public class UserAuthorizationException extends Exception {

	UserAuthorizationException(String message) {
		super(message);
	}

	UserAuthorizationException(String message, Throwable cause) {
		super(message, cause);
	}
}
