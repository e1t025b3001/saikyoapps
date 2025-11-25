package team1.saikyoapps.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MatchingQueueMapper {
  @Select("SELECT COUNT(*) FROM matching_queue WHERE status = 'WAITING' AND requested_game = #{game}")
  int countWaitingPlayersByGame(String game);

  @Insert("INSERT INTO matching_queue (user_name, requested_game, status) VALUES (#{userName}, #{requestedGame}, #{status})")
  void insert(String userName, String requestedGame, String status);
}
