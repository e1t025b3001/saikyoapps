package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import team1.saikyoapps.model.MatchingQueue;
import team1.saikyoapps.model.MatchingQueueMapper;
import team1.saikyoapps.model.PlayerStatus;
import team1.saikyoapps.model.GomokuGameMapper;
import team1.saikyoapps.model.GomokuGame;
import team1.saikyoapps.model.GomokuMoveMapper;
import team1.saikyoapps.model.MarubatsuGameMapper;
import team1.saikyoapps.model.MarubatsuGame;
import team1.saikyoapps.model.MarubatsuMoveMapper;
import team1.saikyoapps.model.MatchHistoryMapper;
import java.sql.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MatchingController {
  private static final Logger logger = LoggerFactory.getLogger(MatchingController.class);

  @Autowired
  MatchingQueueMapper matchingQueueMapper;

  @Autowired
  GomokuGameMapper gomokuGameMapper;

  @Autowired
  GomokuMoveMapper gomokuMoveMapper;

  @Autowired
  MarubatsuGameMapper marubatsuGameMapper;

  @Autowired
  MarubatsuMoveMapper marubatsuMoveMapper;

  @Autowired
  MatchHistoryMapper historyMapper;

  @GetMapping("/matching")
  public String matching(@RequestParam(name = "game", required = false) String game, Model model,
      Authentication authentication) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }

    PlayerStatus ps = null;
    if (authentication != null) {
      ps = matchingQueueMapper.findPlayerStatus(authentication.getName());
    }

    // 如果玩家狀態已經是 playing，直接導向配對成功頁面（以 players_status 的 current_game 為準）
    if (ps != null && "playing".equals(ps.getStatus())) {
      model.addAttribute("game", ps.getCurrentGame());
      model.addAttribute("waitingCount", matchingQueueMapper.countWaitingPlayersByGame(ps.getCurrentGame()));

      // marubatsu の場合、matchId を作成または取得してテンプレートへ渡す
      if ("marubatsu".equals(ps.getCurrentGame()) && authentication != null) {
        MarubatsuGame mg = marubatsuGameMapper.findByPlayer(authentication.getName());
        if (mg == null) {
          String newGameId = UUID.randomUUID().toString();
          List<String> playingUsers = matchingQueueMapper.findPlayingUsersByGame("marubatsu");
          String playerX = null;
          String playerO = null;
          if (playingUsers.size() >= 1)
            playerX = playingUsers.get(0);
          if (playingUsers.size() >= 2)
            playerO = playingUsers.get(1);
          if (playerX == null)
            playerX = authentication.getName();
          if (playerO == null)
            playerO = "";
          marubatsuGameMapper.insert(newGameId, playerX, playerO, null, "X", "playing");
          logger.info("Created marubatsu game {} players: {}/{}", newGameId, playerX, playerO);
          mg = marubatsuGameMapper.findByGameId(newGameId);
        }
        if (mg != null)
          model.addAttribute("matchId", mg.getGameId());
      }

      return "match_success";
    }

    if (authentication != null && game != null) {
      // 檢查該使用者是否已有活動配對或正在遊玩
      int active = matchingQueueMapper.countActiveByUser(authentication.getName());
      boolean inActivity = active > 0
          || (ps != null && ("matching".equals(ps.getStatus()) || "playing".equals(ps.getStatus())));
      if (!inActivity) {
        // 新增等待紀錄，並設定 players_status 為 matching
        matchingQueueMapper.insert(authentication.getName(), game, "WAITING");
        // 如果 players_status 尚未存在，插入，否則更新
        if (ps == null) {
          matchingQueueMapper.insertPlayerStatus(authentication.getName(), "matching", game);
        } else {
          matchingQueueMapper.updatePlayerStatus(authentication.getName(), "matching", game);
        }
        // 重新讀取 players_status
        ps = matchingQueueMapper.findPlayerStatus(authentication.getName());
      }
    }

    int requiredPlayers = 2;
    if ("shinkeisuijaku".equals(game) || "uno".equals(game) || "dairo".equals(game)) {
      requiredPlayers = 3;
    }

    int waitingCount = matchingQueueMapper.countWaitingPlayersByGame(game);
    model.addAttribute("waitingCount", waitingCount);
    model.addAttribute("game", game != null ? game : "未選択");

    // 嘗試配對：只在等待人數足夠時，從等待隊列中選出所需玩家並標記為 MATCHED
    if (waitingCount >= requiredPlayers && game != null) {
      List<MatchingQueue> waiters = matchingQueueMapper.findFirstNWaitingByGame(game, requiredPlayers);
      if (waiters.size() >= requiredPlayers) {
        // 選出前 requiredPlayers 名並標記為 MATCHED，並更新 players_status 為 playing
        for (int i = 0; i < requiredPlayers; i++) {
          MatchingQueue mq = waiters.get(i);
          matchingQueueMapper.updateStatusById(mq.getId(), "MATCHED");
          matchingQueueMapper.updatePlayerStatus(mq.getUserName(), "playing", game);
          // 由於玩家已進入 playing，從 matching_queue 中移除該玩家的等待紀錄，避免在 per-game view 中看到他們
          matchingQueueMapper.deleteByUserAndGame(mq.getUserName(), game);
        }
        // 在 model 中放入 game，確保直接回傳 match_success 時能正確導向
        model.addAttribute("game", game);

        // marubatsu の場合はここで matchId を作成して template に渡す
        if ("marubatsu".equals(game)) {
          String newGameId = UUID.randomUUID().toString();
          List<String> playingUsers = matchingQueueMapper.findPlayingUsersByGame("marubatsu");
          String playerX = null;
          String playerO = null;
          if (playingUsers.size() >= 1)
            playerX = playingUsers.get(0);
          if (playingUsers.size() >= 2)
            playerO = playingUsers.get(1);
          if (playerX == null && authentication != null)
            playerX = authentication.getName();
          if (playerX == null)
            playerX = "";
          if (playerO == null)
            playerO = "";
          try {
            marubatsuGameMapper.insert(newGameId, playerX, playerO, null, "X", "playing");
            logger.info("Created marubatsu game (matchSuccess) {} players: {}/{}", newGameId, playerX, playerO);
            model.addAttribute("matchId", newGameId);
          } catch (Exception ex) {
            logger.warn("Failed to insert marubatsu_game for match: {}", ex.getMessage());
          }
        }

        return "match_success"; // 需建立 match_success.html
      }
    }

    return "matching";
  }

  // 新增 JSON 狀態 API，供前端輪詢使用
  @GetMapping("/matching/status")
  @ResponseBody
  public Map<String, Object> matchingStatus(@RequestParam(name = "game", required = false) String game,
      Authentication authentication) {
    Map<String, Object> res = new HashMap<>();
    if (authentication == null) {
      res.put("status", "guest");
      res.put("waitingCount", 0);
      return res;
    }
    String user = authentication.getName();
    PlayerStatus ps = matchingQueueMapper.findPlayerStatus(user);
    String status = (ps != null) ? ps.getStatus() : "lobby";
    String currentGame = (ps != null) ? ps.getCurrentGame() : null;

    // 如果前端指定 game，優先計算該 game 的等待數
    String targetGame = (currentGame != null) ? currentGame : game;
    int waitingCount = 0;
    if (targetGame != null) {
      waitingCount = matchingQueueMapper.countWaitingPlayersByGame(targetGame);
    }

    res.put("status", status);
    res.put("currentGame", currentGame);
    res.put("waitingCount", waitingCount);

    // 若玩家已在 playing 並且是 gomoku，嘗試找出該玩家的 gomoku_game 並回傳 gameId, turn, myColor
    if ("playing".equals(status) && "gomoku".equals(targetGame)) {
      GomokuGame gg = gomokuGameMapper.findByPlayer(user);
      if (gg == null) {
        // 尚未建立 game session，嘗試建立（產生 UUID）
        String newGameId = UUID.randomUUID().toString();
        // 取得同一遊戲的正在 playing 的玩家（取前兩名）
        List<String> playingUsers = matchingQueueMapper.findPlayingUsersByGame("gomoku");
        String playerBlack = null;
        String playerWhite = null;
        if (playingUsers.size() >= 1)
          playerBlack = playingUsers.get(0);
        if (playingUsers.size() >= 2)
          playerWhite = playingUsers.get(1);
        // 如無對手則留空
        if (playerBlack == null)
          playerBlack = user;
        if (playerWhite == null)
          playerWhite = "";
        gomokuGameMapper.insert(newGameId, playerBlack, playerWhite, null, "black", "playing");
        gg = gomokuGameMapper.findByGameId(newGameId);
      }
      res.put("gameId", gg.getGameId());
      res.put("turn", gg.getTurn());
      // myColor 以 playerBlack/playerWhite 判定
      if (user.equals(gg.getPlayerBlack()))
        res.put("myColor", "black");
      else if (user.equals(gg.getPlayerWhite()))
        res.put("myColor", "white");
    }

    // 若玩家已在 playing 並且是 marubatsu，嘗試找出或建立 marubatsu_game 並回傳 gameId
    if ("playing".equals(status) && "marubatsu".equals(targetGame)) {
      MarubatsuGame mg = marubatsuGameMapper.findByPlayer(user);
      if (mg == null) {
        String newGameId = UUID.randomUUID().toString();
        List<String> playingUsers = matchingQueueMapper.findPlayingUsersByGame("marubatsu");
        String playerX = null;
        String playerO = null;
        if (playingUsers.size() >= 1)
          playerX = playingUsers.get(0);
        if (playingUsers.size() >= 2)
          playerO = playingUsers.get(1);
        if (playerX == null)
          playerX = user;
        if (playerO == null)
          playerO = "";
        try {
          marubatsuGameMapper.insert(newGameId, playerX, playerO, null, "X", "playing");
        } catch (Exception ex) {
          logger.warn("Failed to insert marubatsu_game in matchingStatus: {}", ex.getMessage());
        }
        mg = marubatsuGameMapper.findByGameId(newGameId);
      }
      if (mg != null) {
        res.put("gameId", mg.getGameId());
        res.put("turn", mg.getTurn());
        if (user.equals(mg.getPlayerX()))
          res.put("mySymbol", "X");
        else if (user.equals(mg.getPlayerO()))
          res.put("mySymbol", "O");
      }
    }

    return res;
  }

  // 使用者在遊戲中按下棄權（或離開）時，通知後端：該使用者視為敗者，其他人勝利
  @PostMapping("/match/forfeit")
  public String forfeit(Model model, Authentication authentication, @RequestParam(name = "game") String game) {
    if (authentication == null) {
      return "redirect:/";
    }
    String user = authentication.getName();
    // 取得其他正在 playing 的玩家
    List<String> playing = matchingQueueMapper.findPlayingUsersByGame(game);
    String winner = null;
    for (String p : playing) {
      if (!p.equals(user)) {
        winner = p;
        break;
      }
    }
    if (winner == null) {
      // 沒有對手，直接回到 lobby
      matchingQueueMapper.updatePlayerStatus(user, "lobby", null);
      model.addAttribute("game", game);
      model.addAttribute("winner", "");
      model.addAttribute("loser", user);
      return "match_result";
    }

    // 刪除 matching_queue 的紀錄（保險）
    matchingQueueMapper.deleteByUserAndGame(user, game);
    matchingQueueMapper.deleteByUserAndGame(winner, game);

    // 若是 marubatsu，刪除即時對戰紀錄、寫入 history 並刪除或標記 game
    if ("marubatsu".equals(game)) {
      try {
        MarubatsuGame mgUser = marubatsuGameMapper.findByPlayer(user);
        MarubatsuGame mgWinner = marubatsuGameMapper.findByPlayer(winner);
        MarubatsuGame mg = (mgUser != null) ? mgUser : mgWinner;
        String mid = null;
        if (mg != null)
          mid = mg.getGameId();
        // 刪除 moves
        if (mid != null) {
          try {
            marubatsuMoveMapper.deleteByGameId(mid);
          } catch (Exception ex) {
            logger.warn("Failed to delete marubatsu_move for {}: {}", mid, ex.getMessage());
          }
          try {
            marubatsuGameMapper.deleteByGameId(mid);
          } catch (Exception ex) {
            logger.warn("Failed to delete marubatsu_game {}: {}", mid, ex.getMessage());
          }
        }
        // 寫入 match_history
        try {
          String px = (mg != null) ? mg.getPlayerX() : null;
          String po = (mg != null) ? mg.getPlayerO() : null;
          historyMapper.insert("marubatsu", px, po, winner, null, new Timestamp(System.currentTimeMillis()), "forfeit");
        } catch (Exception ex) {
          logger.warn("Failed to insert match_history for marubatsu forfeit: {}", ex.getMessage());
        }
      } catch (Exception ex) {
        logger.warn("Error cleaning up marubatsu game on forfeit: {}", ex.getMessage());
      }
    }

    // 刪除 gomoku_move 的紀錄
    // gomoku の場合、該当する gameId を取得して moves を削除する
    GomokuGame ggUser = gomokuGameMapper.findByPlayer(user);
    GomokuGame ggWinner = gomokuGameMapper.findByPlayer(winner);
    String gomokuGameId = null;
    if (ggUser != null) {
      gomokuGameId = ggUser.getGameId();
    } else if (ggWinner != null) {
      gomokuGameId = ggWinner.getGameId();
    }
    if (gomokuGameId != null) {
      gomokuMoveMapper.deleteByGameId(gomokuGameId);
    }

    // 更新 players_status
    matchingQueueMapper.updatePlayerStatus(user, "lobby", null);
    matchingQueueMapper.updatePlayerStatus(winner, "lobby", null);

    model.addAttribute("game", game);
    model.addAttribute("winner", winner);
    model.addAttribute("loser", user);
    return "match_result";
  }

  // 各遊戲頁面的 GET endpoint，避免 Whitelabel Error Page
  @GetMapping("/marubatsu")
  public String marubatsu(
      @org.springframework.web.bind.annotation.RequestParam(name = "matchId", required = false) String matchId,
      Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "marubatsu");
    if (matchId != null && !matchId.isEmpty()) {
      model.addAttribute("matchId", matchId);
    }
    return "marubatsu";
  }

  @GetMapping("/othello")
  public String othello(Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "othello");
    return "othello";
  }

  @GetMapping("/gomoku")
  public String gomoku(Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "gomoku");
    // 新增日誌，紀錄進入 gomoku 頁面的資訊
    logger.info("Rendering gomoku page for user={}", authentication != null ? authentication.getName() : "guest");
    return "gomoku";
  }

  @GetMapping("/shinkeisuijaku")
  public String shinkeisuijaku(Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "shinkeisuijaku");
    return "shinkeisuijaku";
  }

  @GetMapping("/uno")
  public String uno(Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "uno");
    return "uno";
  }

  @GetMapping("/dairo")
  public String dairo(Model model, Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", "dairo");
    return "dairo";
  }

  // 使用者從匹配或遊戲頁返回 LOBBY 的 API
  @PostMapping("/match/leave")
  public String leaveMatch(@RequestParam(name = "game", required = false) String game, Model model,
      Authentication authentication) {
    if (authentication != null) {
      String user = authentication.getName();
      // 刪除 matching_queue 中的等待紀錄
      if (game != null) {
        matchingQueueMapper.deleteByUserAndGame(user, game);
      }

      // 如果是 gomoku，嘗試清除該局的 moves 與 gomoku_game，並將對手也回到 lobby
      if ("gomoku".equals(game)) {
        try {
          GomokuGame gg = gomokuGameMapper.findByPlayer(user);
          if (gg != null) {
            String gid = gg.getGameId();
            // 刪除 moves
            try {
              gomokuMoveMapper.deleteByGameId(gid);
            } catch (Exception ex) {
              logger.warn("Failed to delete gomoku_move for {}: {}", gid, ex.getMessage());
            }
            // 刪除 gomoku_game
            try {
              gomokuGameMapper.deleteByGameId(gid);
            } catch (Exception ex) {
              logger.warn("Failed to delete gomoku_game {}: {}", gid, ex.getMessage());
            }
            // 若有對手，將對手狀態回 lobby 並刪除 matching_queue 的殘留
            String opp = null;
            if (user.equals(gg.getPlayerBlack()))
              opp = gg.getPlayerWhite();
            else if (user.equals(gg.getPlayerWhite()))
              opp = gg.getPlayerBlack();
            if (opp != null && !opp.isEmpty()) {
              try {
                matchingQueueMapper.deleteByUserAndGame(opp, "gomoku");
                matchingQueueMapper.updatePlayerStatus(opp, "lobby", null);
              } catch (Exception ex) {
                logger.warn("Failed to cleanup opponent {} after leave: {}", opp, ex.getMessage());
              }
            }
          }
        } catch (Exception ex) {
          logger.warn("Error while cleaning up gomoku game on leave for {}: {}", user, ex.getMessage());
        }
      }

      // 更新 players_status 為 lobby
      matchingQueueMapper.updatePlayerStatus(user, "lobby", null);
    }
    return "redirect:/";
  }

  // 配對成功後，遊戲頁面會向後端發起結束請求，或放棄/結束時呼叫
  @PostMapping("/match/end")
  public String endMatch(@RequestParam("winner") String winner, @RequestParam("loser") String loser,
      @RequestParam("game") String game, Model model) {
    // 刪除這兩位玩家在該遊戲的紀錄
    matchingQueueMapper.deleteByUserAndGame(winner, game);
    matchingQueueMapper.deleteByUserAndGame(loser, game);

    // 更新 players_status 為 lobby
    matchingQueueMapper.updatePlayerStatus(winner, "lobby", null);
    matchingQueueMapper.updatePlayerStatus(loser, "lobby", null);

    model.addAttribute("winner", winner);
    model.addAttribute("loser", loser);
    model.addAttribute("game", game);

    return "match_result";
  }

  @GetMapping("/match_success")
  public String matchSuccess(@RequestParam(name = "game", required = false) String game, Model model,
      Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());

    // 若沒有傳入 game，嘗試從 players_status 取得該使用者的 currentGame
    if (game == null && authentication != null) {
      PlayerStatus ps = matchingQueueMapper.findPlayerStatus(authentication.getName());
      if (ps != null && ps.getCurrentGame() != null) {
        game = ps.getCurrentGame();
      }
    }

    // marubatsu の場合、matchId を作成または取得してテンプレートへ渡す
    if ("marubatsu".equals(game) && authentication != null) {
      MarubatsuGame mg = marubatsuGameMapper.findByPlayer(authentication.getName());
      if (mg == null) {
        String newGameId = UUID.randomUUID().toString();
        List<String> playingUsers = matchingQueueMapper.findPlayingUsersByGame("marubatsu");
        String playerX = null;
        String playerO = null;
        if (playingUsers.size() >= 1)
          playerX = playingUsers.get(0);
        if (playingUsers.size() >= 2)
          playerO = playingUsers.get(1);
        if (playerX == null)
          playerX = authentication.getName();
        if (playerO == null)
          playerO = "";
        marubatsuGameMapper.insert(newGameId, playerX, playerO, null, "X", "playing");
        logger.info("Created marubatsu game (matchSuccess) {} players: {}/{}", newGameId, playerX, playerO);
        mg = marubatsuGameMapper.findByGameId(newGameId);
      }
      if (mg != null)
        model.addAttribute("matchId", mg.getGameId());
    }

    model.addAttribute("game", game != null ? game : "marubatsu");
    return "match_success";
  }

  @GetMapping("/match_result")
  public String matchResult(@RequestParam(name = "game", required = false) String game,
      @RequestParam(name = "winner", required = false) String winner,
      @RequestParam(name = "loser", required = false) String loser, Model model,
      Authentication authentication) {
    if (authentication != null)
      model.addAttribute("username", authentication.getName());
    model.addAttribute("game", game != null ? game : "未選択");
    model.addAttribute("winner", winner);
    model.addAttribute("loser", loser);
    return "match_result";
  }

  @GetMapping("/tictactoe")
  public String tictactoe(Model model, Authentication authentication) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }
    return "tictactoe";
  }
}
