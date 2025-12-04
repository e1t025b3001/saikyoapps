package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import team1.saikyoapps.model.Match;
import team1.saikyoapps.service.MatchService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class MatchController {

  @Autowired
  MatchService matchService;

  // 簡易インメモリマッチストア（後でDBに移行可能）
  private static Map<String, Match> matches = new ConcurrentHashMap<>();

  // マッチの状態を返す（ポーリング用）
  @GetMapping("/match/state")
  @ResponseBody
  public Match getMatchState(@RequestParam("matchId") String matchId) {
    return matches.get(matchId);
  }

  // マッチ生成（マッチング完了時に呼ばれる想定）
  // 既存の MatchingController が match_success にリダイレクトするので、
  // そこから別途 matchId を取得できるようにするか、MatchingController 側でここを呼ぶようにする予定
  @PostMapping("/match/create")
  @ResponseBody
  public Map<String, String> createMatch(@RequestParam("game") String game, @RequestParam("player1") String player1,
      @RequestParam("player2") String player2) {
    String id = UUID.randomUUID().toString();
    Match m = new Match(id, game, player1, player2);
    matches.put(id, m);
    return Map.of("matchId", id);
  }

  // プレイヤーの手をサーバで受け取る
  @PostMapping("/match/move")
  @ResponseBody
  public Map<String, Object> doMove(@RequestBody Map<String, Object> body, Authentication authentication) {
    String matchId = (String) body.get("matchId");
    Integer pos = (Integer) body.get("pos");
    if (authentication == null) {
      return Map.of("error", "unauthenticated");
    }
    String user = authentication.getName();
    Match m = matches.get(matchId);
    if (m == null) return Map.of("error", "no such match");
    try {
      matchService.applyMove(m, user, pos);
      // 更新を保存
      matches.put(matchId, m);
      return Map.of("ok", true, "match", m);
    } catch (IllegalArgumentException e) {
      return Map.of("error", e.getMessage());
    }
  }

  // マッチ終了 API（投了など）
  @PostMapping("/match/end")
  @ResponseBody
  public Map<String, Object> endMatch(@RequestBody Map<String, Object> body, Authentication authentication) {
    String matchId = (String) body.get("matchId");
    Match m = matches.remove(matchId);
    if (m == null) return Map.of("error", "no such match");
    return Map.of("ok", true, "matchId", matchId);
  }
}
