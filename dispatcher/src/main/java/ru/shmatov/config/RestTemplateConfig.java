package ru.shmatov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        var restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // ничего не делаем — отключаем выброс исключений
            }
        });

        return restTemplate;
    }
}
