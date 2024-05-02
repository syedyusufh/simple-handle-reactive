package com.integration.sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class SimpleHandleReactiveFlowConfig {

	@Bean
	public IntegrationFlow monoAsyncFalseFlow() {

		// @formatter:off
		return IntegrationFlow.fromSupplier(() -> "Good Morning")
							.enrichHeaders(hdrSpec -> hdrSpec.headerFunction("INPUT_GREETING", m -> m.getPayload()))
							.handle((p, headers) -> {
								log.info("TraceId is getting printed here");
								return p;
							})
							.handleReactive(m -> this.voidMono());
		// @formatter:on
	}

	@Bean
	public Mono<Void> voidMono() {

		// @formatter:off
		return Mono.just("Hi There")
					.doOnSuccess(retVal -> log.info("Expecting TraceId here"))
					.then();
		// @formatter:on
	}

}
