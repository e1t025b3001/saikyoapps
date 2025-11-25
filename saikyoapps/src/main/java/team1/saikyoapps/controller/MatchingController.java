package team1.saikyoapps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import team1.saikyoapps.model.MatchingQueueMapper;

@Controller
public class MatchingController {
  @Autowired
  MatchingQueueMapper matchingQueueMapper;

  @GetMapping("/matching")
  public String matching(@RequestParam(name = "game", required = false) String game, Model model,
      Authentication authentication) {
    if (authentication != null) {
      model.addAttribute("username", authentication.getName());
    } else {
      model.addAttribute("username", "guest");
    }

    // 本来はindex.htmlでマッチング開始ボタン押下時に待ち行列に参加するが、デバッグ用にここで処理する
    if (authentication != null && game != null) {
      matchingQueueMapper.insert(authentication.getName(), game, "WAITING");
    }

    // ダミーとして、marubatsuで待機している人数を表示する
    int waitingCount = matchingQueueMapper.countWaitingPlayersByGame(game);
    model.addAttribute("waitingCount", waitingCount);

    model.addAttribute("game", game != null ? game : "未選択");
    return "matching";
  }
}
