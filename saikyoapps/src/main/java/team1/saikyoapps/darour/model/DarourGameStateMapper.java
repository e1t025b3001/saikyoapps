package team1.saikyoapps.darour.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DarourGameStateMapper {
  @Insert("""
      INSERT INTO darour_game_state (
        game_id,
        player1_hand,
        player2_hand,
        player3_hand,
        current_player_index,
        last_played_player_index,
        table_combination
      ) VALUES (
        #{gameID},
        #{player1Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        #{player2Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        #{player3Hand,
            typeHandler=team1.saikyoapps.darour.typehandler.HandTypeHandler},
        #{currentPlayerIndex},
        #{lastPlayedPlayerIndex},
        #{tableCombination,
            typeHandler=team1.saikyoapps.darour.typehandler.CombinationTypeHandler}
      )
      """)
  void insertDarourGameState(DarourGameState darourGameState);

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
        table_combination = #{tableCombination,
            typeHandler=team1.saikyoapps.darour.typehandler.CombinationTypeHandler}
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
