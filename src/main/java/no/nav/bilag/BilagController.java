package no.nav.bilag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
	public ResponseEntity<byte[]> hentDokument(@PathVariable
											   @NotBlank(message = "dokId kan ikke være blank")
											   @Pattern(regexp = "^\\d{1,32}$", message = "dokId må være numerisk og må ha 1-32 siffer")
											   String dokId,
											   HttpServletRequest servletRequest) {
		log.info("hentDokument henter dokument med dokId={} fra brevserver", dokId);
		var dokument = brevserverConsumer.hentDokument(dokId, servletRequest.getSession());
		log.info("hentDokument har hentet dokument med dokId={}", dokId);
		return ResponseEntity.ok().contentType(APPLICATION_PDF).body(dokument);
	}
}