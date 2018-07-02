package edu.umich.verdict;

import edu.umich.verdict.datatypes.JdbcQueryResult;
//import org.h2.jdbc.JdbcResultSetMetaData;
import edu.umich.verdict.datatypes.JdbcResultSetMetaData;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.VARCHAR;
import static org.junit.Assert.assertEquals;

public class jdbcResultSetMetaDataH2Test {


  static Connection conn;

  private static Statement stmt;

  private JdbcResultSetMetaData jdbcResultSetMetaData1;

  private ResultSet rs;

  @BeforeClass
  public static void setupH2Database() throws SQLException {
    final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);

    List<List<Object>> contents = new ArrayList<>();
    contents.add(Arrays.<Object>asList(1, "Anju", "female", 15, 170.2, "USA", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(2, "Sonia", "female", 17, 156.5, "USA", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "Asha", "male", 23, 168.1, "CHN", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "Joe", "male", 14, 178.6, "USA", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "JoJo", "male", 18, 190.7, "CHN", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "Sam", "male", 18, 190.0, "USA", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "Alice", "female", 18, 190.21, "CHN", "2017-10-12 21:22:23"));
    contents.add(Arrays.<Object>asList(3, "Bob", "male", 18, 190.3, "CHN", "2017-10-12 21:22:23"));
    stmt = conn.createStatement();
    stmt.execute("DROP TABLE PEOPLE IF EXISTS");
    stmt.execute("CREATE TABLE PEOPLE(id smallint, name varchar(255), gender varchar(8), age float, height float, nation varchar(8), birth timestamp)");
    for (List<Object> row : contents) {
      String id = row.get(0).toString();
      String name = row.get(1).toString();
      String gender = row.get(2).toString();
      String age = row.get(3).toString();
      String height = row.get(4).toString();
      String nation = row.get(5).toString();
      String birth = row.get(6).toString();
      stmt.execute(String.format("INSERT INTO PEOPLE(id, name, gender, age, height, nation, birth) VALUES(%s, '%s', '%s', %s, %s, '%s', '%s')", id, name, gender, age, height, nation, birth));
    }
  }

  @Before
  public void createJdbcResultSetMetaData() throws SQLException {
    rs = stmt.executeQuery("SELECT gender, count(*) as cnt, avg(age) as ageavg FROM PEOPLE GROUP BY gender");
    JdbcQueryResult queryResult = new JdbcQueryResult(rs);
    jdbcResultSetMetaData1 = new JdbcResultSetMetaData(queryResult);
  }

  @Test
  public void getColumnCountTest() throws SQLException {
    assertEquals(3, jdbcResultSetMetaData1.getColumnCount());
  }

  //@Test
  public void isCaseSensitive() throws SQLException {
    assertEquals(true, jdbcResultSetMetaData1.isCaseSensitive(1));
    assertEquals(false, jdbcResultSetMetaData1.isCaseSensitive(2));
    assertEquals(false, jdbcResultSetMetaData1.isCaseSensitive(3));
  }

  @Test
  public void isSignedTest() throws SQLException {
    assertEquals(false, jdbcResultSetMetaData1.isSigned(1));
    assertEquals(true, jdbcResultSetMetaData1.isSigned(2));
    assertEquals(true, jdbcResultSetMetaData1.isSigned(3));
  }

  @Test
  public void getColumnDisplaySizeTest() throws SQLException {
    assertEquals(rs.getMetaData().getColumnDisplaySize(2), jdbcResultSetMetaData1.getColumnDisplaySize(2));
    assertEquals(rs.getMetaData().getColumnDisplaySize(3), jdbcResultSetMetaData1.getColumnDisplaySize(3));
  }

  @Test
  public void getColumnNameTest() throws SQLException {
    assertEquals("gender", jdbcResultSetMetaData1.getColumnName(1));
    assertEquals("cnt", jdbcResultSetMetaData1.getColumnName(2));
    assertEquals("ageavg", jdbcResultSetMetaData1.getColumnName(3));
  }

  @Test
  public void getColumnLabelTest() throws SQLException {
    assertEquals("gender", jdbcResultSetMetaData1.getColumnLabel(1));
    assertEquals("cnt", jdbcResultSetMetaData1.getColumnLabel(2));
    assertEquals("ageavg", jdbcResultSetMetaData1.getColumnLabel(3));
  }

  @Test
  public void getPrecisionTest() throws SQLException {
    assertEquals(rs.getMetaData().getPrecision(1), jdbcResultSetMetaData1.getPrecision(1));
    assertEquals(rs.getMetaData().getPrecision(2), jdbcResultSetMetaData1.getPrecision(2));
    assertEquals(rs.getMetaData().getPrecision(3), jdbcResultSetMetaData1.getPrecision(3));
  }

  @Test
  public void getScaleTest() throws SQLException {
    assertEquals(rs.getMetaData().getScale(1), jdbcResultSetMetaData1.getScale(1));
    assertEquals(rs.getMetaData().getScale(3), jdbcResultSetMetaData1.getScale(3));
    assertEquals(rs.getMetaData().getScale(3), jdbcResultSetMetaData1.getScale(3));
  }

  @Test
  public void getColumnTypeTest() throws SQLException {
    assertEquals(VARCHAR, jdbcResultSetMetaData1.getColumnType(1));
    assertEquals(BIGINT, jdbcResultSetMetaData1.getColumnType(2));
    assertEquals(DOUBLE, jdbcResultSetMetaData1.getColumnType(3));
  }

  @Test
  public void getColumnTypeNameTest() throws SQLException {
    assertEquals("varchar", jdbcResultSetMetaData1.getColumnTypeName(1));
    assertEquals("bigint", jdbcResultSetMetaData1.getColumnTypeName(2));
    assertEquals("double", jdbcResultSetMetaData1.getColumnTypeName(3));
  }

  @Test
  public void getColumnClassNameTest() throws SQLException {
    assertEquals("java.lang.String", jdbcResultSetMetaData1.getColumnClassName(1));
    assertEquals("java.lang.Long", jdbcResultSetMetaData1.getColumnClassName(2));
    assertEquals("java.lang.Double", jdbcResultSetMetaData1.getColumnClassName(3));
  }

  @Test
  public void isCurrencyTest() throws SQLException {
    assertEquals(rs.getMetaData().isCurrency(1), jdbcResultSetMetaData1.isCurrency(1));
    assertEquals(rs.getMetaData().isCurrency(2), jdbcResultSetMetaData1.isCurrency(2));
    assertEquals(rs.getMetaData().isCurrency(3), jdbcResultSetMetaData1.isCurrency(3));
  }

  @Test
  public void isNullableTest() throws SQLException {
    assertEquals(rs.getMetaData().isNullable(1), jdbcResultSetMetaData1.isNullable(1));
    assertEquals(rs.getMetaData().isNullable(2), jdbcResultSetMetaData1.isNullable(2));
    assertEquals(rs.getMetaData().isNullable(3), jdbcResultSetMetaData1.isNullable(3));
  }
}
