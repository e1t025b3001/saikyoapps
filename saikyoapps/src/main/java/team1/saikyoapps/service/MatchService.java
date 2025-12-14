package team1.saikyoapps.service;

import org.springframework.stereotype.Service;
import team1.saikyoapps.model.Match;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchService {

  // インメモリマッチストア
  private Map<String, Match> matches = new ConcurrentHashMap<>();

  public MatchService() {
  }

  // マッチ作成
  public String createMatch(String game, String player1, String player2) {
    String id = UUID.randomUUID().toString();
    Match m = new Match(id, game, player1, player2);
    matches.put(id, m);
    return id;
  }

  public Match getMatch(String matchId) {
    return matches.get(matchId);
  }

  public void saveMatch(Match m) {
    if (m != null && m.getMatchId() != null)
      matches.put(m.getMatchId(), m);
  }

  public Match removeMatch(String matchId) {
    return matches.remove(matchId);
  }

  // 指定ユーザが参加している指定ゲームのマッチIDを返す（なければ null）
  public String findMatchIdByPlayer(String player, String game) {
    for (Map.Entry<String, Match> e : matches.entrySet()) {
      Match m = e.getValue();
      if (m == null)
        continue;
      if (!m.getGame().equals(game))
        continue;
      if (player.equals(m.getPlayer1()) || player.equals(m.getPlayer2())) {
        return e.getKey();
      }
    }
    return null;
  }

  // 盤面に手を打つ。pos: 0..8
  // player: username
  // 戻り値: 更新された Match オブジェクト
  public synchronized Match applyMove(Match match, String player, int pos) throws IllegalArgumentException {
    if (match == null)
      throw new IllegalArgumentException("match is null");
    if (pos < 0 || pos > 8)
      throw new IllegalArgumentException("pos out of range");
    if (!"playing".equals(match.getStatus()))
      throw new IllegalArgumentException("match not playing");
    if (!player.equals(match.getTurn()))
      throw new IllegalArgumentException("not your turn");
    if (match.getBoard()[pos] != 0)
      throw new IllegalArgumentException("cell occupied");

    int mark = player.equals(match.getPlayer1()) ? 1 : 2;
    int[] b = match.getBoard();
    b[pos] = mark;
    match.setBoard(b);

    int winner = checkWinner(b);
    if (winner == 1) {
      match.setWinner(match.getPlayer1());
      match.setStatus("finished");
    } else if (winner == 2) {
      match.setWinner(match.getPlayer2());
      match.setStatus("finished");
    } else if (winner == 3) {
      match.setWinner(null);
      match.setStatus("finished");
    } else {
      // ターン交代
      String next = player.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
      match.setTurn(next);
    }

    // 保存
    saveMatch(match);
    return match;
  }

  // 勝敗判定: 0=続行,1=player1勝ち,2=player2勝ち,3=引き分け
  private int checkWinner(int[] b) {
    int[][] lines = new int[][] {
        { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 },
        { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 },
        { 0, 4, 8 }, { 2, 4, 6 }
    };
    for (int[] l : lines) {
      int a = b[l[0]];
      if (a != 0 && a == b[l[1]] && a == b[l[2]]) {
        return a;
      }
    }
    boolean full = true;
    for (int v : b)
      if (v == 0) {
        full = false;
        break;
      }
    if (full)
      return 3;
    return 0;
  }
}
