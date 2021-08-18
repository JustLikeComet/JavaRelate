package myjdbc.mongodb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class MongoDBDriver implements Driver {
	
    private final static String connectStrPrefix = "jdbc:mongo:";

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		if (!acceptsURL(url)) {
		    return null;
		}
		String mongoUrlStr = url.replace(connectStrPrefix, "mongodb:");
		
		
		return new MongoDBConnection(mongoUrlStr);
	}

	private boolean checkConnetionUrl(String url){
		String tempUrlStr = url.replace(connectStrPrefix, "http:");
		URI tempUrl = null;
		try {
			tempUrl = new URI(tempUrlStr);
		} catch (URISyntaxException e) {
			return false;
		}
		return tempUrl==null?false:true;
	}
	
	@Override
	public boolean acceptsURL(String url) throws SQLException {
		if(url.startsWith(connectStrPrefix) && checkConnetionUrl(url) ){
			return true;
		}
		return false;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {

		DriverPropertyInfo propInfos[] = new DriverPropertyInfo[0];

		return propInfos;
	}

	@Override
	public int getMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

    static {
		try {
		    DriverManager.registerDriver(new MongoDBDriver());
		} catch (Exception e) {
		}
    }
    
	public static void main(String[] args) throws SQLException {
		try {
			Class.forName("myjdbc.mongodb.MongoDBDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Connection conn = DriverManager.getConnection("jdbc:mongo://127.0.0.1:27017/TestDB", "", "");
		java.sql.Statement statement = conn.createStatement();
		statement.executeUpdate("insert into test01 (c1,c2,c3) values ('123','456','789')");
		statement.executeUpdate("update test01 set c2='ABC' where c1='123'");
		ResultSet rs = statement.executeQuery("select * from test01");
		while(rs.next()){
			System.out.println(String.format("%s %s %s", rs.getString(1), rs.getString(2), rs.getString(3)));
		}
		rs.close();
		rs = null;
		statement.executeUpdate("delete from test01");
	}

}
