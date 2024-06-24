package com.integration.sample.config;

import static java.time.Duration.ofMinutes;
import static org.springframework.integration.context.IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
import static org.springframework.integration.dsl.Pollers.fixedDelay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RestoreReactorContextFlowConfig {

	private static final String ECHO_GET = "https://echo.free.beeceptor.com/sample-request?author=beeceptor";

	@Bean
	public IntegrationFlow restoreReactorContextFlow(WebClient webClient) {

		// @formatter:off
		return IntegrationFlow.fromSupplier(() -> "Good Morning", ec -> ec.poller(fixedDelay(ofMinutes(1))))
							.enrichHeaders(hdrSpec -> hdrSpec.headerFunction("INPUT_GREETING", m -> m.getPayload()))
							.log(LoggingHandler.Level.INFO, m -> "----------------------START-------------------------")
							.log(LoggingHandler.Level.INFO, m -> "WebClient GET Request (start of reactorContext)")
							
							.handle(WebFlux.outboundGateway(ECHO_GET, webClient)
										.httpMethod(HttpMethod.GET)
										.expectedResponseType(String.class),
									ec -> ec.customizeMonoReply((message, mono) -> mono.contextCapture()))
							.channel(new ExecutorSubscribableChannel())
							
							// induced exception
							.transform(Message.class, m -> {
								throw new RuntimeException("Induced");
							})
							
							.log(LoggingHandler.Level.INFO, m -> "WebClient Response (with diferent traceId) in Imperative: " + m.getPayload())
							.log(LoggingHandler.Level.INFO, m -> "---------------------END-----------------------------")
							.channel(NULL_CHANNEL_BEAN_NAME)
							.get();
		// @formatter:on
	}

}
