package com.jungleegames.apigateway.filters;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Slf4j
public class LoggingFilter implements GatewayFilter, Ordered {
	private LoggingGatewayFilterFactory.Config config;

	public LoggingFilter(LoggingGatewayFilterFactory.Config config) {
		this.config = config;
	}

	@Override
	public int getOrder() {
		// -1 is response write filter, must be called before that
		return -2;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpResponse originalResponse = exchange.getResponse();
		DataBufferFactory bufferFactory = originalResponse.bufferFactory();
		ServerHttpRequest request = exchange.getRequest();
		String requestId = request.getHeaders().getFirst("requestId") == null
				? (request.getHeaders().getFirst("userId") ==null?"Guest"+new Random().nextInt(10000):request.getHeaders().getFirst("userId"))+ "_" + System.currentTimeMillis()
				: request.getHeaders().getFirst("requestId");
		if(config.isRequestLogEnable()) {
		Flux<DataBuffer> requestBody = exchange.getRequest().getBody();
		
		requestBody.subscribe(buffer -> {
			byte[] bytes = new byte[buffer.readableByteCount()];
			buffer.read(bytes);
			DataBufferUtils.release(buffer);
			String reqBody = new String(bytes, StandardCharsets.UTF_8);
			log.info("requestId: "+requestId+"\nRequest : " + (config.isRequestMaskEnable()?maskValues(reqBody, config.getRequestMaskKeys()):reqBody));
		});
		}

		ServerHttpResponse response = exchange.getResponse();
		response.getHeaders().add("requestId", requestId);
		ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				if (config.isResponseLogEnable()&&(body instanceof Flux)) {
					Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
					return super.writeWith(fluxBody.map(dataBuffer -> {
						// probably should reuse buffers
						byte[] content = new byte[dataBuffer.readableByteCount()];
						dataBuffer.read(content);
						// Release the memory
						DataBufferUtils.release(dataBuffer);

						String s = new String(content, Charset.forName("UTF-8"));
						log.info("requestId: "+requestId+"\nResponse : " + (config.isResponseMaskEnable()?maskValues(s, config.getResponseMaskKeys()):s));
						return bufferFactory.wrap(new String(content, Charset.forName("UTF-8")).getBytes());
					}));
				}
				// if body is not a flux. never got there.
				return super.writeWith(body);
			}
		};
		// replace response with decorator
		return chain.filter(exchange.mutate().response(decoratedResponse).build());
	}

	private String maskValues(String s, List<String> maskKyes) {
		StringBuilder sb = new StringBuilder(s);
		for (String key : maskKyes) {
			for (int index = s.indexOf("\"" + key + "\""); index >= 0; index = s.indexOf("\"" + key + "\"",
					index + 1)) {
				for (int idx = s.indexOf(":", index + 1)+1; idx < s.length(); ++idx) {
					char c = s.charAt(idx);
					if (c == ',' || c == '}' || c == ']') {
						break;
					}
					if (c != '"'&&c!=' ') {
						sb.setCharAt(idx, '*');
					}
				}
			}
		}

		return sb.toString();
	}

}
