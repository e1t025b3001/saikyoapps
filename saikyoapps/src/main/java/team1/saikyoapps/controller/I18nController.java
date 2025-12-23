package team1.saikyoapps.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import team1.saikyoapps.model.I18nConfig;
import team1.saikyoapps.model.I18nConfigMapper;

@Controller
public class I18nController {

  @Autowired
  I18nConfigMapper i18nConfigMapper;

  // 改為 /api/i18n 以避免與 HomeController 中的 /i18n POST 路徑衝突
  @PostMapping("/api/i18n")
  public String setI18n(@RequestParam("i18n") String locale, HttpServletRequest req, HttpServletResponse resp) {
    // set cookie
    Cookie cookie = new Cookie("i18n", locale);
    cookie.setPath("/");
    cookie.setMaxAge(60 * 60 * 24 * 365);
    resp.addCookie(cookie);

    // save to DB if authenticated
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
      String username = auth.getName();
      try {
        I18nConfig existing = i18nConfigMapper.findByUserName(username);
        if (existing == null) {
          i18nConfigMapper.insert(username, locale);
        } else {
          i18nConfigMapper.updateLocaleByUserName(username, locale);
        }
      } catch (Exception e) {
        // ignore DB errors for now
        e.printStackTrace();
      }
    }

    // store in session as well
    req.getSession().setAttribute("i18n", locale);

    String referer = req.getHeader("Referer");
    if (referer != null && !referer.isEmpty())
      return "redirect:" + referer;
    return "redirect:/";
  }
}
