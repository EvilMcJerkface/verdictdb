package edu.umich.verdict.datatypes;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DATE;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.TIME;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.TINYINT;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class JdbcResultSetMetaData implements ResultSetMetaData {
  
  DbmsQueryResult queryResult;
  
  List<String> caseSensitiveColumnTypes = Arrays.asList("char", "string", "text");
  
  List<String> signedColumnTypes = Arrays.asList("double", "int", "real");
  
  public JdbcResultSetMetaData(DbmsQueryResult queryResult) {
    this.queryResult = queryResult;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public int getColumnCount() throws SQLException {
    return queryResult.getColumnCount();
  }

  @Override
  public boolean isAutoIncrement(int column) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public boolean isCaseSensitive(int column) throws SQLException {
    String typeName = DataTypeConverter.typeName(queryResult.getColumnType(column-1));
    for (String a : caseSensitiveColumnTypes) {
      if (typeName.contains(a)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isSearchable(int column) throws SQLException {
    return false;
  }

  @Override
  public boolean isCurrency(int column) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public int isNullable(int column) throws SQLException {
    return java.sql.ResultSetMetaData.columnNullableUnknown;
  }

  @Override
  public boolean isSigned(int column) throws SQLException {
    String typeName = DataTypeConverter.typeName(queryResult.getColumnType(column-1));
    for (String a : signedColumnTypes) {
      if (typeName.contains((a))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int getColumnDisplaySize(int column) throws SQLException {
    return Math.max(getPrecision(column-1), queryResult.getColumnName(column-1).length());
  }

  @Override
  public String getColumnLabel(int column) throws SQLException {
    return queryResult.getColumnName(column-1);
  }

  @Override
  public String getColumnName(int column) throws SQLException {
    return queryResult.getColumnName(column-1);
  }

  @Override
  public String getSchemaName(int column) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public int getPrecision(int column) throws SQLException {
    int coltype = queryResult.getColumnType(column-1);
    if (coltype == BIGINT) {
      return 19;
    }
    else if (coltype == INTEGER) {
      return 10;
    }
    else if (coltype == SMALLINT) {
      return 5;
    }
    else if (coltype == TINYINT) {
      return 3;
    }
    else if (coltype == DOUBLE || coltype == NUMERIC) {
      return 64;
    }
    else if (coltype == FLOAT) {
      return 32;
    }
    else if (coltype == DATE) {
      return 10;
    }
    else if (coltype == TIME) {
      return 11;
    }
    else if (coltype == TIMESTAMP) {
      return 22;
    }
    else {
      return 0;
    }
  }

  @Override
  public int getScale(int column) throws SQLException {
    String typeName = DataTypeConverter.typeName(queryResult.getColumnType(column-1));
    if (typeName.contains("double") || typeName.contains("float")) {
      return 10;
    }
    return 0;
  }

  @Override
  public String getTableName(int column) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public String getCatalogName(int column) throws SQLException {
    throw new SQLException("Not supported function.");
  }

  @Override
  public int getColumnType(int column) throws SQLException {
    return queryResult.getColumnType(column-1);
  }

  @Override
  public String getColumnTypeName(int column) throws SQLException {
    return DataTypeConverter.typeName(queryResult.getColumnType(column-1));
  }

  @Override
  public boolean isReadOnly(int column) throws SQLException {
    return true;
  }

  @Override
  public boolean isWritable(int column) throws SQLException {
    return false;
  }

  @Override
  public boolean isDefinitelyWritable(int column) throws SQLException {
    return false;
  }

  @Override
  public String getColumnClassName(int column) throws SQLException {
    return DataTypeConverter.typeName(queryResult.getColumnType(column-1));
  }

}
