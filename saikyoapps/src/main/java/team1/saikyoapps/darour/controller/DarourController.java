package team1.saikyoapps.darour.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import team1.saikyoapps.darour.model.DarourGame;
import team1.saikyoapps.darour.model.DarourGameMapper;
import team1.saikyoapps.darour.model.DarourGameState;
import team1.saikyoapps.darour.model.DarourGameStateMapper;

@Controller
public class DarourController {
  @Autowired
  DarourGameMapper darourGame;

  @Autowired
  DarourGameStateMapper darourGameState;

  // TODO gameIDを知らないがプレイヤーの場合の処理

  @GetMapping("/darour/{gameID}")
  public String darour(Model model, Authentication authentication, @PathVariable String gameID) {
    DarourGameState gameState = darourGameState.selectDarourGameStateByGameID(gameID);

    if (gameID == null || gameState == null) {
      return "redirect:/";
    }

    model.addAttribute("username", authentication.getName());

    DarourGame game = darourGame.selectDarourGameByPlayer(authentication.getName()).getFirst();

    model.addAttribute("player1", game.getPlayer1());
    model.addAttribute("player2", game.getPlayer2());
    model.addAttribute("player3", game.getPlayer3());

    // model.addAttribute("hand", hand);

    return "darour.html";
  }
}
