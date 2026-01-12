package team1.saikyoapps.darour.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import team1.saikyoapps.darour.dto.PlayRequestBody;
import team1.saikyoapps.darour.model.Card;
import team1.saikyoapps.darour.model.Combination;
import team1.saikyoapps.darour.model.DarourGame;
import team1.saikyoapps.darour.model.DarourGameMapper;
import team1.saikyoapps.darour.model.DarourGameState;
import team1.saikyoapps.darour.model.DarourGameStateMapper;
import team1.saikyoapps.darour.model.Hand;
import team1.saikyoapps.darour.service.DarourPlayTurnService;

@Controller
public class DarourController {
  @Autowired
  DarourGameMapper darourGame;

  @Autowired
  DarourGameStateMapper darourGameState;

  @Autowired
  DarourPlayTurnService darourPlayTurnService;

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

  @PostMapping("/darour")
  @ResponseBody
  public Map<String, String> play(Model model, Authentication authentication, @RequestParam String matchId,
      @RequestBody PlayRequestBody playRequestBody) {

    boolean success = darourPlayTurnService.play(
        matchId,
        playRequestBody.playerName,
        Combination.deserialize(playRequestBody.serializedCombination));

    return Map.of("success", success + "");
  }

  @GetMapping("/darour/{matchId}/state")
  @ResponseBody
  public Map<String, Object> getGameState(@PathVariable String matchId, Authentication authentication) {
    Map<String, Object> response = new HashMap<>();

    if (authentication == null) {
      response.put("error", "unauthorized");
      return response;
    }

    DarourGameState gameState = darourGameState.selectDarourGameStateByGameID(matchId);
    if (gameState == null) {
      response.put("error", "game_not_found");
      return response;
    }

    DarourGame game = darourGame.selectDarourGameByPlayer(authentication.getName());
    if (game == null) {
      response.put("error", "player_not_found");
      return response;
    }

    String username = authentication.getName();
    int playerIndex = game.getPlayer1().equals(username) ? 0
        : game.getPlayer2().equals(username) ? 1 : 2;

    Hand player1Hand = gameState.getPlayer1Hand();
    Hand player2Hand = gameState.getPlayer2Hand();
    Hand player3Hand = gameState.getPlayer3Hand();

    // 手札がnullの場合はエラーを返す
    if (player1Hand == null || player2Hand == null || player3Hand == null) {
      response.put("error", "invalid_game_state");
      return response;
    }

    Hand myHand = playerIndex == 0 ? player1Hand
        : playerIndex == 1 ? player2Hand
            : player3Hand;

    response.put("matchId", matchId);
    response.put("player1", game.getPlayer1());
    response.put("player2", game.getPlayer2());
    response.put("player3", game.getPlayer3());
    response.put("currentPlayerIndex", gameState.getCurrentPlayerIndex());
    response.put("lastPlayedPlayerIndex", gameState.getLastPlayedPlayerIndex());
    response.put("myPlayerIndex", playerIndex);
    response.put("isMyTurn", gameState.getCurrentPlayerIndex().equals(playerIndex));

    List<Map<String, String>> handCards = new ArrayList<>();
    for (Card card : myHand.getCards()) {
      Map<String, String> cardData = new HashMap<>();
      cardData.put("serialized", card.serialize());
      cardData.put("suit", card.getSuit().serialize());
      cardData.put("rank", card.getRank().serialize());
      handCards.add(cardData);
    }
    response.put("myHand", handCards);

    response.put("player1HandCount", player1Hand.getCards().size());
    response.put("player2HandCount", player2Hand.getCards().size());
    response.put("player3HandCount", player3Hand.getCards().size());

    Combination tableCombination = gameState.getTableCombination();
    if (tableCombination != null) {
      Map<String, Object> tableData = new HashMap<>();
      tableData.put("serialized", tableCombination.serialize());
      tableData.put("type", tableCombination.result.toString());
      List<Map<String, String>> tableCards = new ArrayList<>();
      for (Card card : tableCombination.cards) {
        Map<String, String> cardData = new HashMap<>();
        cardData.put("serialized", card.serialize());
        cardData.put("suit", card.getSuit().serialize());
        cardData.put("rank", card.getRank().serialize());
        tableCards.add(cardData);
      }
      tableData.put("cards", tableCards);
      response.put("tableCombination", tableData);
    }

    int[] handCounts = {
        player1Hand.getCards().size(),
        player2Hand.getCards().size(),
        player3Hand.getCards().size()
    };
    boolean finished = false;
    String winner = null;
    for (int i = 0; i < 3; i++) {
      if (handCounts[i] == 0) {
        finished = true;
        winner = i == 0 ? game.getPlayer1() : i == 1 ? game.getPlayer2() : game.getPlayer3();
        break;
      }
    }
    response.put("finished", finished);
    response.put("winner", winner);

    return response;
  }

  @PostMapping("/darour/{matchId}/pass")
  @ResponseBody
  public Map<String, String> pass(@PathVariable String matchId, Authentication authentication) {
    if (authentication == null) {
      return Map.of("success", "false", "error", "unauthorized");
    }

    boolean success = darourPlayTurnService.pass(matchId, authentication.getName());
    return Map.of("success", success + "");
  }
}
