package com.dustymotors.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Value('${dustybox.security.enabled:true}')
    private boolean securityEnabled

    @Value('${dustybox.security.username:admin}')
    private String username

    @Value('${dustybox.security.password:admin}')
    private String password

    @Value('${dustybox.security.csrf-enabled:true}')
    private boolean csrfEnabled

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            return http
                    .authorizeHttpRequests { auth ->
                        auth.anyRequest().permitAll()
                    }
                    .csrf(AbstractHttpConfigurer::disable)
                    .build()
        }

        http
                .authorizeHttpRequests { auth ->
                    auth
                    // Разрешаем доступ к статическим ресурсам и страницам логина без аутентификации
                            .requestMatchers("/", "/index.htm", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                    // Разрешаем доступ к Swagger UI
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    // Все остальные запросы требуют аутентификации
                            .anyRequest().authenticated()
                }
                .sessionManagement { session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                }
                .formLogin { form ->
                    form
                            .loginPage("/login")
                            .defaultSuccessUrl("/web/scripts", true)
                            .failureUrl("/login?error=true")
                            .permitAll()
                }
                .logout { logout ->
                    logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl("/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll()
                }
                .httpBasic { httpBasic ->
                    httpBasic.realmName("Dustybox API")
                }

        // Настройка CSRF
        if (!csrfEnabled) {
            http.csrf(AbstractHttpConfigurer::disable)
        } else {
            // Разрешаем CSRF для API если нужно
            http.csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**")
            }
        }

        return http.build()
    }

    @Bean
    UserDetailsService userDetailsService() {
        def user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("USER", "ADMIN")
                .build()

        return new InMemoryUserDetailsManager(user)
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }
}