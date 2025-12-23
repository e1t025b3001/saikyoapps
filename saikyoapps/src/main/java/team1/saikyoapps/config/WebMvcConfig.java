package team1.saikyoapps.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Interceptor to apply locale from session or cookie to LocaleContextHolder
    registry.addInterceptor(new HandlerInterceptor() {
      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. session
        Object sess = request.getSession().getAttribute("i18n");
        String code = null;
        if (sess instanceof String) {
          code = (String) sess;
        }
        // 2. cookie
        if (code == null) {
          Cookie[] cookies = request.getCookies();
          if (cookies != null) {
            for (Cookie c : cookies) {
              if ("i18n".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                code = c.getValue();
                break;
              }
            }
          }
        }
        if (code != null) {
          // normalize underscore to hyphen for BCP47
          String bcp = code.replace('_', '-');
          Locale locale = Locale.forLanguageTag(bcp);
          if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isEmpty()) {
            LocaleContextHolder.setLocale(locale);
          }
        }
        return true;
      }
    }).addPathPatterns("/**");
  }
}
