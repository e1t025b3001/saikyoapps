package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Locale;

@ControllerAdvice
public class LocaleModelAdvice {

  @Autowired
  private MessageSource messageSource;

  @ModelAttribute("i18n")
  public String currentLocale(HttpServletRequest request) {
    // 先從 session 取值（I18nController 會放入 session）
    Object sess = request.getSession().getAttribute("i18n");
    if (sess instanceof String)
      return (String) sess;

    // 再從 cookie 取值（I18nController 會寫入名為 "i18n" 的 cookie）
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie c : cookies) {
        if ("i18n".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
          return c.getValue();
        }
      }
    }

    // 最後使用 LocaleResolver 回傳的 Locale，轉為簡短 code
    Locale locale = RequestContextUtils.getLocale(request);
    if (locale == null)
      return "ja";
    String lang = locale.getLanguage();
    if ("zh".equals(lang)) {
      String country = locale.getCountry();
      if ("TW".equalsIgnoreCase(country))
        return "zh_TW";
      return "zh";
    }
    if ("ja".equals(lang))
      return "ja";
    if ("en".equals(lang))
      return "en";
    return lang;
  }

  @ModelAttribute("i18n_title")
  public String i18nTitle(HttpServletRequest request, Principal principal) {
    Locale locale = RequestContextUtils.getLocale(request);
    String username = principal != null ? principal.getName() : "";
    try {
      return messageSource.getMessage("title", new Object[] { username }, locale);
    } catch (Exception e) {
      return "";
    }
  }
}
