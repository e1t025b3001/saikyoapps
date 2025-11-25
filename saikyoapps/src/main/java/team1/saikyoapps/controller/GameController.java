package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import team1.saikyoapps.service.GameSession;
import team1.saikyoapps.service.MatchingService;

@Controller
public class GameController {

  @Autowired
  MatchingService matchingService;

  @GetMapping("/game/{game}/{sessionId}")
  public String game(@PathVariable String game, @PathVariable String sessionId, Model model,
      Authentication authentication) {
    GameSession gs = matchingService.getSession(sessionId);
    if (gs == null) {
      model.addAttribute("error", "セッションが見つかりません");
      return "matching";
    }
    model.addAttribute("game", game);
    model.addAttribute("sessionId", sessionId);
    model.addAttribute("players", gs.getPlayers());
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }
    return "game";
  }
}
