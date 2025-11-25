package team1.saikyoapps.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DarourController {
  @GetMapping("/darour")
  public String darour(Model model,
      Authentication authentication) {

    model.addAttribute("username", authentication.getName());
    return "darour";
  }
}
