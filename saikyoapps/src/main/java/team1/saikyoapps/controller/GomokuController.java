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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

  // 用於暫存剛結束對局的勝方資訊，供前端輪詢時取得 winner
  // 存放結束資訊：{ gameId -> {"winner": <user>, "loser": <user>, "winningLine":
  // <List<int[]>> } }
  private final Map<String, Map<String, Object>> finishedWinners = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  // 取得遊戲狀態
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId, Authentication authentication) {
    try {
      GomokuGame g = gameMapper.findByGameId(gameId);
      if (g == null) {
        // 若找不到遊戲，但剛結束的 winners 暫存裡有資料，也回傳 finished 資訊，避免前端卡住
        if (finishedWinners.containsKey(gameId)) {
          @SuppressWarnings("unchecked")
          Map<String, Object> info = (Map<String, Object>) finishedWinners.get(gameId);
          Map<String, Object> res = new HashMap<>();
          res.put("board", null);
          res.put("turn", null);
          res.put("finished", true);
          res.put("winner", info.get("winner"));
          res.put("loser", info.get("loser"));
          if (info.containsKey("winningLine")) {
            res.put("winningLine", info.get("winningLine"));
          }
          return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
      }
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
      boolean finished = "finished".equals(g.getStatus());
      res.put("finished", finished);
      // 若遊戲結束且我們有暫存的 winner/loser，回傳
      if (finished && finishedWinners.containsKey(gameId)) {
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) finishedWinners.get(gameId);
        res.put("winner", info.get("winner"));
        res.put("loser", info.get("loser"));
        if (info.containsKey("winningLine")) {
          res.put("winningLine", info.get("winningLine"));
        }
      }
      // 若有登入使用者，回傳 myColor
      if (authentication != null) {
        String user = authentication.getName();
        if (user.equals(g.getPlayerBlack()))
          res.put("myColor", "black");
        else if (user.equals(g.getPlayerWhite()))
          res.put("myColor", "white");
      }
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
      String color = "black"; // fallback
      if (playerName.equals(g.getPlayerWhite()))
        color = "white";
      else if (playerName.equals(g.getPlayerBlack()))
        color = "black";

      int colorVal = "black".equals(color) ? 1 : 2;

      // 簡易ルール：如果目前回合未設定，接受第一個動作玩家的顏色
      if (g.getTurn() == null || g.getTurn().isEmpty()) {
        if ("unknown".equals(playerName) || playerName.equals(g.getPlayerBlack())
            || playerName.equals(g.getPlayerWhite())) {
          g.setTurn(color);
        } else {
          return ResponseEntity.status(403).body(Map.of("error", "player not in game"));
        }
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
      List<int[]> winningLine = null;
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
          // 計算連線的起訖座標並產生座標列表
          int sx = x;
          int sy = y;
          while (true) {
            int nx = sx - d[0];
            int ny = sy - d[1];
            if (nx < 0 || ny < 0 || nx >= 15 || ny >= 15)
              break;
            if (board[ny][nx] == colorVal) {
              sx = nx;
              sy = ny;
            } else
              break;
          }
          int ex = x;
          int ey = y;
          while (true) {
            int nx = ex + d[0];
            int ny = ey + d[1];
            if (nx < 0 || ny < 0 || nx >= 15 || ny >= 15)
              break;
            if (board[ny][nx] == colorVal) {
              ex = nx;
              ey = ny;
            } else
              break;
          }
          winningLine = new ArrayList<>();
          int steps = Math.max(Math.abs(ex - sx), Math.abs(ey - sy));
          int dx = (steps == 0) ? 0 : (ex - sx) / steps;
          int dy = (steps == 0) ? 0 : (ey - sy) / steps;
          for (int i = 0; i <= steps; i++) {
            int cx = sx + i * dx;
            int cy = sy + i * dy;
            winningLine.add(new int[] { cx, cy });
          }
          break outer;
        }
      }

      // 更新 game board / turn / status
      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        String newBoardJson = om.writeValueAsString(board);
        String nextTurn = "black".equals(g.getTurn()) ? "white" : "black";
        // 非勝利時是通常どおり保存
        if (!win) {
          g.setBoardState(newBoardJson);
          g.setTurn(nextTurn);
          try {
            gameMapper.update(g.getGameId(), newBoardJson, g.getTurn(), g.getStatus());
          } catch (Exception ex) {
            logger.warn("Failed to update game {} after move: {}", gameId, ex.getMessage());
          }
        } else {
          // 勝利時：記錄比賽歷史、刪除 moves、標記 game 為 finished，並暫存 winner 供前端輪詢
          g.setStatus("finished");
          String winnerName = null;
          if ("black".equals(color))
            winnerName = g.getPlayerBlack();
          else
            winnerName = g.getPlayerWhite();
          String loserName = (winnerName != null && winnerName.equals(g.getPlayerBlack())) ? g.getPlayerWhite()
              : g.getPlayerBlack();

          try {
            historyMapper.insert("gomoku", g.getPlayerBlack(), g.getPlayerWhite(), winnerName, null,
                new Timestamp(System.currentTimeMillis()), null);
          } catch (Exception ex) {
            logger.warn("Failed to insert match_history for win {}: {}", gameId, ex.getMessage());
          }
          try {
            // 立即刪除 moves 確保即時表只包含正在對戰的資料
            moveMapper.deleteByGameId(gameId);
          } catch (Exception ex) {
            logger.warn("Failed to delete moves for game {}: {}", gameId, ex.getMessage());
          }

          // 更新 gomoku_game 為 finished (保留最後一手於 boardState)，以便前端 polling 能讀到 finished=true
          // 與最終盤面
          try {
            // 保留 newBoardJson 為最終盤面，清空回合資訊(turn)
            g.setBoardState(newBoardJson);
            g.setTurn(null);
            g.setStatus("finished");
            gameMapper.update(g.getGameId(), newBoardJson, null, g.getStatus());
          } catch (Exception ex) {
            logger.warn("Failed to update gomoku_game {} to finished: {}", gameId, ex.getMessage());
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

          // 暫存 winner/loser 資訊，並安排在 60 秒後清理暫存與刪除遊戲（給前端時間同步導頁）
          Map<String, Object> infoMap = new HashMap<>();
          infoMap.put("winner", winnerName == null ? "" : winnerName);
          infoMap.put("loser", loserName == null ? "" : loserName);
          // 記錄勝方顏色（black/white），供前端顯示誰贏誰輸
          infoMap.put("winnerColor", color);
          infoMap.put("winningLine", winningLine);
          finishedWinners.put(gameId, infoMap);
          scheduler.schedule(() -> {
            try {
              // 確保 moves 已被刪除（防止殘留）13
              moveMapper.deleteByGameId(gameId);
            } catch (Exception e) {
              logger.warn("Scheduled move delete failed for {}: {}", gameId, e.getMessage());
            }
            try {
              gameMapper.deleteByGameId(gameId);
            } catch (Exception e) {
              logger.warn("Scheduled game delete failed for {}: {}", gameId, e.getMessage());
            }
            // 清除暫存 winner/loser
            finishedWinners.remove(gameId);
          }, 5, TimeUnit.SECONDS);

          // 回傳勝利結果（包含最終盤面，讓 client 可以繪出最後一顆棋子）
          Map<String, Object> resWin = new HashMap<>();
          resWin.put("board", board);
          resWin.put("turn", null);
          resWin.put("finished", true);
          resWin.put("winner", winnerName);
          resWin.put("winnerColor", color);
          resWin.put("loser", loserName);
          resWin.put("winningLine", winningLine);
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
      if (win) {
        // 在此分支通常不會走到（勝利已在上方處理），但若走到則還是回傳勝方顏色與名稱
        String wcol = "black".equals(color) ? "black" : "white";
        res.put("winner", g.getPlayerBlack());
        res.put("winnerColor", wcol);
      }
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

      // 刪除 moves（立即）
      try {
        moveMapper.deleteByGameId(gameId);
      } catch (Exception ex) {
        logger.warn("Failed to delete moves for forfeit {}: {}", gameId, ex.getMessage());
      }

      // 更新 game 狀態為 finished（保留紀錄以供前端顯示），並暫存 winner
      try {
        // 保留當前盤面（若有），清空回合並標記 finished，讓雙方可從 GET /gomoku/{gameId} 讀到 finished 與 winner
        g.setStatus("finished");
        // 不覆寫 boardState 為 null，保留現有盤面以便雙方能看到結束時的盤面
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

      // 暫存 winner/loser 並安排在 60 秒後清除暫存與刪除 gomoku_game，不立即刪除 gomoku_game
      Map<String, Object> fi = new HashMap<>();
      fi.put("winner", winner == null ? "" : winner);
      fi.put("loser", loser == null ? "" : loser);
      // 決定 winnerColor
      String winnerColor = null;
      if (winner != null) {
        if (winner.equals(g.getPlayerBlack()))
          winnerColor = "black";
        else if (winner.equals(g.getPlayerWhite()))
          winnerColor = "white";
      }
      fi.put("winnerColor", winnerColor);
      fi.put("winningLine", null);
      finishedWinners.put(gameId, fi);
      scheduler.schedule(() -> {
        try {
          moveMapper.deleteByGameId(gameId);
        } catch (Exception e) {
          logger.warn("Scheduled move delete failed for forfeit {}: {}", gameId, e.getMessage());
        }
        try {
          gameMapper.deleteByGameId(gameId);
        } catch (Exception e) {
          logger.warn("Scheduled game delete failed for forfeit {}: {}", gameId, e.getMessage());
        }
        finishedWinners.remove(gameId);
      }, 5, TimeUnit.SECONDS);

      // 回傳完整資訊給投降者，前端可即時顯示結果及盤面
      Map<String, Object> res = new HashMap<>();
      // 嘗試解析並回傳現有盤面
      if (g.getBoardState() != null && !g.getBoardState().isEmpty()) {
        try {
          com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
          int[][] board = om.readValue(g.getBoardState(), int[][].class);
          res.put("board", board);
        } catch (Exception e) {
          res.put("board", null);
        }
      } else {
        res.put("board", null);
      }
      res.put("finished", true);
      res.put("winner", winner);
      res.put("winnerColor", winnerColor);
      res.put("loser", loser);
      res.put("winningLine", null);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in forfeitGame for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

}
