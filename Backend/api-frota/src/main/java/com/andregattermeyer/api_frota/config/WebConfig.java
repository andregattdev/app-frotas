package com.andregattermeyer.api_frota.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://192.168.*.*:4200",
                        "http://10.*.*.*:4200",
                        "http://172.16.*.*:4200",
                        "http://172.17.*.*:4200",
                        "http://172.18.*.*:4200",
                        "http://172.19.*.*:4200",
                        "http://172.20.*.*:4200",
                        "http://172.21.*.*:4200",
                        "http://172.22.*.*:4200",
                        "http://172.23.*.*:4200",
                        "http://172.24.*.*:4200",
                        "http://172.25.*.*:4200",
                        "http://172.26.*.*:4200",
                        "http://172.27.*.*:4200",
                        "http://172.28.*.*:4200",
                        "http://172.29.*.*:4200",
                        "http://172.30.*.*:4200",
                        "http://172.31.*.*:4200",
                        "https://localhost:4200",
                        "https://127.0.0.1:4200",
                        "https://192.168.*.*:4200",
                        "https://10.*.*.*:4200",
                        "https://172.16.*.*:4200",
                        "https://172.17.*.*:4200",
                        "https://172.18.*.*:4200",
                        "https://172.19.*.*:4200",
                        "https://172.20.*.*:4200",
                        "https://172.21.*.*:4200",
                        "https://172.22.*.*:4200",
                        "https://172.23.*.*:4200",
                        "https://172.24.*.*:4200",
                        "https://172.25.*.*:4200",
                        "https://172.26.*.*:4200",
                        "https://172.27.*.*:4200",
                        "https://172.28.*.*:4200",
                        "https://172.29.*.*:4200",
                        "https://172.30.*.*:4200",
                        "https://172.31.*.*:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
