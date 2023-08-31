package no.nav.bilag;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import no.nav.bilag.auth.OauthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.valueOf;

@Slf4j
@Validated
@RestController
@RequestMapping
public class BilagController {

	private final BrevserverConsumer brevserverConsumer;
	private final OauthService oauthService;

	public BilagController(BrevserverConsumer brevserverConsumer, OauthService oauthService) {
		this.brevserverConsumer = brevserverConsumer;
		this.oauthService = oauthService;
	}

	@GetMapping("/hent/{dokId}")
	public ResponseEntity<byte[]> hentDokument(@PathVariable @Positive(message = "Sti-parameter dokId må være et positivt tall") Long dokId,
											   HttpServletRequest servletRequest) {

		AccessToken rawAccessToken = oauthService.getOAuth2AuthorizationFromSession(servletRequest.getSession()).get();

		String bearerToken = rawAccessToken.getValue();
		var dokument = brevserverConsumer.hentDokument(dokId, bearerToken);
		log.info("hentDokument har hentet dokument med dokId={}", dokId);

		return ResponseEntity.ok().contentType(valueOf(APPLICATION_PDF_VALUE)).body(dokument);
	}

}