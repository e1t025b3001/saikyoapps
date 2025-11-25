package team1.saikyoapps.service;

import java.util.List;

public class GameSession {
  private String id;
  private String game;
  private List<String> players;

  public GameSession(String id, String game, List<String> players) {
    this.id = id;
    this.game = game;
    this.players = players;
  }

  public String getId() {
    return id;
  }

  public String getGame() {
    return game;
  }

  public List<String> getPlayers() {
    return players;
  }
}
