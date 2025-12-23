package team1.saikyoapps.darour.model;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DarourGameMapper {
  @Insert("""
      INSERT INTO darour_game (player1, player2, player3, game_state)
      VALUES (#{player1}, #{player2}, #{player3}, #{gameState})
      """)
  void insertDarourGame(DarourGame darourGame);

  @Select("""
      SELECT * FROM darour_game
      WHERE player1 = #{player} OR player2 = #{player} OR player3 = #{player}
      """)
  List<DarourGame> selectDarourGameByPlayer(String player);
}
