package no.nav.bilag;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.exceptions.BrevserverFunctionalException;
import no.nav.bilag.exceptions.BrevserverTechnicalException;
import no.nav.bilag.exceptions.DokumentIkkeFunnetException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.TEXT_HTML;

@Slf4j
@ControllerAdvice
public class BilagExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({
				ConstraintViolationException.class, // feiler Positive-annotasjonen
				MethodArgumentTypeMismatchException.class // får inn string i stedet for Long etc.
	})
	public ResponseEntity<Object> invalidInputHandler(Exception e) {
		log.warn("Ugyldig inputdata til hentDokument med feilmelding={}", e.getMessage(), e);

		return ResponseEntity
				.status(BAD_REQUEST)
				.contentType(TEXT_HTML)
				.body(badRequestHtml());
	}

	@ExceptionHandler({DokumentIkkeFunnetException.class})
	public ResponseEntity<Object> dokumentIkkeFunnetHandler(Exception e) {
		log.warn("Dokument ikke funnet i brevserver med feilmelding={}", e.getMessage(), e);

		return ResponseEntity
				.status(NOT_FOUND)
				.contentType(TEXT_HTML)
				.body(dokumentIkkeFunnetHtml());
	}

	@ExceptionHandler({BrevserverTechnicalException.class})
	public ResponseEntity<Object> tekniskFeilHandler(Exception e) {
		log.warn("Teknisk feil med feilmelding={}", e.getMessage(), e);

		return ResponseEntity
				.status(INTERNAL_SERVER_ERROR)
				.contentType(TEXT_HTML)
				.body(tekniskFeilHtml());
	}

	@ExceptionHandler({BrevserverFunctionalException.class})
	public ResponseEntity<Object> funksjonellFeilHandler(Exception e) {
		log.warn("Funksjonell feil med feilmelding={}", e.getMessage(), e);

		return ResponseEntity
				.status(INTERNAL_SERVER_ERROR)
				.contentType(TEXT_HTML)
				.body(funksjonellFeilHtml());
	}

	@ExceptionHandler({Exception.class})
	public ResponseEntity<Object> catchUnhandledExceptions(Exception e) {
		log.warn("Uhåndtert execption: " + e);

		return ResponseEntity
				.status(INTERNAL_SERVER_ERROR)
				.contentType(TEXT_HTML)
				.body(ukjentTekniskFeilHtml());
	}

	private static String badRequestHtml() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>DokId er ugyldig</title>
				</head>
				<body>
				    <h1>DokId er ikke en gyldig dokumentId</h1>
				    <p>DokId må være et 1-32 langt numerisk siffer.</p>
				    <p>Prøv igjen med en gyldig dokumentId, eller kontakt Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}

	private static String dokumentIkkeFunnetHtml() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>Dokument ikke funnet</title>
				</head>
				<body>
				    <h1>Fant ikke dokumentet</h1>
				    <p>Det kan være at dokumentet fremdeles er under oppretting, eller det kan ha feilet under produksjon.</p>
				    <p>Prøv igjen med en gyldig dokumentId, eller kontakt Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}

	private static String tekniskFeilHtml() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>Teknisk feil</title>
				</head>
				<body>
				    <h1>Teknisk feil</h1>
				    <p>Feilen kan komme av problem med bakenforliggende systemer.</p>
				    <p>Prøv igjen med en gyldig dokumentId, eller kontakt Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}

	private static String funksjonellFeilHtml() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>Funksjonell feil</title>
				</head>
				<body>
				    <h1>Funksjonell feil</h1>
				    <p>Det kan være flere grunner til denne feilen.</>
				    <p>Prøv igjen med en gyldig dokumentId, eller kontakt Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}

	private static String ukjentTekniskFeilHtml() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>Uhåndtert teknisk feil</title>
				</head>
				<body>
				    <h1>Uhåndtert teknisk feil</h1>
				    <p>Prøv igjen senere. Ved vedvarende problemer kan du kontakte Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}

}
