package com.festora.authservice.customer.config;

import com.festora.authservice.customer.filter.SessionValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFilterConfig {

    @Bean
    public FilterRegistrationBean<SessionValidationFilter> sessionFilter(
            SessionValidationFilter filter
    ) {
        FilterRegistrationBean<SessionValidationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.setOrder(1);
        reg.addUrlPatterns("/menu/*", "/cart/*", "/order/*");
        return reg;
    }
}

