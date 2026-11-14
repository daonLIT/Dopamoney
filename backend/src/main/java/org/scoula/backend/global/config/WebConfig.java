package org.scoula.backend.global.config;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final AuthInterceptor authInterceptor;

    public WebConfig(@Value("${app.frontend-url}") String frontendUrl, AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
        // FRONTEND_URL이 비었거나 스킴/슬래시가 어긋나도 Render 배포가 막히지 않게 기본 패턴을 함께 허용한다.
        this.allowedOrigins = Stream.concat(
                        Arrays.stream(frontendUrl.split(","))
                                .map(String::trim)
                                .map(origin -> origin.replaceAll("/+$", ""))
                                .filter(origin -> !origin.isEmpty())
                                .map(origin -> origin.contains("://") ? origin : "https://" + origin),
                        Stream.of("https://*.onrender.com", "http://localhost:*", "http://127.0.0.1:*"))
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
