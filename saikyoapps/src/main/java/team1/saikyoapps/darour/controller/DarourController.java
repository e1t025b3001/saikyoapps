package team1.saikyoapps.darour.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

  @GetMapping(value = "/darour", params = "!matchId")
  public String darourWhenMatchIdNotPresent(Model model, Authentication authentication) {
    // matchIdがない場合の処理

    if (authentication == null) {
      return "redirect:/";
    }

    String matchId = darourGame.selectDarourGameByPlayer(authentication.getName()).getGameID();

    if (matchId == null) {
      System.out.println("DEBUG: No matchId found for user: " + authentication.getName());
      return "darour.html";
    }

    // /darour?matchId=xxxx にリダイレクト
    return "redirect:/darour?matchId=" + matchId;
  }

  @GetMapping(value = "/darour", params = "matchId")
  public String darour(Model model, Authentication authentication, @RequestParam String matchId) {
    DarourGameState gameState = darourGameState.selectDarourGameStateByGameID(matchId);

    if (matchId == null || gameState == null) {
      // 追加ログ：DB側の現状を出力してアプリが見ている行を確認
      // System.out.println("DEBUG: matchId: " + matchId + ", gameState: " +
      // gameState);
      // System.out.println("DEBUG: darour_game rows:");
      // jdbcTemplate.queryForList("SELECT * FROM DAROUR_GAME").forEach(row ->
      // System.out.println(row));
      // System.out.println("DEBUG: darour_game_state rows (if exists):");
      // jdbcTemplate.queryForList("SELECT * FROM DAROUR_GAME_STATE").forEach(row ->
      // System.out.println(row));
      return "redirect:/";
    }

    model.addAttribute("username", authentication.getName());
    model.addAttribute("matchId", matchId);

    DarourGame game = darourGame.selectDarourGameByPlayer(authentication.getName());

    model.addAttribute("player1", game.getPlayer1());
    model.addAttribute("player2", game.getPlayer2());
    model.addAttribute("player3", game.getPlayer3());

    // model.addAttribute("hand", hand);

    return "darour.html";
  }
}
