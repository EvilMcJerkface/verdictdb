package edu.umich.verdict.datatypes;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcQueryResult implements DbmsQueryResult {

  List<String> columnNames = new ArrayList<>();

  List<Integer> columnTypes = new ArrayList<>();

//  ResultSet resultSet;

  List<List<Object>> result = new ArrayList<>();

  int cursor = -1;

  DbmsQueryResultMetaData dbmsQueryResultMetaData = new DbmsQueryResultMetaData();

  public JdbcQueryResult(ResultSet resultSet) throws SQLException {

    List<Boolean> isCurrency = new ArrayList<>();
    List<Integer> isNullable = new ArrayList<>();
    List<Integer> precision = new ArrayList<>();
    List<Integer> scale = new ArrayList<>();
    List<Integer> columnDisplaySize = new ArrayList<>();
    List<Boolean> isAutoIncrement = new ArrayList<>();
    List<String> columnClassName = new ArrayList<>();

    ResultSetMetaData meta = resultSet.getMetaData();
    int columnCount = meta.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      columnNames.add(meta.getColumnLabel(i+1).toLowerCase());
      columnTypes.add(meta.getColumnType(i+1));
      precision.add(meta.getPrecision(i+1));
      scale.add(meta.getScale(i+1));
      columnDisplaySize.add(meta.getColumnDisplaySize(i+1));
      isNullable.add(meta.isNullable(i+1));
      isCurrency.add(meta.isCurrency(i+1));
      isAutoIncrement.add(meta.isAutoIncrement(i+1));
      columnClassName.add(meta.getColumnClassName(i+1));
    }
    dbmsQueryResultMetaData.columnDisplaySize = columnDisplaySize;
    dbmsQueryResultMetaData.isAutoIncrement = isAutoIncrement;
    dbmsQueryResultMetaData.isCurrency = isCurrency;
    dbmsQueryResultMetaData.isNullable = isNullable;
    dbmsQueryResultMetaData.precision = precision;
    dbmsQueryResultMetaData.scale = scale;
    dbmsQueryResultMetaData.columnClassName = columnClassName;


    while (resultSet.next()) {
      List<Object> row = new ArrayList<>();
      for (int i=0; i< columnCount; i++) {
        row.add(resultSet.getObject(i+1));
      }
      result.add(row);
    }
  }

  @Override
  public DbmsQueryResultMetaData getMetaData() {
    return dbmsQueryResultMetaData;
  }

  public void setColumnName(int index, String name) {
    columnNames.set(index, name);
  }

  @Override
  public int getColumnCount() {
    return columnNames.size();
  }

  @Override
  public String getColumnName(int index) {
    return columnNames.get(index);
  }

  @Override
  public int getColumnType(int index) {
    return columnTypes.get(index);
  }

  @Override
  public boolean next() {
    if (cursor<result.size()-1) {
      cursor++;
      return true;
    }
    else return false;
    /*
    boolean nextExists = false;
    try {
      nextExists = resultSet.next();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return nextExists;
    */
  }

  @Override
  public Object getValue(int index) {
    Object value = null;
    try {
      value = (Object) result.get(cursor).get(index-1);
      // value = resultSet.getObject(index + 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return value;
  }

  @Override
  public void printContent() {
    StringBuilder row;
    boolean isFirstCol = true;
    
    // print column names
    row = new StringBuilder();
    for (String col : columnNames) {
      if (isFirstCol) {
        row.append(col);
        isFirstCol = false;
      }
      else {
        row.append("\t" + col);
      }
    }
    System.out.println(row.toString());
    
    // print contents
    int colCount = getColumnCount();
    while(this.next()) {
      row = new StringBuilder();
      for (int i = 0; i < colCount; i++) {
        if (i == 0) {
          row.append(getValue(i).toString());
        }
        else {
          row.append("\t");
          row.append(getValue(i).toString());
        }
      }
      System.out.println(row.toString());
    }
    
  }

  public List<List<Object>> getResult() {
    return result;
  }
}
