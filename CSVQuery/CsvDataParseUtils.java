
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class CsvDataParseUtils  implements Iterable<List<String>> {

	private File csvFile = null;
	private String csvData = null;

	public CsvDataParseUtils(File csvpath) {
		csvFile = csvpath;
		csvData = null;
	}

	public CsvDataParseUtils(String csvData) {
		this.csvData = csvData;
		csvFile = null;
	}

	@Override
	public Iterator<List<String>> iterator() {
		DataIterator iterator = null;
		try {
			iterator = new DataIterator();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return iterator;
	}
	
	private void fixLikeConditionStr(JSONArray condiArray){
		for(int i=0; i<condiArray.size(); ++i){
			if(condiArray.getJSONObject(i).getString("condition").equals("like")){
				String likestr =  condiArray.getJSONObject(i).getString("value");
				
				likestr = likestr.replaceAll("\\.", "\\\\.");
				likestr = likestr.replaceAll("\\[", "\\\\[");
				likestr = likestr.replaceAll("\\]", "\\\\]");
				likestr = likestr.replaceAll("\\{", "\\\\{");
				likestr = likestr.replaceAll("\\}", "\\\\}");
				likestr = likestr.replaceAll("\\-", "\\\\-");
				likestr = likestr.replaceAll("\\+", "\\\\+");
				likestr = likestr.replaceAll("\\^", "\\\\^");
				likestr = likestr.replaceAll("\\^", "\\\\$");
				likestr = likestr.replaceAll("%", ".*");
				
				condiArray.getJSONObject(i).put("value", likestr);
			}
		}
	}
	
	public Iterator<List<String>> query(JSONObject condition) {
		DataIterator iterator = null;
		fixLikeConditionStr(condition.getJSONArray("conditions"));
		try {
			iterator = new DataIterator(condition);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return iterator;
	}
	
	private class DataIterator implements Iterator {
		private BufferedReader in = null;
        private JSONObject condition = null;
        private List<File> dataFileList = null;
        private int nextFileIndex = 0;
        private List<String> currRecord = null;
        private boolean isRecordRead = false;
        
        private boolean checkLogic(List<String> records, JSONArray condiArray){
        	boolean result = false;
        	boolean prevVal = true;
        	boolean currVal = false;
        	for(int i=0; i<condiArray.size(); ++i){
        		JSONObject obj = condiArray.getJSONObject(i);
        		records.get(obj.getIntValue("index"));
    			if(obj.containsKey("conditions")){
    				currVal = ! checkLogic( records, obj.getJSONArray("conditions"));
    			}else{
	        		switch(obj.getString("condition")){
	        		case "<>":
	        			currVal = !records.get(obj.getIntValue("index")).equals(obj.getString("value"));
	        			break;
	        		case "=":
	        			currVal = records.get(obj.getIntValue("index")).equals(obj.getString("value"));
	        			break;
	        		case "<":
	        			currVal = records.get(obj.getIntValue("index")).compareTo(obj.getString("value"))<0;
	        			break;
	        		case ">":
	        			currVal = records.get(obj.getIntValue("index")).compareTo(obj.getString("value"))>0;
	        			break;
	        		case "<=":
	        			currVal = records.get(obj.getIntValue("index")).compareTo(obj.getString("value"))<=0;
	        			break;
	        		case ">=":
	        			currVal = records.get(obj.getIntValue("index")).compareTo(obj.getString("value"))>=0;
	        			break;
	        		case "like":
	        			String likestr = obj.getString("value");
	        			currVal = records.get(obj.getIntValue("index")).matches("^"+likestr+"$");
	        			break;
	        		case "is null":
	        			currVal = records.get(obj.getIntValue("index"))==null;
	        			break;
	        		case "is not null":
	        			currVal = records.get(obj.getIntValue("index"))!=null;
	        			break;
	        		}
    			}
        		if(i==0){
        			prevVal = currVal;
        			if(condiArray.size()==1 || condiArray.getJSONObject(1).getString("logic").equals("or")){
        				if(prevVal){
        					result = true;
        					break;
        				}
        			}
        		}else{
        			if(condiArray.getJSONObject(i+1).getString("logic").equals("and")){
        				if(!(prevVal && currVal) ){
        					prevVal = currVal = false;
        					result = false;
        				}
        				break;
        			}
        			if(condiArray.size()>i+1 && condiArray.getJSONObject(i+1).getString("logic").equals("or")){
        				if(prevVal){
        					result = true;
        					break;
        				}
        			}
        		}
        	}
        	return result;
        }
        
        private boolean checkRecords(List<String> records){
        	if(condition!=null){
        		JSONArray condiArray = condition.getJSONArray("conditions");
        		return checkLogic(records, condiArray);
        	}
        	return true;
        }
        
        private boolean readNextRecord(){
        	if(!isRecordRead){
        		if(currRecord!=null){
        			return true;
        		}
        	}
        	String onerecord = null;
        	while(true){
	        	try {
					onerecord = in.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        	if(onerecord==null){
	        		currRecord = null;
	        		if(dataFileList!=null && nextFileIndex<dataFileList.size()){
	        			try {
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
	        			try {
							in = new BufferedReader(new FileReader(dataFileList.get(nextFileIndex++)));
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							break;
						}
	        			continue;
	        		}
	        		break;
	        	}
	        	
	        	List<String> records = new ArrayList<String>(); 
	            StringTokenizer st=new StringTokenizer(onerecord,",\n\r");
	            while(st.hasMoreTokens()) {
	            	records.add(st.nextToken());
	            }
	            
	            if(checkRecords(records)){
	            	currRecord = records;
	            	isRecordRead = false;
	            	break;
	            }
        	}
            
        	if(onerecord!=null){
        		return true;
        	}
        	return false;
        }

        @Override
        public boolean hasNext() {
        	isRecordRead = true;
            return readNextRecord();
        }

        @Override
        public Object next() {
            return currRecord;
        }

        public DataIterator() throws FileNotFoundException {
        	if(csvFile!=null){
        		if(csvFile.isDirectory()){
        			dataFileList = new ArrayList<File>();
        			File[] dataFileArray = csvFile.listFiles();
        			for(File f : dataFileArray){
        				if( dataFileArray[nextFileIndex].isFile()
            					&& dataFileArray[nextFileIndex].getName().matches(".*[^.\\s]+\\.csv$") ){
        					dataFileList.add(f);
        				}
        			}
        			if(dataFileList.size()>0){
	        			in = new BufferedReader(new FileReader(dataFileList.get(nextFileIndex++)));
        			}else{
        				dataFileList = null;
        			}
        		}else{
        			in = new BufferedReader(new FileReader(csvFile));
        		}
        	}
        	if(csvData!=null){
        		in = new BufferedReader(new StringReader(csvData));
        	}
        	this.condition = null;
        }

        public DataIterator(JSONObject condition) throws FileNotFoundException {
        	if(csvFile!=null){
        		in = new BufferedReader(new FileReader(csvFile));
        	}
        	if(csvData!=null){
        		in = new BufferedReader(new StringReader(csvData));
        	}
            this.condition = condition;
        }
        
        public void close(){
        	if(in!=null){
        		try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        		in = null;
        	}
        }

        @Override
        protected void finalize()
                throws Throwable{
        	if(in!=null){
        		in.close();
        	}
        }
    }
	
	public static void main(String[] args) {
		StringBuffer strb = new StringBuffer();
		strb.append("A,12,650001.light.001,2020-01-02 01:03:20\n" );
		strb.append("B,13,650001.water.004,2020-01-02 02:25:20\n");
		strb.append("C,14,650001.SF3.009,2020-01-02 03:15:21\n");
		strb.append("D,15,650001.oxygen.007,2020-01-02 04:16:21\n");
		CsvDataParseUtils testParser = new CsvDataParseUtils(strb.toString());
		Iterator<List<String>> ite01 = testParser.iterator();
		for(; ite01.hasNext(); ){
			List<String> a = ite01.next();
			System.out.println(a);
		}
		JSONObject obj = new JSONObject();
		obj.put("condition", "=");
		obj.put("index", 1);
		obj.put("value", "13");
		JSONArray array = new JSONArray();
		array.add(obj);
		JSONObject conditionObj = new JSONObject();
		conditionObj.put("conditions", array);
		Iterator<List<String>> ite02 = testParser.query(conditionObj);
		for(; ite02.hasNext(); ){
			List<String> a = ite02.next();
			System.out.println(a);
		}
		array.clear();
		obj = new JSONObject();
		obj.put("condition", "like");
		obj.put("index", 2);
		obj.put("value", "%.light.%");
		array.add(obj);
		conditionObj.clear();
		conditionObj.put("conditions", array);
		Iterator<List<String>> ite03 = testParser.query(conditionObj);
		for(; ite03.hasNext(); ){
			List<String> a = ite03.next();
			System.out.println(a);
		}
	}

}
