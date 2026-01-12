package team1.saikyoapps.darour.model;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/*
CREATE TABLE IF NOT EXISTS darour_game (
  game_id VARCHAR(64) UNIQUE,
  player1 VARCHAR(255),
  player2 VARCHAR(255),
  player3 VARCHAR(255)
);
 */

@Mapper
public interface DarourGameMapper {
  @Insert("""
      INSERT INTO darour_game (
        game_id,
        player1,
        player2,
        player3
      ) VALUES (
        #{gameID},
        #{player1},
        #{player2},
        #{player3}
      )
      """)
  void insertDarourGame(DarourGame darourGame);

  @Select("""
      SELECT * FROM darour_game
      WHERE player1 = #{player} OR player2 = #{player} OR player3 = #{player}
      """)
  List<DarourGame> selectDarourGameByPlayer(String player);
}
