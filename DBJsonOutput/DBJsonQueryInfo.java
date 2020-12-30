import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DBJsonQueryInfo {
	private List<String> tableList = new ArrayList<String>();
	private List<String> fieldQuery = new ArrayList<String>();
	private List<String> fieldMapJson = new ArrayList<String>();
	private String conditionString = null;
	private String sqlString = null;
	private boolean regenerateSql = true;

	public void addTable(String tbDefine) {
		tableList.add(tbDefine);
		regenerateSql = true;
	}

	public void addFieldJsonMap(String field, String jsonField) {
		fieldQuery.add(field);
		fieldMapJson.add(jsonField);
		regenerateSql = true;
	}

	public void setCondition(String condstr) {
		conditionString = condstr;
	}
	
	public void setSqlString(String str) {
		sqlString = str;
		regenerateSql = false;
	}

	public String getSql() {
		if (regenerateSql) {
			StringBuffer strb = new StringBuffer();

			strb.append("select ");
			strb.append(String.format(" %s", fieldQuery.get(0)));
			for (int i = 1; i < fieldQuery.size(); ++i) {
				strb.append(String.format(", %s", fieldQuery.get(i)));
			}
			strb.append(String.format(" from %s", tableList.get(0)));
			for (int i = 1; i < tableList.size(); ++i) {
				strb.append(String.format(", %s", tableList.get(i)));
			}
			if (conditionString != null) {
				strb.append(String.format(" where %s", conditionString));
			}
			sqlString = strb.toString();
			regenerateSql = false;
		}
		return sqlString;
	}

	public JSONObject parseToJSONArray(ResultSet rs) throws Exception {
		JSONObject dbDict = null;
		if (rs != null) {
			dbDict = new JSONObject();
			while (rs.next()) {
				JSONObject tempObject = new JSONObject();
				String tempId = null;
				for (int i = 0; i < fieldMapJson.size(); ++i) {
					if ("id".equals(fieldMapJson.get(i))) {
						tempId = rs.getString(i + 1);
					} else {
						String tempStr = rs.getString(i + 1);
						tempObject.put(fieldMapJson.get(i), tempStr);
						/*
						 * if(tempStr==null){ tempObject.put( fieldMapJson.get(i), tempStr ); }else{
						 * tempObject.put( fieldMapJson.get(i), new String(tempStr.getBytes("UTF-8")) );
						 * }
						 */
					}
				}
				dbDict.put(tempId, tempObject);
			}
		}
		return dbDict;
	}
}
