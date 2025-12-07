package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MatchHistoryMapper {
  @Insert("INSERT INTO match_history (game_name, player1, player2, winner, started_at, ended_at, extra) VALUES (#{gameName}, #{player1}, #{player2}, #{winner}, #{startedAt}, #{endedAt}, #{extra})")
  void insert(String gameName, String player1, String player2, String winner, java.sql.Timestamp startedAt,
      java.sql.Timestamp endedAt, String extra);
}
