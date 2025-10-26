package org.example.tryonx.security.config;

import org.example.tryonx.security.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfigs {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfigs(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain myFilter(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a->a
                        // Admin only endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                        
                        // Public endpoints - 명시적으로 먼저 선언
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/main/similar-styles", "/api/v1/main/popular-styles").permitAll()
                        
                        // Auth endpoints
                        .requestMatchers(
                        "/login",
                                "/ws/**",
                        "/index.html",
                        "/api/v1/auth/**",
                        "/api/v1/fitting/camera/*",
                                "/upload/**",
                        "/kakao/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                ).permitAll()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated())
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        // 변경: ngrok 주소를 허용 목록에 추가
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://*.ngrok-free.app"
        ));

        // WebSocket 엔드포인트 허용 추가
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder makePassword(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
