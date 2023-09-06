package no.nav.bilag.mdc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcInterceptorConfig implements WebMvcConfigurer {

	private final MDCInterceptor mdcInterceptor;

	public WebMvcInterceptorConfig(MDCInterceptor mdcInterceptor) {
		this.mdcInterceptor = mdcInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(mdcInterceptor);
	}
}
