package myjdbc.mongodb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.QueryOperators.*;

import org.bson.Document;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;

public class ParseSqlText {
	
	public static Document buildBetWeenCondition(Between betweenExpr){
		Document betweenVal = null;
		Column column = (Column)betweenExpr.getLeftExpression();
		Expression startExpr = betweenExpr.getBetweenExpressionStart();
		Expression endExpr = betweenExpr.getBetweenExpressionEnd();
		if(startExpr instanceof LongValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((LongValue)startExpr).getBigIntegerValue())
					.append("$lte", ((LongValue)endExpr).getBigIntegerValue()));
		} else if(startExpr instanceof DoubleValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((DoubleValue)startExpr).getValue())
					.append("$lte", ((DoubleValue)endExpr).getValue()));
		}else if(startExpr instanceof StringValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((StringValue)startExpr).getValue())
					.append("$lte", ((StringValue)endExpr).getValue()));
		}else if(startExpr instanceof DateValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((DateValue)startExpr).getValue())
					.append("$lte", ((DateValue)endExpr).getValue()));
		}else if(startExpr instanceof TimeValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((TimeValue)startExpr).getValue())
					.append("$lte", ((TimeValue)endExpr).getValue()));
		}else if(startExpr instanceof TimestampValue){
			betweenVal = new Document(column.getColumnName(), 
					new Document("$gte", ((TimestampValue)startExpr).getValue())
					.append("$lte", ((TimestampValue)endExpr).getValue()));
		}
		return betweenVal;
	}
	
	public static Document buildCompareCondition(String name, String condition, Expression expr){
		Document cond = null;
		if(expr instanceof LongValue){
			cond = new Document(name, 
					new Document(condition, ((LongValue)expr).getBigIntegerValue()));
		} else if(expr instanceof DoubleValue){
			cond = new Document(name, 
					new Document(condition, ((DoubleValue)expr).getValue()));
		}else if(expr instanceof StringValue){
			cond = new Document(name, 
					new Document(condition, ((StringValue)expr).getValue()));
		}else if(expr instanceof DateValue){
			cond = new Document(name, 
					new Document(condition, ((DateValue)expr).getValue()));
		}else if(expr instanceof TimeValue){
			cond = new Document(name, 
					new Document(condition, ((TimeValue)expr).getValue()));
		}else if(expr instanceof TimestampValue){
			cond = new Document(name, 
					new Document(condition, ((TimestampValue)expr).getValue()));
		}
		return cond;
	}
	
	public static Document parseWhereStament(Expression expr){
		Document query = new Document();
		if(expr instanceof AndExpression){
			AndExpression andExpression = (AndExpression)expr;
			Document left = parseWhereStament(andExpression.getLeftExpression());
			Document right = parseWhereStament(andExpression.getRightExpression());
			query.append("$and", Arrays.asList(left, right));
		} else if(expr instanceof OrExpression){
			OrExpression orExpression = (OrExpression)expr;
			Document left = parseWhereStament(orExpression.getLeftExpression());
			Document right = parseWhereStament(orExpression.getRightExpression());
			query.append("$or", Arrays.asList(left, right));
		} else if(expr instanceof Between){
			Between betweenExpr = (Between)expr;
			query = buildBetWeenCondition(betweenExpr); 
		} else if(expr instanceof EqualsTo){
			EqualsTo equalsTo = (EqualsTo)expr;
			boolean leftIsColumn = equalsTo.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(equalsTo.getLeftExpression()) : (Column)(equalsTo.getRightExpression());
			Expression valExpr = leftIsColumn? equalsTo.getRightExpression() : equalsTo.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$eq", valExpr);
		} else if(expr instanceof ExistsExpression){
		} else if(expr instanceof GreaterThan){
			GreaterThan greaterThan = (GreaterThan)expr;
			boolean leftIsColumn = greaterThan.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(greaterThan.getLeftExpression()) : (Column)(greaterThan.getRightExpression());
			Expression valExpr = leftIsColumn?greaterThan.getRightExpression() : greaterThan.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$gt", valExpr);
		} else if(expr instanceof GreaterThanEquals){
			GreaterThanEquals greaterThanEquals = (GreaterThanEquals)expr;
			boolean leftIsColumn = greaterThanEquals.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(greaterThanEquals.getLeftExpression()) : (Column)(greaterThanEquals.getRightExpression());
			Expression valExpr = leftIsColumn? greaterThanEquals.getRightExpression() : greaterThanEquals.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$gte", valExpr);
		} else if(expr instanceof InExpression){
		} else if(expr instanceof IsNullExpression){
		} else if(expr instanceof LikeExpression){
		} else if(expr instanceof MinorThan){
			MinorThan minorThan = (MinorThan)expr;
			boolean leftIsColumn = minorThan.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(minorThan.getLeftExpression()) : (Column)(minorThan.getRightExpression());
			Expression valExpr = leftIsColumn? minorThan.getRightExpression() : minorThan.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$lt", valExpr);
		} else if(expr instanceof MinorThanEquals){
			MinorThanEquals minorThanEquals = (MinorThanEquals)expr;
			boolean leftIsColumn = minorThanEquals.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(minorThanEquals.getLeftExpression()) : (Column)(minorThanEquals.getRightExpression());
			Expression valExpr = leftIsColumn? minorThanEquals.getRightExpression() : minorThanEquals.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$lte", valExpr);
		} else if(expr instanceof NotEqualsTo){
			NotEqualsTo notEqualsTo = (NotEqualsTo)expr;
			boolean leftIsColumn = notEqualsTo.getLeftExpression() instanceof Column;
			Column column = leftIsColumn?(Column)(notEqualsTo.getLeftExpression()) : (Column)(notEqualsTo.getRightExpression());
			Expression valExpr = leftIsColumn? notEqualsTo.getRightExpression() : notEqualsTo.getLeftExpression();
			query = buildCompareCondition(column.getColumnName(), "$ne", valExpr);
		}
		return query;
	}
	
	public static void addNameValueToDoc(Document doc, String name, Expression expr){
		if(expr instanceof LongValue){
			doc.append(name, ((LongValue)expr).getBigIntegerValue());
		} else if(expr instanceof DoubleValue){
			doc.append(name, ((DoubleValue)expr).getValue());
		}else if(expr instanceof StringValue){
			doc.append(name, ((StringValue)expr).getValue());
		}else if(expr instanceof DateValue){
			doc.append(name, ((DateValue)expr).getValue());
		}else if(expr instanceof TimeValue){
			doc.append(name, ((TimeValue)expr).getValue());
		}else if(expr instanceof TimestampValue){
			doc.append(name, ((TimestampValue)expr).getValue());
		}
	}
}
