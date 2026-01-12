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
import team1.saikyoapps.model.MarubatsuMove;
import team1.saikyoapps.model.MatchHistoryMapper;
import team1.saikyoapps.model.MatchingQueueMapper;
import team1.saikyoapps.service.MarubatsuService;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

  // 暫存剛結束的對局結果，避免 race condition
  private final Map<String, Map<String, Object>> finishedWinners = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  // 観戦一覧（対局中のみ）
  @GetMapping("/spectate/list")
  public ResponseEntity<List<Map<String, Object>>> listSpectateGames(Authentication authentication) {
    try {
      if (authentication == null) {
        return ResponseEntity.status(401).build();
      }
      List<MarubatsuGame> games = gameMapper.findPlayingGames();
      List<Map<String, Object>> res = new ArrayList<>();
      for (MarubatsuGame g : games) {
        res.add(Map.of(
            "gameId", g.getGameId(),
            "playerX", g.getPlayerX(),
            "playerO", g.getPlayerO()));
      }
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in listSpectateGames", ex);
      return ResponseEntity.status(500).build();
    }
  }

  // ゲーム状態取得（クライアントから poll される）
  @GetMapping("/{gameId}")
  public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId, Authentication authentication) {
    try {
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null) {
        // 若遊戲已被刪除，但暫存有結束資訊，回傳 finished 資訊給前端
        if (finishedWinners.containsKey(gameId)) {
          Map<String, Object> info = finishedWinners.get(gameId);
          Map<String, Object> res = new HashMap<>();
          res.put("board", info.getOrDefault("board", null));
          res.put("turn", null);
          res.put("finished", true);
          res.put("winner", info.get("winner"));
          res.put("loser", info.get("loser"));
          if (info.containsKey("winningLine"))
            res.put("winningLine", info.get("winningLine"));
          // 観戦表示用に player を null にし mode を spectator に設定
          res.put("playerX", null);
          res.put("playerO", null);
          res.put("mode", "spectator");
          return ResponseEntity.ok(res);
        }
        return ResponseEntity.notFound().build();
      }
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
      boolean finished = "finished".equals(g.getStatus());
      res.put("finished", finished);
      if (finished && finishedWinners.containsKey(gameId)) {
        Map<String, Object> info = finishedWinners.get(gameId);
        res.put("winner", info.get("winner"));
        res.put("loser", info.get("loser"));
        if (info.containsKey("winningLine"))
          res.put("winningLine", info.get("winningLine"));
      }

      // モード判定（player / spectator）と mySymbol を返す
      String mode = "spectator";
      if (authentication != null) {
        String user = authentication.getName();
        if (user.equals(g.getPlayerX())) {
          res.put("mySymbol", "X");
          mode = "player";
        } else if (user.equals(g.getPlayerO())) {
          res.put("mySymbol", "O");
          mode = "player";
        }
      }

      res.put("playerX", g.getPlayerX());
      res.put("playerO", g.getPlayerO());
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
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null)
        return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      int pos = ((Number) body.getOrDefault("pos", -1)).intValue();
      if (pos < 0 || pos >= 9)
        return ResponseEntity.badRequest().body(Map.of("error", "invalid pos"));

      // 先不直接依賴 boardState，使用 moves 重建盤面以配合三子規則
      int[] board = new int[9];
      try {
        // 如果資料庫已有 moves，從 moves 建立盤面
        List<MarubatsuMove> existingMoves = moveMapper.findByGameId(gameId);
        if (existingMoves != null) {
          for (MarubatsuMove m : existingMoves) {
            int idx = m.getY() * 3 + m.getX();
            if (m.getPlayer() != null && m.getPlayer().equals(g.getPlayerX())) {
              board[idx] = 1;
            } else if (m.getPlayer() != null && m.getPlayer().equals(g.getPlayerO())) {
              board[idx] = 2;
            }
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to build board from moves for {}: {}", gameId, e.getMessage());
      }

      String playerName = (authentication != null) ? authentication.getName() : "unknown";
      String symbol = "X";
      if (playerName.equals(g.getPlayerO()))
        symbol = "O";
      else if (playerName.equals(g.getPlayerX()))
        symbol = "X";

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
      if (board[pos] != 0)
        return ResponseEntity.status(409).body(Map.of("error", "cell occupied"));

      // 三子規則：若該玩家已有 3 顆，刪除最古一顆
      try {
        int playerCount = moveMapper.countByGameIdAndPlayer(gameId, playerName);
        if (playerCount >= 3) {
          MarubatsuMove earliest = moveMapper.findEarliestByGameIdAndPlayer(gameId, playerName);
          if (earliest != null) {
            moveMapper.deleteById(earliest.getId());
            // 從 board 中移除該位置，保持一致
            int eidx = earliest.getY() * 3 + earliest.getX();
            if (eidx >= 0 && eidx < 9)
              board[eidx] = 0;
          }
        }
      } catch (Exception ex) {
        logger.warn("Failed to enforce 3-piece rule for {}: {}", gameId, ex.getMessage());
      }

      // 插入新手（在刪除最古後）
      int moveNo = moveMapper.countByGameId(gameId) + 1;
      moveMapper.insert(gameId, playerName, pos % 3, pos / 3, moveNo);

      // 重新從 moves 讀取並建立盤面，確保 server 為真實單一來源
      try {
        int[] newBoard = new int[9];
        List<MarubatsuMove> newMoves = moveMapper.findByGameId(gameId);
        if (newMoves != null) {
          for (MarubatsuMove m : newMoves) {
            int idx = m.getY() * 3 + m.getX();
            if (m.getPlayer() != null && m.getPlayer().equals(g.getPlayerX())) {
              newBoard[idx] = 1;
            } else if (m.getPlayer() != null && m.getPlayer().equals(g.getPlayerO())) {
              newBoard[idx] = 2;
            }
          }
        }
        board = newBoard;
      } catch (Exception e) {
        logger.warn("Failed to rebuild board after insert for {}: {}", gameId, e.getMessage());
      }

      // 勝敗判定は service を利用
      boolean win = marubatsuService.checkWin(board, val);
      boolean draw = !win && marubatsuService.isFull(board);
      List<int[]> winningLine = null;

      try {
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        String newBoardJson = om.writeValueAsString(board);
        String nextTurn = "X".equals(g.getTurn()) ? "O" : "X";
        if (!win && !draw) {
          g.setBoardState(newBoardJson);
          g.setTurn(nextTurn);
          try {
            gameMapper.update(g.getGameId(), newBoardJson, g.getTurn(), g.getStatus());
          } catch (Exception ex) {
            logger.warn("Failed to update game {} after move: {}", gameId, ex.getMessage());
          }
        } else {
          // 勝利または引き分けの終了処理
          g.setStatus("finished");
          try {
            String winnerSymbol = win ? ("X".equals(g.getTurn()) ? "O" : "X") : null;
            String winnerName = null;
            if (win) {
              if ("X".equals(winnerSymbol))
                winnerName = g.getPlayerX();
              else
                winnerName = g.getPlayerO();
            }
            historyMapper.insert("marubatsu", g.getPlayerX(), g.getPlayerO(), winnerName, null,
                new Timestamp(System.currentTimeMillis()), win ? null : null);
          } catch (Exception ex) {
            logger.warn("Failed to insert match_history for win {}: {}", gameId, ex.getMessage());
          }
          try {
            moveMapper.deleteByGameId(gameId);
          } catch (Exception ex) {
            logger.warn("Failed to delete moves for game {}: {}", gameId, ex.getMessage());
          }

          // 產生 winningLine（若有）
          if (win) {
            int target = val;
            int[][] lines = { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 },
                { 2, 4, 6 } };
            for (int[] ln : lines) {
              if (board[ln[0]] == target && board[ln[1]] == target && board[ln[2]] == target) {
                winningLine = new ArrayList<>();
                for (int idx : ln) {
                  int rpos = idx / 3;
                  int cpos = idx % 3;
                  winningLine.add(new int[] { cpos, rpos });
                }
                break;
              }
            }
          }

          // 更新 marubatsu_game 為 finished，保留最後盤面以供前端顯示
          try {
            g.setBoardState(newBoardJson);
            g.setTurn(null);
            g.setStatus("finished");
            gameMapper.update(g.getGameId(), newBoardJson, null, g.getStatus());
          } catch (Exception ex) {
            logger.warn("Failed to update marubatsu_game {} to finished: {}", gameId, ex.getMessage());
          }

          // 更新 players_status 回 lobby
          try {
            if (g.getPlayerX() != null && !g.getPlayerX().isEmpty()) {
              matchingQueueMapper.deleteByUserAndGame(g.getPlayerX(), "marubatsu");
              matchingQueueMapper.updatePlayerStatus(g.getPlayerX(), "lobby", null);
            }
            if (g.getPlayerO() != null && !g.getPlayerO().isEmpty()) {
              matchingQueueMapper.deleteByUserAndGame(g.getPlayerO(), "marubatsu");
              matchingQueueMapper.updatePlayerStatus(g.getPlayerO(), "lobby", null);
            }
          } catch (Exception ex) {
            logger.warn("Failed to cleanup player status after game end: {}", ex.getMessage());
          }

          // 暫存結果並安排延遲刪除（5s）
          Map<String, Object> info = new HashMap<>();
          String winnerSym = win ? ("X".equals(symbol) ? "X" : "O") : null;
          String winnerName = null;
          String loserName = null;
          if (win) {
            if (val == 1)
              winnerSym = "X";
            else
              winnerSym = "O";
            winnerName = ("X".equals(winnerSym)) ? g.getPlayerX() : g.getPlayerO();
            loserName = ("X".equals(winnerSym)) ? g.getPlayerO() : g.getPlayerX();
          }
          info.put("winner", winnerName == null ? "" : winnerName);
          info.put("loser", loserName == null ? "" : loserName);
          info.put("winningLine", winningLine);
          info.put("board", board);
          finishedWinners.put(gameId, info);
          scheduler.schedule(() -> {
            try {
              moveMapper.deleteByGameId(gameId);
            } catch (Exception e) {
              logger.warn("Scheduled move delete failed for {}: {}", gameId, e.getMessage());
            }
            try {
              gameMapper.deleteByGameId(gameId);
            } catch (Exception e) {
              logger.warn("Scheduled game delete failed for {}: {}", gameId, e.getMessage());
            }
            finishedWinners.remove(gameId);
          }, 5, TimeUnit.SECONDS);

          Map<String, Object> resWin = new HashMap<>();
          resWin.put("board", board);
          resWin.put("turn", null);
          resWin.put("finished", true);
          resWin.put("winner", win ? winnerSym : null);
          resWin.put("loser", win ? loserName : null);
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
      res.put("finished", win || draw);
      if (win)
        res.put("winner", symbol);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in postMove for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

  @PostMapping("/{gameId}/forfeit")
  public ResponseEntity<Map<String, Object>> forfeitGame(@PathVariable String gameId, Authentication authentication) {
    try {
      MarubatsuGame g = gameMapper.findByGameId(gameId);
      if (g == null)
        return ResponseEntity.badRequest().body(Map.of("error", "game not found"));

      String loser = (authentication != null) ? authentication.getName() : "unknown";
      String winner = null;
      if (g.getPlayerX() != null && !g.getPlayerX().isEmpty() && !g.getPlayerX().equals(loser))
        winner = g.getPlayerX();
      if (winner == null && g.getPlayerO() != null && !g.getPlayerO().isEmpty() && !g.getPlayerO().equals(loser))
        winner = g.getPlayerO();

      try { // forfeit logic
        historyMapper.insert("marubatsu", g.getPlayerX(), g.getPlayerO(), winner, null,
            new Timestamp(System.currentTimeMillis()), "forfeit");
      } catch (Exception ex) {
        logger.warn("Failed to insert match_history for forfeit {}: {}", ex.getMessage(), ex);
      }
      try {
        moveMapper.deleteByGameId(gameId);
      } catch (Exception ex) {
        logger.warn("Failed to delete moves for forfeit {}: {}", gameId, ex.getMessage());
      }

      try {
        g.setStatus("finished");
        g.setTurn(null);
        gameMapper.update(g.getGameId(), g.getBoardState(), null, g.getStatus());
      } catch (Exception ex) {
        logger.warn("Failed to update game status for forfeit {}: {}", gameId, ex.getMessage());
      }

      try {
        if (g.getPlayerX() != null && !g.getPlayerX().isEmpty()) {
          matchingQueueMapper.deleteByUserAndGame(g.getPlayerX(), "marubatsu");
          matchingQueueMapper.updatePlayerStatus(g.getPlayerX(), "lobby", null);
        }
        if (g.getPlayerO() != null && !g.getPlayerO().isEmpty()) {
          matchingQueueMapper.deleteByUserAndGame(g.getPlayerO(), "marubatsu");
          matchingQueueMapper.updatePlayerStatus(g.getPlayerO(), "lobby", null);
        }
      } catch (Exception ex) {
        logger.warn("Failed to cleanup player status after forfeit: {}", ex.getMessage());
      }

      Map<String, Object> info = new HashMap<>();
      info.put("winner", winner == null ? "" : winner);
      info.put("loser", loser == null ? "" : loser);
      info.put("winningLine", null);
      // 嘗試回傳 board
      try {
        if (g.getBoardState() != null && !g.getBoardState().isEmpty()) {
          com.fasterxml.jackson.databind.ObjectMapper om2 = new com.fasterxml.jackson.databind.ObjectMapper();
          int[] boardRes = om2.readValue(g.getBoardState(), int[].class);
          info.put("board", boardRes);
        } else {
          info.put("board", new int[9]);
        }
      } catch (Exception e) {
        info.put("board", new int[9]);
      }
      finishedWinners.put(gameId, info);
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

      Map<String, Object> res = new HashMap<>();
      res.put("board", info.get("board"));
      res.put("finished", true);
      res.put("winner", winner);
      res.put("loser", loser);
      res.put("winningLine", null);
      return ResponseEntity.ok(res);
    } catch (Exception ex) {
      logger.error("Error in forfeitGame for {}", gameId, ex);
      return ResponseEntity.status(500).body(Map.of("error", "server error"));
    }
  }

}
