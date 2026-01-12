package team1.saikyoapps.darour.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import team1.saikyoapps.darour.model.Hand;

@MappedJdbcTypes(JdbcType.CLOB)
@MappedTypes(Hand.class)
public class HandTypeHandler extends BaseTypeHandler<Hand> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Hand parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter.serialize());
  }

  @Override
  public Hand getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return Hand.deserialize(rs.getString(columnName));
  }

  @Override
  public Hand getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return Hand.deserialize(rs.getString(columnIndex));
  }

  @Override
  public Hand getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return Hand.deserialize(cs.getString(columnIndex));
  }
}
