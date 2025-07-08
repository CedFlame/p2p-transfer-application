package ru.shmatov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.shmatov.config.security.filter.JwtFilter;
import ru.shmatov.util.JwtUtil;

@Configuration
public class ComponentConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(); // без зависимостей
    }

    @Bean
    public JwtFilter jwtFilter(JwtUtil jwtUtil, org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        return new JwtFilter(jwtUtil, userDetailsService);
    }
}
