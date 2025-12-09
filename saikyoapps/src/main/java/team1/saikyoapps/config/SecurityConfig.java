package team1.saikyoapps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // DelegatingPasswordEncoder を使うことで {bcrypt} プレフィックス付きハッシュを扱えるようにする
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    // 事前にハッシュ済みパスワードを {bcrypt} プレフィックス付きで登録する
    UserDetails foo = User.withUsername("foo")
        .password("{bcrypt}$2y$05$m/bFb4oVM/SwkJmjRNJQr.M.c46/gdDu39kvXTWoGmxVwsdf0PVAy")
        .roles("PLAYER")
        .build();

    UserDetails bar = User.withUsername("bar")
        .password("{bcrypt}$2y$05$4LwYSdqYDV3u9m3IwBWVFe.u52Uf8xLJlc1/tJfIorbnn.hLIJsaK")
        .roles("PLAYER")
        .build();

    UserDetails buz = User.withUsername("buz")
        .password("{bcrypt}$2y$05$nUsb7ufJ83W9cIpc5KiM6e.JtFcl8Um0qkgUU1yKSENPSnUD3sB/a")
        .roles("PLAYER")
        .build();

    return new InMemoryUserDetailsManager(foo, bar, buz);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/login")
            .permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
            .permitAll())
        .logout(logout -> logout.permitAll())
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/h2-console/*", "/gomoku/**"))
        .headers(headers -> headers
            .frameOptions(
                frameOptions -> frameOptions
                    .sameOrigin()));

    return http.build();
  }
}
