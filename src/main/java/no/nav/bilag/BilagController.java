package no.nav.bilag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.springframework.http.MediaType.APPLICATION_PDF;

@Slf4j
@Validated
@Controller
public class BilagController {

	private final BrevserverConsumer brevserverConsumer;

	public BilagController(BrevserverConsumer brevserverConsumer) {
		this.brevserverConsumer = brevserverConsumer;
	}

	@GetMapping("/hent/{dokId}")
	public ResponseEntity<byte[]> hentDokument(@PathVariable @Positive(message = "Sti-parameter dokId må være et positivt tall") Long dokId,
											   HttpServletRequest servletRequest) {

		var dokument = brevserverConsumer.hentDokument(dokId, servletRequest.getSession());

		log.info("hentDokument har hentet dokument med dokId={}", dokId);

		return ResponseEntity.ok().contentType(APPLICATION_PDF).body(dokument);
	}

}