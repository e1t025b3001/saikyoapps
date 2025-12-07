package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team1.saikyoapps.model.GomokuGame;
import team1.saikyoapps.model.GomokuGameMapper;
import team1.saikyoapps.model.GomokuMoveMapper;
import team1.saikyoapps.model.MatchHistoryMapper;
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

  // 取得遊戲狀態
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId) {
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
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in getGame for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

  // 落子 API
  @PostMapping("/{gameId}/move")
  public ResponseEntity<Map<String, Object>> postMove(@PathVariable String gameId,
      @RequestBody Map<String, Object> body, @RequestParam(required = false) String user) {
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
      String playerName = (user != null) ? user : "unknown";
      String color = "black"; // fallback
      if (playerName.equals(g.getPlayerWhite()))
        color = "white";
      else if (playerName.equals(g.getPlayerBlack()))
        color = "black";

      int colorVal = "black".equals(color) ? 1 : 2;

      // 簡易ルール：もし現在のturnが未設定(null/空)であれば、最初に動いたプレイヤーの色を現在の手番として受け入れる
      if (g.getTurn() == null || g.getTurn().isEmpty()) {
        // プレイヤーがゲームの参加者であることを許容する（unknownは許容）
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
        g.setBoardState(newBoardJson);
        g.setTurn(nextTurn);
        if (win) {
          g.setStatus("finished");
          // 寫入 match_history
          historyMapper.insert("gomoku", g.getPlayerBlack(), g.getPlayerWhite(), color, null,
              new Timestamp(System.currentTimeMillis()), null);
        }
        gameMapper.update(g.getGameId(), newBoardJson, g.getTurn(), g.getStatus());
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

}
