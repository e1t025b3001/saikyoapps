package team1.saikyoapps.darour.controller;

import java.util.ArrayList;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import team1.saikyoapps.darour.model.Card;
import team1.saikyoapps.darour.model.DarourGame;
import team1.saikyoapps.darour.model.DarourGameMapper;
import team1.saikyoapps.darour.model.Hand;
import team1.saikyoapps.darour.model.Rank;
import team1.saikyoapps.darour.model.Suit;

@Controller
public class DarourController {
  @Autowired
  DarourGameMapper darourGame;

  @GetMapping("/darour")
  public String darour(Model model, Authentication authentication) {
    model.addAttribute("username", authentication.getName());

    DarourGame game = darourGame.selectDarourGameByPlayer(authentication.getName()).getFirst();

    model.addAttribute("player1", game.getPlayer1());
    model.addAttribute("player2", game.getPlayer2());
    model.addAttribute("player3", game.getPlayer3());

    // ランダムなカードを用意する
    Random random = new Random();
    ArrayList<Card> cards = new ArrayList<>();
    while (cards.size() < Hand.HAND_SIZE) {
      Suit suit = Suit.values()[random.nextInt(Suit.values().length)];
      Rank rank = Rank.values()[random.nextInt(Rank.values().length)];
      Card card = Card.of(suit, rank);

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
