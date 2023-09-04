package no.nav.bilag.config;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static no.nav.bilag.config.MDCInterceptor.getCallId;
import static no.nav.bilag.config.MDCInterceptor.getUserId;

public class NavHeadersFilter implements ExchangeFilterFunction {

	public static final String NAV_CALLID = "Nav-Callid";
	public static final String NAV_USER_ID = "Nav-User-Id";

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(ClientRequest.from(request)
				.headers(httpHeaders -> {
					httpHeaders.set(NAV_CALLID, getCallId());
					httpHeaders.set(NAV_USER_ID, getUserId());
				})
				.build());
	}
}
