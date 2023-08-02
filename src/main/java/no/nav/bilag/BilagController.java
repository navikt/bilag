package no.nav.bilag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rest")
public class BilagController {

	@GetMapping("/hentDokument")
	public void hentDokument() {
		log.info("Kall til hentDokument mottatt");
	}
}