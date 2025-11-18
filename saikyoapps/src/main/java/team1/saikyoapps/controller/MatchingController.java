package team1.saikyoapps.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MatchingController {

  @GetMapping("/matching")
  public String matching(@RequestParam(name = "game", required = false) String game, Model model,
      Authentication authentication) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }
    model.addAttribute("game", game != null ? game : "未選択");
    return "matching";
  }
}
