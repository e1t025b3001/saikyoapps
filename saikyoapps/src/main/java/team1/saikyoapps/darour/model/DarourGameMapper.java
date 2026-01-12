package team1.saikyoapps.darour.model;

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
      SELECT game_id AS gameId, player1 AS player1, player2 AS player2, player3 AS player3 FROM darour_game
      WHERE player1 = #{player} OR player2 = #{player} OR player3 = #{player}
      """)
  DarourGame selectDarourGameByPlayer(String player);
}
