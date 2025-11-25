package team1.saikyoapps.model;

public class MatchingQueue {
  int id;
  String userName;
  String requestedGame; // "marubatsu" | "darour" | ...
  String status; // WAITING / MATCHED

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getRequestedGame() {
    return requestedGame;
  }

  public void setRequestedGame(String requestedGame) {
    this.requestedGame = requestedGame;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
