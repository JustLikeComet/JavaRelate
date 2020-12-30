import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class MysqlOperate {

	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	private static final String DB_URLFMT = "jdbc:mysql://%s:%d/%s?serverTimezone=GMT%%2B8&useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
	private String mysqlServer = "127.0.0.1";
	private String mysqlDB = "test";
	private int mysqlPort = 3306;
	private String USER = "root";
	private String PASS = null;
	private Connection conn = null;

	static {
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public MysqlOperate(String server, int port, String user, String passwd, String dbname) {
		mysqlServer = server;
		mysqlPort = port;
		USER = user;
		PASS = passwd;
		mysqlDB = dbname;
	}

	public void reconnect() throws Exception {
		if (conn != null) {
			conn.close();
			conn = null;
		}
		conn = DriverManager.getConnection(String.format(DB_URLFMT, mysqlServer, mysqlPort, mysqlDB), USER, PASS);
	}

	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			conn = null;
		}
	}

	public JSONObject query(DBJsonQueryInfo dbJsonInfo) throws Exception {
		Statement stmt = conn.createStatement();
		String sql = dbJsonInfo.getSql();
		ResultSet rs = stmt.executeQuery(sql);
		JSONObject result = dbJsonInfo.parseToJSONArray(rs);
		rs.close();
		stmt.close();
		return result;
	}
	/*
	public static void main(String[] args) {
		MysqlOperate opr = new MysqlOperate("192.168.1.222", 3306, "database", "user", "pass");
		try {
			opr.reconnect();
			DBJsonQueryInfo dbJsonInfo = new DBJsonQueryInfo();

			dbJsonInfo.addTable("cms_distribution_sensor");
			dbJsonInfo.addFieldJsonMap("ID", "id");
			dbJsonInfo.addFieldJsonMap("MAIN_ID", "MAINId");
			dbJsonInfo.addFieldJsonMap("name", "name");
			dbJsonInfo.addFieldJsonMap("CATEGORY", "category");
			dbJsonInfo.addFieldJsonMap("ISSWITCH", "switch");
			dbJsonInfo.addFieldJsonMap("TYPE_ID", "typeId");
			dbJsonInfo.addFieldJsonMap("`TYPE`", "type");
			dbJsonInfo.addFieldJsonMap("SUB_TYPE", "subType");
			dbJsonInfo.addFieldJsonMap("VALUE_UNIT", "unit");
			dbJsonInfo.addFieldJsonMap("QRCODE", "qrcode");
			dbJsonInfo.addFieldJsonMap("ENABLE", "enable");
			dbJsonInfo.addFieldJsonMap("ORG_ID", "orgId");
			dbJsonInfo.addFieldJsonMap("SUB_ORG_ID", "subOrgId");

			System.out.println(opr.query(dbJsonInfo).toString(SerializerFeature.PrettyFormat,
					SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat));

		} catch (SQLException se) {
			// 处理 JDBC 错误
			se.printStackTrace();
		} catch (Exception e) {
			// 处理 Class.forName 错误
			e.printStackTrace();
		} finally {
			opr.close();
		}
		System.out.println("Goodbye!");
	}
	*/

}
