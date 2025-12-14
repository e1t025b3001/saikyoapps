package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import team1.saikyoapps.model.GomokuGame;
import team1.saikyoapps.model.GomokuGameMapper;
import team1.saikyoapps.model.GomokuMoveMapper;
import team1.saikyoapps.model.MatchHistoryMapper;
import team1.saikyoapps.model.MatchingQueueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

@RestController
@RequestMapping("/gomoku")
public class GomokuController {

  private static final Logger logger = LoggerFactory.getLogger(GomokuController.class);

  @Autowired
  GomokuGameMapper gameMapper;

  @Autowired
  GomokuMoveMapper moveMapper;

  @Autowired
  MatchHistoryMapper historyMapper;

  @Autowired
  MatchingQueueMapper matchingQueueMapper;

  // 観戦一覧（対局中のみ）
  @GetMapping("/spectate/list")
  public ResponseEntity<List<Map<String, Object>>> listSpectateGames(Authentication authentication) {
    try {
      if (authentication == null) {
        return ResponseEntity.status(401).build();
      }
      List<GomokuGame> games = gameMapper.findPlayingGames();
      List<Map<String, Object>> res = new ArrayList<>();
      for (GomokuGame g : games) {
        res.add(Map.of(
            "gameId", g.getGameId(),
            "playerBlack", g.getPlayerBlack(),
            "playerWhite", g.getPlayerWhite()));
      }
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in listSpectateGames", ex);
      return ResponseEntity.status(500).build();
    }
  }

  // 取得遊戲狀態
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId, Authentication authentication) {
    try {
      GomokuGame g = gameMapper.findByGameId(gameId);
      if (g == null)
        return ResponseEntity.notFound().build();
      Map<String, Object> res = new HashMap<>();

      // boardState 儲存為 JSON 字串或 null
      if (g.getBoardState() != null && !g.getBoardState().isEmpty()) {
        try {
          com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
          int[][] board = om.readValue(g.getBoardState(), int[][].class);
          res.put("board", board);
        } catch (Exception e) {
          logger.error("Failed to parse boardState for game {}", gameId, e);
          res.put("board", null);
        }
      }

      res.put("turn", g.getTurn());
      res.put("finished", "finished".equals(g.getStatus()));
      res.put("playerBlack", g.getPlayerBlack());
      res.put("playerWhite", g.getPlayerWhite());

      // 若有登入使用者，回傳 myColor / mode
      String mode = "spectator";
      if (authentication != null) {
        String user = authentication.getName();
        if (user.equals(g.getPlayerBlack())) {
          res.put("myColor", "black");
          mode = "player";
        } else if (user.equals(g.getPlayerWhite())) {
          res.put("myColor", "white");
          mode = "player";
        }
      }
      res.put("mode", mode);

      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in getGame for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

  // 落子 API
  @PostMapping("/{gameId}/move")
  public ResponseEntity<Map<String, Object>> postMove(@PathVariable String gameId,
      @RequestBody Map<String, Object> body, Authentication authentication) {
    try {
      GomokuGame g = gameMapper.findByGameId(gameId);
      if (g == null)
        return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      int x = ((Number) body.getOrDefault("x", -1)).intValue();
      int y = ((Number) body.getOrDefault("y", -1)).intValue();
      if (x < 0 || y < 0)
        return ResponseEntity.badRequest().body(Map.of("error", "invalid coords"));

      // 解析 board
      int[][] board;
      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        if (g.getBoardState() == null || g.getBoardState().isEmpty()) {
          board = new int[15][15];
        } else {
          board = om.readValue(g.getBoardState(), int[][].class);
        }
      } catch (Exception e) {
        logger.error("Failed to parse board state for {}", gameId, e);
        board = new int[15][15];
      }

      // 決定玩家顏色
      String playerName = "unknown";
      if (authentication != null)
        playerName = authentication.getName();

      String color = null;
      if (playerName.equals(g.getPlayerWhite()))
        color = "white";
      else if (playerName.equals(g.getPlayerBlack()))
        color = "black";

      // 観戦者（プレイヤー以外）の操作は禁止
      if (color == null) {
        return ResponseEntity.status(403).body(Map.of("error", "player not in game"));
      }

      int colorVal = "black".equals(color) ? 1 : 2;

      // 簡易ルール：如果目前回合未設定，接受第一個動作玩家的顏色
      if (g.getTurn() == null || g.getTurn().isEmpty()) {
        g.setTurn(color);
      }

      // 驗證是否是該玩家回合
      if (g.getTurn() == null || !g.getTurn().equals(color)) {
        return ResponseEntity.status(409).body(Map.of("error", "not your turn"));
      }

      if (board[y][x] != 0)
        return ResponseEntity.status(409).body(Map.of("error", "cell occupied"));

      // 寫入 move
      int moveNo = moveMapper.countByGameId(gameId) + 1;
      moveMapper.insert(gameId, playerName, x, y, moveNo);
      board[y][x] = colorVal;

      // 檢查勝利
      boolean win = false;
      int dirs[][] = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
      outer: for (int[] d : dirs) {
        int cnt = 1;
        for (int k = 1; k < 5; k++) {
          int nr = y + k * d[1];
          int nc = x + k * d[0];
          if (nr < 0 || nc < 0 || nr >= 15 || nc >= 15)
            break;
          if (board[nr][nc] == colorVal)
            cnt++;
          else
            break;
        }
        for (int k = 1; k < 5; k++) {
          int nr = y - k * d[1];
          int nc = x - k * d[0];
          if (nr < 0 || nc < 0 || nr >= 15 || nc >= 15)
            break;
          if (board[nr][nc] == colorVal)
            cnt++;
          else
            break;
        }
        if (cnt >= 5) {
          win = true;
          break outer;
        }
      }

      // 更新 game board / turn / status
      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        String newBoardJson = om.writeValueAsString(board);
        String nextTurn = "black".equals(g.getTurn()) ? "white" : "black";

        if (!win) {
          g.setBoardState(newBoardJson);
          g.setTurn(nextTurn);
          gameMapper.update(g.getGameId(), newBoardJson, g.getTurn(), g.getStatus());
        } else {
          // DB運用方針A: 盤面と moves を保持し、status=finished にする
          g.setStatus("finished");
          g.setBoardState(newBoardJson);
          g.setTurn(null);

          try {
            historyMapper.insert("gomoku", g.getPlayerBlack(), g.getPlayerWhite(), color, null,
                new Timestamp(System.currentTimeMillis()), null);
          } catch (Exception ex) {
            logger.warn("Failed to insert match_history for win {}: {}", gameId, ex.getMessage());
          }

          try {
            gameMapper.update(g.getGameId(), newBoardJson, null, g.getStatus());
          } catch (Exception ex) {
            logger.warn("Failed to update game {} to finished: {}", gameId, ex.getMessage());
          }

          // 更新 players_status 回 lobby
          try {
            if (g.getPlayerBlack() != null && !g.getPlayerBlack().isEmpty()) {
              matchingQueueMapper.deleteByUserAndGame(g.getPlayerBlack(), "gomoku");
              matchingQueueMapper.updatePlayerStatus(g.getPlayerBlack(), "lobby", null);
            }
            if (g.getPlayerWhite() != null && !g.getPlayerWhite().isEmpty()) {
              matchingQueueMapper.deleteByUserAndGame(g.getPlayerWhite(), "gomoku");
              matchingQueueMapper.updatePlayerStatus(g.getPlayerWhite(), "lobby", null);
            }
          } catch (Exception ex) {
            logger.warn("Failed to cleanup player status after game end: {}", ex.getMessage());
          }

          Map<String, Object> resWin = new HashMap<>();
          resWin.put("board", board);
          resWin.put("turn", null);
          resWin.put("finished", true);
          resWin.put("winner", color);
          return ResponseEntity.ok(resWin);
        }
      } catch (Exception e) {
        logger.error("Failed to update game {}", gameId, e);
        return ResponseEntity.status(500).body(Map.of("error", "server error"));
      }

      Map<String, Object> res = new HashMap<>();
      res.put("board", board);
      res.put("turn", g.getTurn());
      res.put("finished", win);
      if (win)
        res.put("winner", color);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in postMove for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

  // 玩家認輸 API：紀錄 match_history、刪除 moves、更新狀態
  @PostMapping("/{gameId}/forfeit")
  public ResponseEntity<Map<String, Object>> forfeitGame(@PathVariable String gameId, Authentication authentication) {
    try {
      GomokuGame g = gameMapper.findByGameId(gameId);
      if (g == null)
        return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      String loser = (authentication != null) ? authentication.getName() : "unknown";
      String winner = null;
      if (g.getPlayerBlack() != null && !g.getPlayerBlack().isEmpty() && !g.getPlayerBlack().equals(loser))
        winner = g.getPlayerBlack();
      if (winner == null && g.getPlayerWhite() != null && !g.getPlayerWhite().isEmpty()
          && !g.getPlayerWhite().equals(loser))
        winner = g.getPlayerWhite();

      // 記錄 match_history
      try {
        historyMapper.insert("gomoku", g.getPlayerBlack(), g.getPlayerWhite(), winner, null,
            new Timestamp(System.currentTimeMillis()), "forfeit");
      } catch (Exception ex) {
        logger.warn("Failed to insert match_history for forfeit {}", ex.getMessage());
      }

      // DB運用方針A: moves/盤面は削除せず、status=finished にする
      try {
        g.setStatus("finished");
        // 投了時は手番を消す（盤面は保持）
        g.setTurn(null);
        gameMapper.update(g.getGameId(), g.getBoardState(), null, g.getStatus());
      } catch (Exception ex) {
        logger.warn("Failed to update game status for forfeit {}: {}", gameId, ex.getMessage());
      }

      // 更新 players_status 並刪除 matching_queue
      try {
        if (g.getPlayerBlack() != null && !g.getPlayerBlack().isEmpty()) {
          matchingQueueMapper.deleteByUserAndGame(g.getPlayerBlack(), "gomoku");
          matchingQueueMapper.updatePlayerStatus(g.getPlayerBlack(), "lobby", null);
        }
        if (g.getPlayerWhite() != null && !g.getPlayerWhite().isEmpty()) {
          matchingQueueMapper.deleteByUserAndGame(g.getPlayerWhite(), "gomoku");
          matchingQueueMapper.updatePlayerStatus(g.getPlayerWhite(), "lobby", null);
        }
      } catch (Exception ex) {
        logger.warn("Failed to cleanup player status after forfeit: {}", ex.getMessage());
      }

      Map<String, Object> res = new HashMap<>();
      res.put("winner", winner);
      res.put("loser", loser);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in forfeitGame for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

}
