package team1.saikyoapps.model;

public class MatchHistory {
  long id;
  String gameName;
  String player1;
  String player2;
  String winner;
  String startedAt;
  String endedAt;
  String extra;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getGameName() {
    return gameName;
  }

  public void setGameName(String gameName) {
    this.gameName = gameName;
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

  public String getWinner() {
    return winner;
  }

  public void setWinner(String winner) {
    this.winner = winner;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(String endedAt) {
    this.endedAt = endedAt;
  }

  public String getExtra() {
    return extra;
  }

  public void setExtra(String extra) {
    this.extra = extra;
  }
}
