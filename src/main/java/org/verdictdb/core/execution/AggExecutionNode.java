package org.verdictdb.core.execution;

import java.util.ArrayList;
import java.util.List;

import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.query.*;
import org.verdictdb.core.rewriter.ScrambleMeta;
import org.verdictdb.exception.VerdictDbException;

public class AggExecutionNode extends CreateTableAsSelectExecutionNode {
  
  String resultSchemaName;
  
  String resultTableName;

  public AggExecutionNode(
      DbmsConnection conn,
      String resultSchemaName, 
      String resultTableName,
      SelectQuery query) {
    super(conn, resultSchemaName, resultTableName, query);
    try {
      generateDependency();
    } catch (VerdictDbException e){
      e.printStackTrace();
    }
  }

  /**
   * Make this agg execution node perform progressive aggregation.
   * 
   * @param scrambleMeta
   * @throws VerdictDbException 
   */
  public QueryExecutionNode toAsyncAgg(ScrambleMeta scrambleMeta) throws VerdictDbException {
    QueryExecutionNode newNode = new AsyncAggExecutionNode(conn, scrambleMeta, resultSchemaName, resultTableName, query);
    
    // make that newNode runs only after the dependencies of this current node complete.
    List<QueryExecutionNode> leaves = newNode.getLeafNodes();
    for (QueryExecutionNode leaf : leaves) {
      for (QueryExecutionNode dep : getDependents()) {
        leaf.addDependency(dep);
      }
    }
    return newNode;
  }

  @Override
  public ExecutionResult executeNode(List<ExecutionResult> downstreamResults) {
    // TODO Auto-generated method stub
    return super.executeNode(downstreamResults);
  }

  void generateDependency() throws VerdictDbException {
    // from list
    for (AbstractRelation table : query.getFromList()) {
      int index = query.getFromList().indexOf(table);
      // If table is subquery, we need to add it to dependency
      if (table instanceof SelectQuery) {
        String temptableName = QueryExecutionPlan.generateTempTableName();
        if (table.isAggregateQuery()) {
          addDependency(new AggExecutionNode(conn, schemaName, temptableName, (SelectQuery) table));
        } else {
          addDependency(new ProjectionExecutionNode(conn, schemaName, temptableName, (SelectQuery) table));
        }
        if (table.getAliasName().isPresent()) {
          query.getFromList().set(index, new BaseTable(schemaName, temptableName, table.getAliasName().get()));
        } else query.getFromList().set(index, new BaseTable(schemaName, temptableName, temptableName));
      } else if (table instanceof JoinTable) {
        for (AbstractRelation jointable : ((JoinTable) table).getJoinList()) {
          int joinindex = ((JoinTable) table).getJoinList().indexOf(jointable);
          if (jointable instanceof SelectQuery) {
            String temptableName = QueryExecutionPlan.generateTempTableName();
            if (table.isAggregateQuery()) {
              addDependency(new AggExecutionNode(conn, schemaName, temptableName, (SelectQuery) jointable));
            } else {
              addDependency(new ProjectionExecutionNode(conn, schemaName, temptableName, (SelectQuery) jointable));
            }
            if (jointable.getAliasName().isPresent()) {
              ((JoinTable) table).getJoinList().set(joinindex, new BaseTable(schemaName, temptableName, jointable.getAliasName().get()));
            } else ((JoinTable) table).getJoinList().set(joinindex, new BaseTable(schemaName, temptableName, temptableName));
          }
        }
      }
    }

    // Filter
    if (query.getFilter().isPresent()) {
      UnnamedColumn where = query.getFilter().get();
      List<UnnamedColumn> filters = new ArrayList<>();
      filters.add(where);
      while (!filters.isEmpty()) {
        UnnamedColumn filter = filters.get(0);
        filters.remove(0);
        // If filter is a subquery, we need to add it to dependency
        if (filter instanceof SubqueryColumn) {
          String temptableName = QueryExecutionPlan.generateTempTableName();
          if (((SubqueryColumn) filter).getSubquery().isAggregateQuery()) {
            addDependency(new AggExecutionNode(conn, schemaName, temptableName, ((SubqueryColumn) filter).getSubquery()));
          } else {
            addDependency(new ProjectionExecutionNode(conn, schemaName, temptableName, ((SubqueryColumn) filter).getSubquery()));
          }
          // To replace the subquery, we use the selectlist of the subquery and tempTable to create a new non-aggregate subquery
          List<SelectItem> newSelectItem = new ArrayList<>();
          for (SelectItem item:((SubqueryColumn) filter).getSubquery().getSelectList()) {
            if (item instanceof AliasedColumn) {
              newSelectItem.add(new AliasedColumn(new BaseColumn(schemaName, temptableName,
                  ((AliasedColumn) item).getAliasName()), ((AliasedColumn) item).getAliasName()));
            } else if (item instanceof AsteriskColumn) {
              newSelectItem.add(new AsteriskColumn());
            } else throw new VerdictDbException("Select list contains SelectItem type that is not AliasedColumn or AsteriskColumn");
          }
          SelectQuery newSubquery = SelectQuery.getSelectQueryOp(newSelectItem, new BaseTable(schemaName, temptableName, temptableName));
          if (((SubqueryColumn) filter).getSubquery().getAliasName().isPresent()) {
            newSubquery.setAliasName(((SubqueryColumn) filter).getSubquery().getAliasName().get());
          }
          ((SubqueryColumn) filter).setSubquery(newSubquery);
        } else if (filter instanceof ColumnOp) {
          filters.addAll(((ColumnOp) filter).getOperands());
        }
      }
    }
  }
}
