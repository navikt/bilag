package no.nav.bilag;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.auth.OauthService;
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
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.valueOf;

@Slf4j
@Validated
@RestController
@RequestMapping("/rest")
public class BilagController {

	private static final String BILAG_FUNKSJONELL_FEILMELDING = "bilag feilet funksjonelt med feilmelding: {}";
	private final BrevserverConsumer brevserverConsumer;
	private final OauthService oauthService;

	public BilagController(BrevserverConsumer brevserverConsumer, OauthService oauthService) {
		this.brevserverConsumer = brevserverConsumer;
		this.oauthService = oauthService;
	}

	@GetMapping("/hentdokument/{dokId}")
	public ResponseEntity<byte[]> hentDokument(@PathVariable @Positive(message = "Sti-parameter dokId må være et positivt tall") Long dokId,
											   HttpServletRequest servletRequest) {

		AccessToken rawAccessToken = oauthService.getOAuth2AuthorizationFromSession(servletRequest.getSession()).get();

		String bearerToken = rawAccessToken.getValue();
		var dokument = brevserverConsumer.hentDokument(dokId, bearerToken);
		log.info("hentDokument har hentet dokument med dokId={}", dokId);

		return ResponseEntity.ok().contentType(valueOf(APPLICATION_PDF_VALUE)).body(dokument);
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