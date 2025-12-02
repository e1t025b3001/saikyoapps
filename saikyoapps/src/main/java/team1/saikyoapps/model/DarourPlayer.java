package team1.saikyoapps.model;

public class DarourPlayer {
  private String playerId;
  private String darourGameId;
  private String userId;
  private String name;
  private String handJson;
  private Integer position;

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }

  public String getDarourGameId() {
    return darourGameId;
  }

  public void setDarourGameId(String darourGameId) {
    this.darourGameId = darourGameId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHandJson() {
    return handJson;
  }

  public void setHandJson(String handJson) {
    this.handJson = handJson;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }
}
