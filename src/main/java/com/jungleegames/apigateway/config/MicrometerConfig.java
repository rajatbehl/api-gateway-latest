package com.jungleegames.apigateway.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.newrelic.telemetry.Attributes;

import io.micrometer.NewRelicRegistryConfig;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.newrelic.NewRelicRegistry;

@Configuration
@AutoConfigureBefore({
    CompositeMeterRegistryAutoConfiguration.class,
    SimpleMetricsExportAutoConfiguration.class
})
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnClass(NewRelicRegistry.class)
public class MicrometerConfig {

    @Bean
    public NewRelicRegistryConfig newRelicConfig() {
        return new NewRelicRegistryConfig() {
        	
        	@Value("${newrelic.api-key}")
        	String apiKey;
        	
        	@Value("${newrelic.uri}")
        	String uri;
        	
        	@Value("${newrelic.step}")
        	private long step;
        	
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String apiKey() {
                return apiKey;
            }

            @Override
            public String uri() {
              return uri;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(step);
            }
            
            @Value("${spring.application.name}")
            String serviceName;

            @Override
            public String serviceName() {
                return serviceName;
            }
        };
    }

    @Bean
    public NewRelicRegistry newRelicMeterRegistry(NewRelicRegistryConfig config)
        throws UnknownHostException {
        NewRelicRegistry newRelicRegistry =
            NewRelicRegistry.builder(config)
                .commonAttributes(
                    new Attributes()
                        .put("host.hostname", InetAddress.getLocalHost().getHostName()))
                .build();
        newRelicRegistry.start(new NamedThreadFactory("newrelic.micrometer.registry"));
        return newRelicRegistry;
    }
}