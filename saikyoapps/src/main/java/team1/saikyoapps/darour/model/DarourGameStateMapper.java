package team1.saikyoapps.darour.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DarourGameStateMapper {
  @Update("""
      UPDATE darour_game_state
      SET
        player1_hand = #{player1Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        player2_hand = #{player2Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        player3_hand = #{player3Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        current_player_index = #{currentPlayerIndex},
        last_played_player_index = #{lastPlayedPlayerIndex},
        table_combination = #{tableCombination}
      WHERE game_id = #{gameID}
      """)
  void updateDarourGameState(DarourGameState darourGameState);

  @Select("""
      SELECT * FROM darour_game_state
      WHERE game_id = #{gameID}
      LIMIT 1
      """)
  DarourGameState selectDarourGameStateByGameID(String gameID);
}
