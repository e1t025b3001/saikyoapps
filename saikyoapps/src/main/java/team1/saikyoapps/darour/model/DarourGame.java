package team1.saikyoapps.darour.model;

/*
 * CREATE TABLE IF NOT EXISTS darour_game (
  id IDENTITY,
  player1 VARCHAR(255),
  player2 VARCHAR(255),
  player3 VARCHAR(255),
  game_state VARCHAR(32),
)
 */
public class DarourGame {
  String gameID;
  String player1;
  String player2;
  String player3;

  public String getGameID() {
    return gameID;
  }

  public void setGameID(String gameID) {
    this.gameID = gameID;
  }

  public String getPlayer1() {
    return player1;
  }

  public void setPlayer1(String player1) {
    this.player1 = player1;
  }

  public String getPlayer2() {
    return player2;
  }

  public void setPlayer2(String player2) {
    this.player2 = player2;
  }

  public String getPlayer3() {
    return player3;
  }

  public void setPlayer3(String player3) {
    this.player3 = player3;
  }
}
