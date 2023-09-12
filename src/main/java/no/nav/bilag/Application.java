package no.nav.bilag;

import no.nav.bilag.auth.AzureProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		BilagProperties.class,
		AzureProperties.class
})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
