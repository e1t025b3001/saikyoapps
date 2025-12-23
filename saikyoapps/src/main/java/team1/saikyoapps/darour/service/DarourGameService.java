package team1.saikyoapps.darour.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import team1.saikyoapps.darour.model.Hand;

@Service
public class DarourGameService {
  final Integer MAX_PLAYERS = 3;

  ArrayList<String> players = new ArrayList<>(3);

  ArrayList<Hand> hands = new ArrayList<>(3);
  Integer currentPlayerIndex = 0;

  public DarourGameService(ArrayList<String> players) {
    this.players = players;

    // クラブの3を持っているプレイヤーから開始
    for (int i = 0; i < players.size(); i++) {
      if (hands.get(i).hasClubThree()) {
        currentPlayerIndex = i;
        break;
      }
    }
  }
}
