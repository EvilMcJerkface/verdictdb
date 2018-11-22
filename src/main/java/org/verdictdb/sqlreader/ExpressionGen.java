/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.sqlreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.verdictdb.core.sqlobject.AsteriskColumn;
import org.verdictdb.core.sqlobject.BaseColumn;
import org.verdictdb.core.sqlobject.ColumnOp;
import org.verdictdb.core.sqlobject.ConstantColumn;
import org.verdictdb.core.sqlobject.SelectQuery;
import org.verdictdb.core.sqlobject.SubqueryColumn;
import org.verdictdb.core.sqlobject.UnnamedColumn;
import org.verdictdb.parser.VerdictSQLParser;
import org.verdictdb.parser.VerdictSQLParser.Column_nameContext;
import org.verdictdb.parser.VerdictSQLParser.Full_column_nameContext;
import org.verdictdb.parser.VerdictSQLParserBaseVisitor;

import com.google.common.base.Joiner;

public class ExpressionGen extends VerdictSQLParserBaseVisitor<UnnamedColumn> {

  //    private MetaData meta;

  //    public ExpressionGen(MetaData meta) {
  //        this.meta = meta;
  //    }

  public ExpressionGen() {}

  @Override
  public ColumnOp visitInterval(VerdictSQLParser.IntervalContext ctx) {
    String unit = "day";
    if (ctx.DAY() != null || ctx.DAYS() != null) {
      unit = "day";
    } else if (ctx.MONTH() != null || ctx.MONTHS() != null) {
      unit = "month";
    } else if (ctx.YEAR() != null || ctx.YEARS() != null) {
      unit = "year";
    }
    return new ColumnOp(
        "interval",
        Arrays.<UnnamedColumn>asList(
            ConstantColumn.valueOf(ctx.constant_expression().getText()),
            ConstantColumn.valueOf(unit)));
  }

  @Override
  public ColumnOp visitDate(VerdictSQLParser.DateContext ctx) {
    return new ColumnOp(
        "date",
        Arrays.<UnnamedColumn>asList(ConstantColumn.valueOf(ctx.constant_expression().getText())));
  }

  @Override
  public ConstantColumn visitPrimitive_expression(
      VerdictSQLParser.Primitive_expressionContext ctx) {
    return ConstantColumn.valueOf(ctx.getText());
  }

  @Override
  public ConstantColumn visitTime_unit(VerdictSQLParser.Time_unitContext ctx) {
    return ConstantColumn.valueOf(ctx.getText());
  }

  @Override
  public BaseColumn visitColumn_ref_expression(VerdictSQLParser.Column_ref_expressionContext ctx) {
    Full_column_nameContext fullColName = ctx.full_column_name();
    if (fullColName == null) {
      return null;
    }

    Column_nameContext columnName = fullColName.column_name();
    String colName = stripQuote(columnName.getText());
    if (fullColName.table_name() == null) {
      return new BaseColumn(colName);
    }
    String tableName = stripQuote(fullColName.table_name().table.getText());
    if (fullColName.table_name().schema == null) {
      return new BaseColumn(tableName, colName);
    } else {
      return new BaseColumn(
          stripQuote(fullColName.table_name().schema.getText()), tableName, colName);
    }
  }

  private String stripQuote(String expr) {
    return expr.replace("\"", "").replace("`", "");
  }

  @Override
  public ColumnOp visitBinary_operator_expression(
      VerdictSQLParser.Binary_operator_expressionContext ctx) {
    String opType = null;
    if (ctx.op.getText().equals("+")) {
      opType = "add";
    } else if (ctx.op.getText().equals("-")) {
      opType = "subtract";
    } else if (ctx.op.getText().equals("*")) {
      opType = "multiply";
    } else if (ctx.op.getText().equals("/")) {
      opType = "divide";
    } else {
      opType = ctx.op.getText();
    }
    return new ColumnOp(opType, Arrays.asList(visit(ctx.expression(0)), visit(ctx.expression(1))));
  }

  @Override
  public UnnamedColumn visitIs_null_expression(VerdictSQLParser.Is_null_expressionContext ctx) {
    UnnamedColumn left = visit(ctx.expression());

    if (ctx.null_notnull().NOT() == null) {
      return ColumnOp.rightisnull(left); // ?? is null
    } else {
      return ColumnOp.rightisnotnull(left); // ?? is not null
    }
  }

  @Override
  public UnnamedColumn visitNot_expression(VerdictSQLParser.Not_expressionContext ctx) {
    UnnamedColumn col = visit(ctx.expression());

    return ColumnOp.not(col);
  }

  @Override
  public ColumnOp visitFunction_call_expression(
      VerdictSQLParser.Function_call_expressionContext ctx) {
    VerdictSQLParserBaseVisitor<ColumnOp> v =
        new VerdictSQLParserBaseVisitor<ColumnOp>() {

          @Override
          public ColumnOp visitAggregate_windowed_function(
              VerdictSQLParser.Aggregate_windowed_functionContext ctx) {
            String fname;
            UnnamedColumn col = null;
            if (ctx.all_distinct_expression() != null) {
              ExpressionGen g = new ExpressionGen();
              col = g.visit(ctx.all_distinct_expression());
            }

            // OverClause overClause = null;

            if (ctx.AVG() != null) {
              fname = "avg";
            } else if (ctx.SUM() != null) {
              fname = "sum";
            } else if (ctx.COUNT() != null) {
              if (ctx.all_distinct_expression() != null
                  && ctx.all_distinct_expression().DISTINCT() != null) {
                fname = "countdistinct";
              } else {
                fname = "count";
                if (ctx.all_distinct_expression() != null) {
                  ExpressionGen g = new ExpressionGen();
                  col = g.visit(ctx.all_distinct_expression());
                } else {
                  col = new AsteriskColumn();
                }
              }
            } // else if (ctx.NDV() != null) {
            //  fname = FuncName.IMPALA_APPROX_COUNT_DISTINCT;}
            else if (ctx.MIN() != null) {
              fname = "min";
            } else if (ctx.MAX() != null) {
              fname = "max";
            } // else if (ctx.STDDEV_SAMP() != null) {
            //  fname = FuncName.STDDEV_SAMP; }
            else {
              fname = "UNKNOWN";
            }

            //  if (ctx.over_clause() != null) {
            //      overClause = OverClause.from(vc, ctx.over_clause());
            //  }

            return new ColumnOp(fname, col);
          }

          @Override
          public ColumnOp visitUnary_function(VerdictSQLParser.Unary_functionContext ctx) {
            ExpressionGen g = new ExpressionGen();
            //        if (ctx.predicate_function()!=null) {
            //          String fname =
            // ctx.predicate_function().function_name.getText().toLowerCase();
            //          if (ctx.NOT()!=null) {
            //            return new ColumnOp("not " + fname,
            // g.visit(ctx.predicate_function().expression()));
            //          }
            //          else return new ColumnOp(fname, g.visit(ctx.expression()));
            //        }
            String fname = ctx.function_name.getText().toLowerCase();
            if (fname.equals("cast")) {
              List<String> dataTypeStr = new ArrayList<>();
              for (ParseTree child : ctx.cast_as_expression().data_type().children) {
                dataTypeStr.add(child.getText());
              }
              return new ColumnOp(
                  fname,
                  Arrays.asList(
                      g.visit(ctx.cast_as_expression().expression()),
                      ConstantColumn.valueOf(Joiner.on(" ").join(dataTypeStr))));
              // ConstantColumn.valueOf(ctx.cast_as_expression().data_type().getText())));
            } else {
              return new ColumnOp(fname, g.visit(ctx.expression()));
            }
          }

          @Override
          public ColumnOp visitNoparam_function(VerdictSQLParser.Noparam_functionContext ctx) {
            String fname = ctx.function_name.getText().toLowerCase();
            return new ColumnOp(fname);
          }

          @Override
          public ColumnOp visitBinary_function(VerdictSQLParser.Binary_functionContext ctx) {
            String fname = ctx.function_name.getText().toLowerCase();
            ExpressionGen g = new ExpressionGen();
            return new ColumnOp(
                fname,
                Arrays.<UnnamedColumn>asList(
                    g.visit(ctx.expression(0)), g.visit(ctx.expression(1))));
          }

          @Override
          public ColumnOp visitTernary_function(VerdictSQLParser.Ternary_functionContext ctx) {
            String fname = ctx.function_name.getText().toLowerCase();
            ExpressionGen g = new ExpressionGen();
            return new ColumnOp(
                fname,
                Arrays.<UnnamedColumn>asList(
                    g.visit(ctx.expression(0)),
                    g.visit(ctx.expression(1)),
                    g.visit(ctx.expression(2))));
          }

          @Override
          public ColumnOp visitNary_function(VerdictSQLParser.Nary_functionContext ctx) {
            String fname = ctx.function_name.getText().toLowerCase();
            ExpressionGen g = new ExpressionGen();
            List<UnnamedColumn> columns = new ArrayList<>();
            for (VerdictSQLParser.ExpressionContext expressionContext : ctx.expression()) {
              columns.add(g.visit(expressionContext));
            }
            return new ColumnOp(fname, columns);
          }

          @Override
          public ColumnOp visitExtract_time_function(
              VerdictSQLParser.Extract_time_functionContext ctx) {
            String fname = "extract";
            ExpressionGen g = new ExpressionGen();
            return new ColumnOp(
                fname,
                Arrays.<UnnamedColumn>asList(
                    ConstantColumn.valueOf(ctx.extract_unit().getText()),
                    g.visit(ctx.expression())));
          }

          @Override
          public ColumnOp visitOverlay_string_function(
              VerdictSQLParser.Overlay_string_functionContext ctx) {
            String fname = "overlay";
            ExpressionGen g = new ExpressionGen();
            List<UnnamedColumn> operands = new ArrayList<>();
            operands.add(g.visit(ctx.expression(0)));
            operands.add(g.visit(ctx.expression(1)));
            operands.add(g.visit(ctx.expression(2)));
            if (ctx.expression().size() == 4) {
              operands.add(g.visit(ctx.expression(3)));
            }
            return new ColumnOp(fname, operands);
          }

          @Override
          public ColumnOp visitSubstring_string_function(
              VerdictSQLParser.Substring_string_functionContext ctx) {
            String fname = "substring";
            ExpressionGen g = new ExpressionGen();
            List<UnnamedColumn> operands = new ArrayList<>();
            operands.add(g.visit(ctx.expression(0)));
            operands.add(g.visit(ctx.expression(1)));
            if (ctx.expression().size() == 3) {
              operands.add(g.visit(ctx.expression(2)));
            }
            return new ColumnOp(fname, operands);
          }

          @Override
          public ColumnOp visitTimestamp_function(VerdictSQLParser.Timestamp_functionContext ctx) {
            ExpressionGen g = new ExpressionGen();
            return new ColumnOp("timestampwithoutparentheses", g.visit(ctx.expression()));
          }

          @Override
          public ColumnOp visitDateadd_function(VerdictSQLParser.Dateadd_functionContext ctx) {
            String fname = "dateadd";
            ExpressionGen g = new ExpressionGen();
            List<UnnamedColumn> operands = new ArrayList<>();
            operands.add(g.visit(ctx.time_unit()));
            operands.add(g.visit(ctx.expression(0)));
            operands.add(g.visit(ctx.expression(1)));
            return new ColumnOp(fname, operands);
          }
        };
    return v.visit(ctx);
  }

  @Override
  public ColumnOp visitCase_expr(VerdictSQLParser.Case_exprContext ctx) {
    if (ctx.search_condition() != null) {
      List<UnnamedColumn> operands = new ArrayList<>();
      CondGen gen = new CondGen();
      for (VerdictSQLParser.ExpressionContext expressionContext : ctx.expression()) {
        int index = ctx.expression().indexOf(expressionContext);
        if (index != ctx.expression().size() - 1)
          operands.add(gen.visit(ctx.search_condition(index)));
        operands.add(visit(expressionContext));
      }
      return new ColumnOp("casewhen", operands);
    } else {
      List<UnnamedColumn> operands = new ArrayList<>();
      ExpressionGen gen = new ExpressionGen();
      for (VerdictSQLParser.ExpressionContext expressionContext : ctx.expression()) {
        operands.add(gen.visit(expressionContext));
      }
      return new ColumnOp("caseexprwhen", operands);
    }
  }

  @Override
  public UnnamedColumn visitBracket_expression(VerdictSQLParser.Bracket_expressionContext ctx) {
    return visit(ctx.expression());
  }

  @Override
  public SubqueryColumn visitSubquery_expression(VerdictSQLParser.Subquery_expressionContext ctx) {
    RelationGen g = new RelationGen();
    return SubqueryColumn.getSubqueryColumn(
        (SelectQuery) g.visit(ctx.subquery().select_statement()));
  }

  public UnnamedColumn getSearch_condition(List<VerdictSQLParser.Search_conditionContext> ctx) {
    CondGen g = new CondGen();
    if (ctx.size() == 1) {
      return g.visit(ctx.get(0));
    } else {
      UnnamedColumn col = visit(ctx.get(0));
      for (int i = 0; i < ctx.size(); i++) {
        col = new ColumnOp("and", Arrays.asList(col, g.visit(ctx.get(i))));
      }
      return col;
    }
  }
}
