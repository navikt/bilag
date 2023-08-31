package no.nav.bilag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.TEXT_HTML;

@Slf4j
@ControllerAdvice
public class BilagExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String feilmelding = ex.getFieldErrors().stream()
				.map(it -> format("%s, mottatt %s=%s", it.getDefaultMessage(), it.getField(), it.getRejectedValue()))
				.collect(Collectors.joining(". "));

		log.warn("Validering av request feilet med feil={}", feilmelding, ex);

		return ResponseEntity.status(BAD_REQUEST)
				.contentType(TEXT_HTML)
				.body(badRequestHttpPage());
	}

	@ExceptionHandler(Exception.class)
	void handleException(Exception ex) {
		log.warn(ex.getMessage(), ex);
		log.warn(ex.getCause().getMessage());
	}

//	@ExceptionHandler(Exception.class)
//	public ResponseEntity<Object> handleAll(Exception e) {
//		String feilmelding = format("rdist001 feilet med feilmelding=%s", e.getMessage());
//
//		log.warn(feilmelding, e);
//
//		return getResponseEntity(INTERNAL_SERVER_ERROR, feilmelding);
//	}

	private static String badRequestHttpPage() {
		return """
				<!DOCTYPE html>
				<html lang="no">
				<head>
					<meta charset="UTF-8">
				    <title>DokId er ugyldig</title>
				</head>
				<body>
				    <h1>DokId er ikke en gyldig dokumentId</h1>
				    <p>Dokid må bestå av et positivt tall.</p>
				    <p>Prøv igjen med en gyldig dokumentId, eller kontakt Team Dokumentløsninger gjennom brukerstøtte eller på Slack-kanalen #team_dokumentløsninger.</p>
				</body>
				</html>
				""";
	}
}
