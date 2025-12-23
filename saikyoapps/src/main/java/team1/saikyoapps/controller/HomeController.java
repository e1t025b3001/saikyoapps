package team1.saikyoapps.controller;

import java.util.Optional;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import team1.saikyoapps.model.I18nConfigMappaer;

@Controller
public class HomeController {

  @Autowired
  I18nConfigMappaer i18nConfigMappaer;

  @Autowired
  MessageSource messageSource;

  @Autowired
  LocaleResolver localeResolver;

  @GetMapping("/")
  public String index(Model model, Authentication authentication) {
    if (authentication != null) {
      String user = authentication.getName();
      model.addAttribute("username", user);
    } else {
      model.addAttribute("username", "guest");
    }

    // 如果 LocaleModelAdvice 已經放入了 i18n（來源：session/cookie/RequestContext），不要覆寫。
    if (!model.containsAttribute("i18n")) {
      if (authentication != null) {
        team1.saikyoapps.model.I18nConfig cfg = i18nConfigMappaer.findByUserName(authentication.getName());
        String localeStr = (cfg == null || cfg.getLocale() == null) ? "ja" : cfg.getLocale();
        model.addAttribute("i18n", localeStr);
      } else {
        model.addAttribute("i18n", "ja");
      }
    }

    // 使用最終決定的 i18n 來取得 title 並放入 model
    String localeStr = (String) model.asMap().getOrDefault("i18n", "ja");
    Locale locale = "zh_TW".equals(localeStr) ? Locale.forLanguageTag("zh-TW")
        : ("en".equals(localeStr) ? Locale.forLanguageTag("en") : Locale.forLanguageTag("ja"));
    String usernameForTitle = (String) model.asMap().getOrDefault("username", "guest");
    String title = messageSource.getMessage("title", new Object[] { usernameForTitle }, locale);
    model.addAttribute("i18n_title", title);

    return "index";
  }

  @GetMapping("/login")
  public String login(@RequestParam Optional<String> error, Model model) {
    error.ifPresent(e -> model.addAttribute("error", true));
    return "login";
  }
}
