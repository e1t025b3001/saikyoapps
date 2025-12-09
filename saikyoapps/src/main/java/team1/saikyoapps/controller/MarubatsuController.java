package team1.saikyoapps.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import team1.saikyoapps.model.MarubatsuGame;
import team1.saikyoapps.model.MarubatsuGameMapper;
import team1.saikyoapps.model.MarubatsuMoveMapper;
import team1.saikyoapps.model.MatchHistoryMapper;
import team1.saikyoapps.model.MatchingQueueMapper;
import team1.saikyoapps.service.MarubatsuService;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/marubatsu")
public class MarubatsuController {

  private static final Logger logger = LoggerFactory.getLogger(MarubatsuController.class);

  @Autowired
  MarubatsuGameMapper gameMapper;

  @Autowired
  MarubatsuMoveMapper moveMapper;

  @Autowired
  MatchHistoryMapper historyMapper;

  @Autowired
  MatchingQueueMapper matchingQueueMapper;

  @Autowired
  MarubatsuService marubatsuService;

  // ゲーム状態取得（クライアントから poll される）
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId, Authentication authentication) {
    try {
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null) return ResponseEntity.notFound().build();
      Map<String, Object> res = new HashMap<>();
      if (g.getBoardState() != null && !g.getBoardState().isEmpty()) {
        try {
          com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
          int[] board = om.readValue(g.getBoardState(), int[].class);
          res.put("board", board);
        } catch (Exception e) {
          logger.error("Failed to parse boardState for game {}", gameId, e);
          res.put("board", null);
        }
      } else {
        res.put("board", new int[9]);
      }
      res.put("turn", g.getTurn());
      res.put("finished", "finished".equals(g.getStatus()));
      if (authentication != null) {
        String user = authentication.getName();
        if (user.equals(g.getPlayerX())) res.put("mySymbol", "X");
        else if (user.equals(g.getPlayerO())) res.put("mySymbol", "O");
      }
      res.put("playerX", g.getPlayerX());
      res.put("playerO", g.getPlayerO());
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
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null) return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      int pos = ((Number) body.getOrDefault("pos", -1)).intValue();
      if (pos < 0 || pos >= 9) return ResponseEntity.badRequest().body(Map.of("error", "invalid pos"));

      int[] board;
      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        if (g.getBoardState() == null || g.getBoardState().isEmpty()) {
          board = new int[9];
        } else {
          board = om.readValue(g.getBoardState(), int[].class);
        }
      } catch (Exception e) {
        logger.error("Failed to parse board state for {}", gameId, e);
        board = new int[9];
      }

      String playerName = (authentication != null) ? authentication.getName() : "unknown";
      String symbol = "X";
      if (playerName.equals(g.getPlayerO())) symbol = "O";
      else if (playerName.equals(g.getPlayerX())) symbol = "X";

      // 初回回合設定
      if (g.getTurn() == null || g.getTurn().isEmpty()) {
        if ("unknown".equals(playerName) || playerName.equals(g.getPlayerX()) || playerName.equals(g.getPlayerO())) {
          g.setTurn(symbol);
        } else {
          return ResponseEntity.status(403).body(Map.of("error", "player not in game"));
        }
      }

      if (g.getTurn() == null || !g.getTurn().equals(symbol)) {
        return ResponseEntity.status(409).body(Map.of("error", "not your turn"));
      }

      int val = "X".equals(symbol) ? 1 : 2;
      if (board[pos] != 0) return ResponseEntity.status(409).body(Map.of("error", "cell occupied"));

      int moveNo = moveMapper.countByGameId(gameId) + 1;
      // x,y mapping for persistence
      int r = pos / 3;
      int c = pos % 3;
      moveMapper.insert(gameId, playerName, c, r, moveNo);
      board[pos] = val;

      // 勝敗判定は service を利用
      boolean win = marubatsuService.checkWin(board, val);
      boolean draw = !win && marubatsuService.isFull(board);

      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        String newBoardJson = om.writeValueAsString(board);
        String nextTurn = "X".equals(g.getTurn()) ? "O" : "X";
        if (!win && !draw) {
          g.setBoardState(newBoardJson);
          g.setTurn(nextTurn);
          try { gameMapper.update(g.getGameId(), newBoardJson, g.getTurn(), g.getStatus()); } catch (Exception ex) { logger.warn("Failed to update game {} after move: {}", gameId, ex.getMessage()); }
        } else {
          // 終了処理
          g.setStatus("finished");
          try { historyMapper.insert("marubatsu", g.getPlayerX(), g.getPlayerO(), win ? symbol : null, null,
              new Timestamp(System.currentTimeMillis()), win ? null : null); } catch (Exception ex) { logger.warn("Failed to insert match_history for win {}: {}", gameId, ex.getMessage()); }
          try { moveMapper.deleteByGameId(gameId); } catch (Exception ex) { logger.warn("Failed to delete moves for game {}: {}", gameId, ex.getMessage()); }
          try { gameMapper.deleteByGameId(gameId); } catch (Exception ex) {
            logger.warn("Failed to delete marubatsu_game {}: {}", gameId, ex.getMessage());
            try { g.setBoardState(null); g.setTurn(null); g.setStatus("finished"); gameMapper.update(g.getGameId(), null, null, g.getStatus()); } catch (Exception ex2) { logger.warn("Fallback update failed for game {}: {}", gameId, ex2.getMessage()); }
          }

          try {
            if (g.getPlayerX() != null && !g.getPlayerX().isEmpty()) { matchingQueueMapper.deleteByUserAndGame(g.getPlayerX(), "marubatsu"); matchingQueueMapper.updatePlayerStatus(g.getPlayerX(), "lobby", null); }
            if (g.getPlayerO() != null && !g.getPlayerO().isEmpty()) { matchingQueueMapper.deleteByUserAndGame(g.getPlayerO(), "marubatsu"); matchingQueueMapper.updatePlayerStatus(g.getPlayerO(), "lobby", null); }
          } catch (Exception ex) { logger.warn("Failed to cleanup player status after game end: {}", ex.getMessage()); }

          Map<String,Object> resWin = new HashMap<>(); resWin.put("board", null); resWin.put("turn", null); resWin.put("finished", true); resWin.put("winner", win ? symbol : null); return ResponseEntity.ok(resWin);
        }
      } catch (Exception e) {
        logger.error("Failed to update game {}", gameId, e);
        return ResponseEntity.status(500).body(Map.of("error", "server error"));
      }

      Map<String,Object> res = new HashMap<>(); res.put("board", board); res.put("turn", g.getTurn()); res.put("finished", win || draw); if (win) res.put("winner", symbol); return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in postMove for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

  @PostMapping("/{gameId}/forfeit")
  public ResponseEntity<Map<String,Object>> forfeitGame(@PathVariable String gameId, Authentication authentication) {
    try {
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null) return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      String loser = (authentication != null) ? authentication.getName() : "unknown";
      String winner = null;
      if (g.getPlayerX() != null && !g.getPlayerX().isEmpty() && !g.getPlayerX().equals(loser)) winner = g.getPlayerX();
      if (winner == null && g.getPlayerO() != null && !g.getPlayerO().isEmpty() && !g.getPlayerO().equals(loser)) winner = g.getPlayerO();

      try { historyMapper.insert("marubatsu", g.getPlayerX(), g.getPlayerO(), winner, null, new Timestamp(System.currentTimeMillis()), "forfeit"); } catch (Exception ex) { logger.warn("Failed to insert match_history for forfeit {}", ex.getMessage()); }
      try { moveMapper.deleteByGameId(gameId); } catch (Exception ex) { logger.warn("Failed to delete moves for forfeit {}: {}", gameId, ex.getMessage()); }

      try { g.setStatus("finished"); g.setBoardState(null); g.setTurn(null); gameMapper.update(g.getGameId(), null, null, g.getStatus()); } catch (Exception ex) { logger.warn("Failed to update game status for forfeit {}: {}", gameId, ex.getMessage()); }

      try { if (g.getPlayerX() != null && !g.getPlayerX().isEmpty()) { matchingQueueMapper.deleteByUserAndGame(g.getPlayerX(), "marubatsu"); matchingQueueMapper.updatePlayerStatus(g.getPlayerX(), "lobby", null); } if (g.getPlayerO() != null && !g.getPlayerO().isEmpty()) { matchingQueueMapper.deleteByUserAndGame(g.getPlayerO(), "marubatsu"); matchingQueueMapper.updatePlayerStatus(g.getPlayerO(), "lobby", null); } } catch (Exception ex) { logger.warn("Failed to cleanup player status after forfeit: {}", ex.getMessage()); }

      Map<String,Object> res = new HashMap<>(); res.put("winner", winner); res.put("loser", loser); return ResponseEntity.ok(res);
    } catch (Exception ex) { logger.error("Error in forfeitGame for {}", gameId, ex); return ResponseEntity.status(500).body(Map.of("error", "server error")); }
  }
}
