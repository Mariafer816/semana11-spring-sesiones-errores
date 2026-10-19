package Sistema.Reservas.Aulas.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API = "/api/**";
    private static final String LOGIN = "/login";
    private static final String REGISTRO = "/registro";
    private static final String LOGOUT = "/logout";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * API REST: se accede con HTTP Basic (correo y contraseña), por ejemplo desde Postman.
     * Al no usar cookies de sesión no necesita token CSRF.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher(API)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** Páginas web: todo requiere sesión salvo el login, el registro y los recursos estáticos. */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(HttpSecurity http,
                                           LoginSuccessHandler loginSuccessHandler,
                                           CerrarSesionHandler cerrarSesionHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(LOGIN, REGISTRO, "/error", "/css/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(LOGIN)
                        .loginProcessingUrl(LOGIN)
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)   // guarda el nombre en la HttpSession
                        .failureUrl(LOGIN + "?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl(LOGOUT)              // POST + token CSRF (lo agrega Thymeleaf)
                        .addLogoutHandler(cerrarSesionHandler)   // invalida la HttpSession
                        .deleteCookies("JSESSIONID")             // borra la cookie de sesión del navegador
                        .logoutSuccessUrl(LOGIN + "?logout")
                        .permitAll());
        return http.build();
    }
}
