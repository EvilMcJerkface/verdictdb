package org.verdictdb.sqlsyntax;

public class H2Syntax extends SqlSyntax {

  // The column index that stored meta information in the original database

  @Override
  public int getSchemaNameColumnIndex() {
    return 0;
  }

  @Override
  public int getTableNameColumnIndex() {
    return 0;
  }

  @Override
  public int getColumnNameColumnIndex() {
    return 0;
  }

  @Override
  public int getColumnTypeColumnIndex() {
    return 1;
  }

  @Override
  public String getQuoteString() {
    return "\"";
  }

  @Override
  public void dropTable(String schema, String tablename) { }

  @Override
  public boolean doesSupportTablePartitioning() {
    return false;
  }

  @Override
  public String randFunction() {
    return "rand()";
  }

  @Override
  public String getSchemaCommand() {
    return "show schemas";
  }

  @Override
  public String getTableCommand(String schema) {
    return "show tables from " + quoteName(schema);
  }

  @Override
  public String getColumnsCommand(String schema, String table) {
    return "show columns from " + quoteName(table) + " from " + quoteName(schema);
  }

  /**
   * H2 does not support partitioning.
   */
  @Override
  public String getPartitionCommand(String schema, String table) {
    return null;
  }

  /**
   * H2 does not support partitioning.
   */
  @Override
  public String getPartitionByInCreateTable() {
    return null;
  }
  
  @Override
  public boolean isAsRequiredBeforeSelectInCreateTable() {
    return true;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) {
      return false;
    }
    return true;
  }
  

}