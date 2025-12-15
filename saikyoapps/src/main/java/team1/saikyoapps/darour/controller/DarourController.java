package team1.saikyoapps.darour.controller;

import java.util.ArrayList;
import java.util.Random;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import team1.saikyoapps.darour.model.Card;
import team1.saikyoapps.darour.model.Hand;
import team1.saikyoapps.darour.model.Rank;
import team1.saikyoapps.darour.model.Suit;

@Controller
public class DarourController {
  @GetMapping("/darour")
  public String darour(Model model, Authentication authentication) {
    model.addAttribute("username", authentication.getName());

    // 13枚のランダムなカードを用意する
    Random random = new Random();

    var cards = new ArrayList<Card>();
    while (cards.size() < Hand.HAND_SIZE) {
      var suit = Suit.values()[random.nextInt(Suit.values().length)];
      var rank = Rank.values()[random.nextInt(Rank.values().length)];
      var card = new Card(suit, rank);

      if (!cards.contains(card)) {
        cards.add(card);
      }
    }

    var hand = new Hand(cards);
    hand.sortHand();

    model.addAttribute("hand", hand);

    return "darour.html";
  }
}
