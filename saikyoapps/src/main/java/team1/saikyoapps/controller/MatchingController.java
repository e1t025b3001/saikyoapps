package team1.saikyoapps.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import team1.saikyoapps.service.MatchingService;
import team1.saikyoapps.service.GameSession;

@Controller
public class MatchingController {

  @Autowired
  MatchingService matchingService;

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

  @PostMapping(path = "/matching/join")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> join(@RequestBody Map<String, String> body,
      Authentication authentication) {
    String game = body.get("game");
    String username = authentication != null ? authentication.getName() : "guest";
    String sessionId = matchingService.joinQueue(game, username);
    Map<String, Object> res = new HashMap<>();
    if (sessionId != null) {
      // マッチ成立
      res.put("matched", true);
      res.put("url", "/game/" + game + "/" + sessionId);
    } else {
      res.put("matched", false);
    }
    return ResponseEntity.ok(res);
  }

  @GetMapping(path = "/matching/status")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> status(@RequestParam(name = "game", required = false) String game,
      Authentication authentication) {
    String username = authentication != null ? authentication.getName() : "guest";
    String sessionId = matchingService.checkSessionForUser(username);
    Map<String, Object> res = new HashMap<>();
    if (sessionId != null) {
      res.put("matched", true);
      GameSession gs = matchingService.getSession(sessionId);
      res.put("url", "/game/" + gs.getGame() + "/" + sessionId);
    } else {
      res.put("matched", false);
    }
    return ResponseEntity.ok(res);
  }
}
