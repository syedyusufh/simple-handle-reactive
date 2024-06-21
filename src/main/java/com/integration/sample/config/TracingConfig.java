package com.integration.sample.config;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Component
public class TracingConfig {

	@Autowired
	private WebClient.Builder webClientBuiler;

	@Autowired
	private CustomWebClientLogger customWebClientLogger;

	@Bean
	public WebClient webClient() {

		// @formatter:off
		return webClientBuiler
						.clientConnector(createReactorClientHttpConnector())
						.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.filters(exchangeFilters -> {
							exchangeFilters.add(requestHeaders());
							exchangeFilters.add(responseHeaders());
						})
						.build();
		// @formatter:on
	}

	public ExchangeFilterFunction requestHeaders() {

		return (clientRequest, next) -> Mono.deferContextual(contextextView -> {
			log.info("Request Headers: {}", clientRequest.headers());
			return next.exchange(clientRequest);
		});
	}

	public ExchangeFilterFunction responseHeaders() {

		return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
			log.info("Response Headers: {}", clientResponse.headers().asHttpHeaders());
			return Mono.just(clientResponse);
		});
	}

	/** Reusable Utils */
	private HttpClient createHttpClient() {

		// @formatter:off
		return HttpClient.create()
						.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
						.protocol(HttpProtocol.H2, HttpProtocol.HTTP11)
						.doOnRequest((request, condition) -> condition.addHandlerFirst(customWebClientLogger))
						.responseTimeout(Duration.ofSeconds(2))
						.secure(t -> t.sslContext(sslContext()));
		// @formatter:on
	}

	private ReactorClientHttpConnector createReactorClientHttpConnector() {

		var httpClient = createHttpClient();
		return new ReactorClientHttpConnector(httpClient);
	}

	@Bean
	public SslContext sslContext() {

		// @formatter:off
		try {
			return SslContextBuilder.forClient()
									.trustManager(InsecureTrustManagerFactory.INSTANCE)
									.build();
		} catch (SSLException e) {
			e.printStackTrace();
			return null;
		}
		// @formatter:on
	}

}
