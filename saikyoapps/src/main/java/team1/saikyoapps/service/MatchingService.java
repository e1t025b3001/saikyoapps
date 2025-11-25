package team1.saikyoapps.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class MatchingService {

  // ゲームごとの待機リスト
  private final Map<String, List<String>> waiting = new ConcurrentHashMap<>();
  // セッションID -> GameSession
  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  // 2人ゲームと3人ゲームの定義
  private final List<String> twoPlayerGames = List.of("tictactoe", "othello", "gomoku");
  private final List<String> threePlayerGames = List.of("concentration", "uno", "daifugo");

  public synchronized String joinQueue(String game, String username) {
    if (game == null || username == null) {
      return null;
    }
    // 既にいずれかのセッションに入っている場合はそのセッションIDを返す
    for (Map.Entry<String, GameSession> e : sessions.entrySet()) {
      if (e.getValue().getPlayers().contains(username)) {
        return e.getKey();
      }
    }

    List<String> q = waiting.computeIfAbsent(game, k -> Collections.synchronizedList(new ArrayList<>()));
    if (q.contains(username)) {
      return null; // 既に待機中
    }
    q.add(username);

    int required = twoPlayerGames.contains(game) ? 2 : (threePlayerGames.contains(game) ? 3 : 2);
    if (q.size() >= required) {
      // マッチング成立
      List<String> players = new ArrayList<>();
      for (int i = 0; i < required; i++) {
        players.add(q.remove(0));
      }
      String sessionId = UUID.randomUUID().toString();
      GameSession gs = new GameSession(sessionId, game, players);
      sessions.put(sessionId, gs);
      return sessionId;
    }
    return null;
  }

  public String checkSessionForUser(String username) {
    for (Map.Entry<String, GameSession> e : sessions.entrySet()) {
      if (e.getValue().getPlayers().contains(username)) {
        return e.getKey();
      }
    }
    return null;
  }

  public GameSession getSession(String sessionId) {
    return sessions.get(sessionId);
  }

}
