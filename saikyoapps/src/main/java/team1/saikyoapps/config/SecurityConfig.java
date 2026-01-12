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

    UserDetails sky7 = User.withUsername("sky7")
        .password("{bcrypt}$2y$05$fgf/ie7/RT8HT2sllPhsJe.qdYXAKLdiYWh4l/msdk5BLuesaWfpK")
        .roles("PLAYER")
        .build();

    UserDetails pico = User.withUsername("pico")
        .password("{bcrypt}$2y$05$Tc4gsbhgrzq7.wx9Lfhq9.Dd6Q0dR5xRsv9idvSi8x4bihWW/1Wju")
        .roles("PLAYER")
        .build();

    UserDetails run = User.withUsername("run")
        .password("{bcrypt}$2y$05$VPduRV82dbyF1G5eKX.PiefQm08n.t84xvcj2Gp9SwUVybPAyZdnu")
        .roles("PLAYER")
        .build();

    UserDetails jake = User.withUsername("jake")
        .password("{bcrypt}$2y$05$6QByN3OEhicfc9LgktibaOiEA8cYT7Hrt0jGvFiW56Qvr3sgR9X5G")
        .roles("PLAYER")
        .build();

    UserDetails leaf = User.withUsername("leaf")
        .password("{bcrypt}$2y$05$nRyVF8SdvWafD7O96gNiL.G5fa3xITlLmw1sAeNpcjQI6I8Md/IfW")
        .roles("PLAYER")
        .build();

    UserDetails tomo = User.withUsername("tomo")
        .password("{bcrypt}$2y$05$nQ1hLQk12erPHtucx/rvjuWFuSEQIwBCaDmR01mfhoXSudx7PZdjq")
        .roles("PLAYER")
        .build();

    UserDetails wave = User.withUsername("wave")
        .password("{bcrypt}$2y$05$Ze6K93tVkQ1W0kj4UgqTcuqLG4GqOedet09swkVhrTTb62hpCYlQW")
        .roles("PLAYER")
        .build();

    UserDetails fox = User.withUsername("fox")
        .password("{bcrypt}$2y$05$TZJDAXlhUejLypXXdTOCAuTtxgNKZ/kJWRYsolTMQGYB6c0rWbcSu")
        .roles("PLAYER")
        .build();

    UserDetails k_5 = User.withUsername("k_5")
        .password("{bcrypt}$2y$05$oVXKReaRIaVEm62b58XjJ.baNrXM7uLq.IZCaSbQ4G9YeAdui7uzC")
        .roles("PLAYER")
        .build();

    UserDetails blue = User.withUsername("blue")
        .password("{bcrypt}$2y$05$OaRg9ZMzbce0SU4QGItUWuopmLuOu5IHnsBh0Z1FZpxGS3ssH8wGy")
        .roles("PLAYER")
        .build();



    return new InMemoryUserDetailsManager(foo, bar, buz, sky7, pico, run, jake, leaf, tomo, wave, fox, k_5, blue);
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
