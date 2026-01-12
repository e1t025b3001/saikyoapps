package team1.saikyoapps.controller;

import java.util.Optional;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

      // DB からユーザの言語設定を読み出す（なければデフォルト ja）
      team1.saikyoapps.model.I18nConfig cfg = i18nConfigMappaer.findByUserName(user);
      String localeStr = (cfg == null || cfg.getLocale() == null) ? "ja" : cfg.getLocale();
      model.addAttribute("i18n", localeStr);

      // messageSource からタイトルを取得
      // サポートするロケールを列挙（zh_TW, en, ja）
      Locale locale;
      if ("zh_TW".equals(localeStr)) {
        locale = Locale.forLanguageTag("zh-TW");
      } else if ("en".equals(localeStr)) {
        locale = Locale.forLanguageTag("en");
      } else {
        locale = Locale.forLanguageTag("ja");
      }
      String title = messageSource.getMessage("title", new Object[] { user }, locale);
      model.addAttribute("i18n_title", title);

    } else {
      model.addAttribute("username", "guest");
      model.addAttribute("i18n", "ja");
      String title = messageSource.getMessage("title", new Object[] { "guest" }, Locale.forLanguageTag("ja"));
      model.addAttribute("i18n_title", title);
    }

    return "index";
  }

  @PostMapping("/i18n")
  public String setI18n(@RequestParam String i18n, Model model, Authentication authentication,
      HttpServletRequest request, HttpServletResponse response) {
    model.addAttribute("i18n", i18n);
    System.err.println("i18n set to: " + i18n);

    if (authentication != null) {
      String user = authentication.getName();
      model.addAttribute("username", user);

      // DB のレコードを取得して null チェック → 存在しなければ挿入、あれば更新する
      team1.saikyoapps.model.I18nConfig cfg = i18nConfigMappaer.findByUserName(user);
      if (cfg == null) {
        i18nConfigMappaer.insert(user, i18n);
        System.err.println("inserted i18n for user=" + user + " locale=" + i18n);
      } else {
        i18nConfigMappaer.updateLocaleByUserName(user, i18n);
        System.err.println("updated i18n for user=" + user + " locale=" + i18n);
      }

      // セッションの Locale を更新して、次回以降のリクエストに反映させる
      Locale locale;
      if ("zh_TW".equals(i18n)) {
        locale = Locale.forLanguageTag("zh-TW");
      } else if ("en".equals(i18n)) {
        locale = Locale.forLanguageTag("en");
      } else {
        locale = Locale.forLanguageTag("ja");
      }
      try {
        localeResolver.setLocale(request, response, locale);
      } catch (Exception e) {
        // setLocale は環境によって例外が出る場合があるため安全にログ出力する
        System.err.println("failed to set locale in resolver: " + e.getMessage());
      }

      // PRG: 設定後はリダイレクトして GET / で反映させる
      return "redirect:/";
    } else {
      model.addAttribute("username", "guest");
      model.addAttribute("i18n_title", "デフォルトタイトル");
      return "index";
    }
  }

  @GetMapping("/login")
  public String login(@RequestParam Optional<String> error, Model model) {
    error.ifPresent(e -> model.addAttribute("error", true));
    return "login";
  }
}
