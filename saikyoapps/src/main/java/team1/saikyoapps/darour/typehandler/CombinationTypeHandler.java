package team1.saikyoapps.darour.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import team1.saikyoapps.darour.model.Combination;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(Combination.class)
public class CombinationTypeHandler extends BaseTypeHandler<Combination> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Combination parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter.serialize());
  }

  @Override
  public Combination getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return Combination.deserialize(rs.getString(columnName));
  }

  @Override
  public Combination getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return Combination.deserialize(rs.getString(columnIndex));
  }

  @Override
  public Combination getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return Combination.deserialize(cs.getString(columnIndex));
  }
}
