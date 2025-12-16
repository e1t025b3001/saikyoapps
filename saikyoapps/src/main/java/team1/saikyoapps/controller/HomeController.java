package team1.saikyoapps.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

  @GetMapping("/")
  public String index(Model model, Authentication authentication) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }

    return "index";
  }

  @PostMapping("/i18n")
  public String setI18n(@RequestParam String i18n, Model model, Authentication authentication) {
    model.addAttribute("i18n", i18n);
    System.err.println("i18n set to: " + i18n);
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }
    return "index";

  }

  @GetMapping("/login")
  public String login(@RequestParam Optional<String> error, Model model) {
    error.ifPresent(e -> model.addAttribute("error", true));
    return "login";
  }
}
