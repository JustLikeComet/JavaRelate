package myjdbc.mongodb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


public class MongoDBStatement implements Statement {
	
	private MongoDatabase mongoDatabase = null;
		
	public MongoDBStatement(MongoDatabase database){
		mongoDatabase = database;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		SearchRelateDatas searchRelateDatas = new SearchRelateDatas();
		net.sf.jsqlparser.statement.Statement statement = parseSql(sql);
		if(statement==null){
			throw new SQLException("Parse sql error");
		}
		if(statement instanceof Select){
			PlainSelect selectBody = (PlainSelect)((Select) statement).getSelectBody();
			
			Table fromItem = (Table) selectBody.getFromItem();
			
			searchRelateDatas.collection = mongoDatabase.getCollection(fromItem.getName());
			searchRelateDatas.columns = new ArrayList<String>();
			
			final List<SelectItem> list = selectBody.getSelectItems();
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof AllColumns) {
					AllColumns allColumns = (AllColumns) list.get(i);
					searchRelateDatas.columns.add(allColumns.toString());
				} else if (list.get(i) instanceof SelectExpressionItem) {
					SelectExpressionItem sei = (SelectExpressionItem) list.get(i);
					Column c = (Column) sei.getExpression();
					//System.out.println(c.getColumnName());
					searchRelateDatas.columns.add( c.getColumnName() );
				}
			}
			
			Expression where = selectBody.getWhere();
			if(where!=null){
				searchRelateDatas.searchCondition = ParseSqlText.parseWhereStament(where);
			}
			if(searchRelateDatas.searchCondition!=null){
				searchRelateDatas.founds = searchRelateDatas.collection.find(searchRelateDatas.searchCondition);
			}else{
				searchRelateDatas.founds = searchRelateDatas.collection.find();
			}
			return new MongoDBResultSet(searchRelateDatas);
		}
		return null;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		net.sf.jsqlparser.statement.Statement statement = parseSql(sql);
		if(statement==null){
			throw new SQLException("Parse sql error");
		}
		if(statement instanceof Drop){
			Drop drop = (Drop) statement;
			mongoDatabase.getCollection(drop.getName().getName()).drop();
		}else if (statement instanceof Truncate) {
			Truncate truncate = (Truncate) statement;
			truncate.getTable().getName();
			mongoDatabase.getCollection(truncate.getTable().getName()).deleteMany(new Document());
		}else if (statement instanceof Update) {
			Update updateStament = (Update) statement;
			Table table = (Table) updateStament.getTables().get(0);
			MongoCollection<Document> collection = mongoDatabase.getCollection(table.getName());
			List<Column> columnList = updateStament.getColumns();
			List<Expression> exprList = updateStament.getExpressions();
			Document updateDoc = new Document();
			for(int i=0; i<columnList.size(); ++i){
				Column updateColumn = columnList.get(i);
				Expression expr = exprList.get(i);
				ParseSqlText.addNameValueToDoc(updateDoc, updateColumn.getColumnName(), expr);
			}
			Expression where = updateStament.getWhere();
			Document searchCondition = null;
			if(where!=null){
				searchCondition = ParseSqlText.parseWhereStament(where);
			}else{
				searchCondition = new Document();
			}
			collection.updateMany(searchCondition, new Document("$set", updateDoc));
		} else if (statement instanceof Insert) {
			Insert insertStament = (Insert) statement;
			MongoCollection<Document> collection = mongoDatabase.getCollection(insertStament.getTable().getName());
			List<Column> columnList = insertStament.getColumns();
			List<Expression> exprList = ((ExpressionList)(insertStament.getItemsList())).getExpressions();
			Document insertDoc = new Document();
			for(int i=0; i<columnList.size(); ++i){
				Column setColumn = columnList.get(i);
				Expression expr = exprList.get(i);
				ParseSqlText.addNameValueToDoc(insertDoc, setColumn.getColumnName(), expr);
			}
			collection.insertOne(insertDoc);
		} else if (statement instanceof Delete) {
			Delete deleteStament = (Delete) statement;
			MongoCollection<Document> collection = mongoDatabase.getCollection(deleteStament.getTable().getName());
			Expression where = deleteStament.getWhere();
			Document searchCondition = null;
			if(where!=null){
				searchCondition = ParseSqlText.parseWhereStament(where);
			}else{
				searchCondition = new Document();
			}
			//if(searchCondition!=null){
				collection.deleteMany(searchCondition);
			//}else{
			//	collection.deleteMany(null);
			//}
		}
		return 0;
	}

	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancel() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCursorName(String name) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean execute(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int[] executeBatch() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	
	private net.sf.jsqlparser.statement.Statement parseSql(String sql){
		try {
			return CCJSqlParserUtil.parse(sql);
		} catch (JSQLParserException e) {
		}
		return null;
	}

}
