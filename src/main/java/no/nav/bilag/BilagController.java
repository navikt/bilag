package no.nav.bilag;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Validated
@RestController
@RequestMapping("/rest")
public class BilagController {

	private static final String BILAG_FUNKSJONELL_FEILMELDING = "bilag feilet funksjonelt med feilmelding: {}";

	@GetMapping("/hentdokument/{dokId}")
	public String hentDokument(@PathVariable @Positive(message = "Sti-parameter dokId må være et positivt tall") Long dokId) {

		log.info("hentDokument har mottatt kall om å hente dokument med dokId={}", dokId);

		return "success";
	}

	@ExceptionHandler({
			ConstraintViolationException.class,
	})
	public ResponseEntity<Object> inputValidationExceptionHandler(Exception e) {
		log.warn(BILAG_FUNKSJONELL_FEILMELDING, e.getMessage(), e);

		return getResponseEntity(BAD_REQUEST, e.getMessage());
	}

	private static ResponseEntity<Object> getResponseEntity(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.contentType(APPLICATION_JSON)
				.body(format("\"%s\"", message));
	}
}