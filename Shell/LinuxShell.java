import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LinuxShell {

	private static final int PATH_ACCESS_OK = 0;
	private static final int PATH_ACCESS_DENIED = -1;
	private static final int PATH_NOT_EXIST = -2;
	
	private InputStream errInStream;
	private InputStream stdInStream;
	private OutputStream stdOutStream;
	private Process process ;
	private long lastUpdateTime = 0;
	private StringBuffer strbuffer = new StringBuffer();
	private Map<String, String> envMap = new HashMap<String, String>();
	
	private static final String[] initEnvNames = {"HOSTNAME","SHELL","HISTSIZE","PERL_MB_OPT",
										"JRE_HOME","USER","PATH","PWD",
										"JAVA_HOME","LANG","HOME","PERL_LOCAL_LIB_ROOT",
										"LOGNAME","CLASSPATH","PERL_MM_OPT","NODE_HOME", "LD_LIBRARY_PATH"};
	
	public LinuxShell(InetSocketAddress addr){
		inetaddr = addr;
		lastUpdateTime = System.currentTimeMillis();
		
		for(String name : initEnvNames){
			String val = System.getenv(name);
			if(val==null){
				val = "";
			}
			envMap.put( name, val );
		}
		envMap.put("_", "");
		appendCommandPromot();
	}
	
	private void appendCommandPromot(){
		strbuffer.append(String.format("[%s:%s]$ ", envMap.get("USER"), envMap.get("HOSTNAME")));
	}

	public void parseInputData(byte[] buff, int len) {
		if(stdOutStream==null){
			String cmdStr = new String(buff, 0, len);
			if(analyseInnerCommands(cmdStr)){
			}else{
				if(cmdStr.trim().length()==0){
					appendCommandPromot();
				}else{
					try {
						String[] envArray = envMapToArray();
						String workingDir = envMap.get("PWD");
						//process = Runtime.getRuntime().exec(cmdStr, envArray);
						String[] command = {"/bin/bash", "-c", cmdStr};
						process = Runtime.getRuntime().exec(command, envArray, new File(workingDir));
						errInStream = process.getErrorStream();
						stdInStream = process.getInputStream();
						stdOutStream = process.getOutputStream();
					} catch (IOException e) {
						e.printStackTrace();
						__innerClose();
						strbuffer.append(String.format("command %s execute error\n", cmdStr));
						appendCommandPromot();
					}
				}
			}
		}else{
			try {
				stdOutStream.write(buff, 0, len);
				stdOutStream.write((byte)10);
				stdOutStream.flush();
				lastUpdateTime = System.currentTimeMillis();
			} catch (IOException e) {
				e.printStackTrace();
				__innerClose();
				appendCommandPromot();
			}
		}
	}

	public byte[] procCommandOutput() {
		int stdinsize = 0 ;
		int errinsize = 0 ;
		byte[] buff = null;
		if(stdInStream!=null){
			try {
				stdinsize = stdInStream.available();
			} catch (IOException e) {
				//e.printStackTrace();
				__innerClose();
				appendCommandPromot();
			}
		}
		if(errInStream!=null){
			try {
				errinsize = errInStream.available();
			} catch (IOException e) {
				//e.printStackTrace();
				__innerClose();
				appendCommandPromot();
			}
		}
		if(stdinsize>0 || errinsize>0){
			buff = new byte[stdinsize+errinsize];
		}
		if(errInStream!=null){
			if(errinsize>0){
				try {
					errInStream.read(buff, 0, errinsize);
					//lastUpdateTime = System.currentTimeMillis();
				} catch (IOException e) {
					//e.printStackTrace();
					__innerClose();
					appendCommandPromot();
				}
			}
		}
		if(stdInStream!=null){
			if(stdinsize>0){
				try {
					stdInStream.read(buff, errinsize, stdinsize);
					//lastUpdateTime = System.currentTimeMillis();
				} catch (IOException e) {
					//e.printStackTrace();
					__innerClose();
					appendCommandPromot();
				}
			}
		}
		if(buff!=null && buff.length>0){
			strbuffer.append(new String(buff));
		}
		if(process!=null && !process.isAlive()){
			__innerClose();
			appendCommandPromot();
		}
		buff = strbuffer.toString().getBytes();
		strbuffer.delete(0, strbuffer.length());
		return buff;
	}

	public void close() {
		__innerClose();
	}

	private void __innerClose(){
		if(stdInStream!=null){
			try {
				stdInStream.close();
			} catch (IOException e1) {
			}
		}
		stdInStream = null;
		if(stdOutStream!=null){
			try {
				stdOutStream.close();
			} catch (IOException e1) {
			}
		}
		stdOutStream = null;
		if(errInStream!=null){
			try {
				errInStream.close();
			} catch (IOException e1) {
			}
		}
		errInStream = null;
		if(process!=null){
			process.destroyForcibly();
			try {
				process.waitFor();
			} catch (InterruptedException e) {
			}
		}
		process = null;
	}
	
	private boolean analyseInnerCommands( String cmdStr ){
		if(analyseCommandExport(cmdStr)){
			return true;
		}else if(analyseCommandEnv(cmdStr)){
			return true;
		}else if(analyseCommandChangdir(cmdStr)){
			return true;
		}else if(analyseCommandPwd(cmdStr)){
			return true;
		}
		return false;
	}
	
	private boolean analyseCommandExport( String cmdStr ){
		
		String exportCheckPattern = "\\s*export\\s+.*";
		
		if(cmdStr.matches(exportCheckPattern)){
			Pattern p1 = Pattern.compile("\\s*export\\s+([A-zA-Z0-9_]+)=([^\r\n\t]*)");
			Matcher m = p1.matcher(cmdStr);
			if(m!=null && m.find()){
				String envname = m.group(1);
				String envValue = m.group(2);
				envMap.put(envname, envValue);
			}else{
				strbuffer.append( "export error\n" );
			}
			appendCommandPromot();
			return true;
		}
		return false;
	}
	
	private boolean analyseCommandEnv( String cmdStr ){
		
		String envCheckPattern = "\\s*env\\s*";
		
		if(cmdStr.matches(envCheckPattern)){
			for(String k : envMap.keySet()){
				strbuffer.append( String.format("%s=%s\n", k, envMap.get(k)) );
			}
			appendCommandPromot();
			return true;
		}
		
		return false;
	}
	
	private int checkDirectoryExistAndAccessOk(String pathname){
		File fpath = new File(pathname);
		if(fpath.exists()){
			if(fpath.canRead()){
				return PATH_ACCESS_OK;
			}else{
				return PATH_ACCESS_DENIED;
			}
		}
		return PATH_NOT_EXIST;
	}
	
	private String toWellformatedPath(String pathname){
		File fpath = new File(pathname);
		try{
			return fpath.getCanonicalPath();
		}catch(IOException e){
		}
		return fpath.getAbsolutePath();
	}
	
	private boolean analyseCommandChangdir( String cmdStr ){
		
		String changedirCheckPattern = "\\s*cd\\s+.*";
		
		if(cmdStr.matches(changedirCheckPattern)){
			Pattern p1 = Pattern.compile("\\s*cd\\s+(.*)");
			Matcher m = p1.matcher(cmdStr);
			if(m!=null && m.find()){
				String pathname = m.group(1);
				String tempPathName = null;
				if(pathname.charAt(0)=='/'){
					tempPathName = toWellformatedPath(pathname);
				}else if(pathname.equals("~")){
					tempPathName = envMap.get("PWD");
				}else{
					tempPathName = toWellformatedPath( String.format("%s/%s", envMap.get("PWD"), pathname) );
				}
				int accessResult = checkDirectoryExistAndAccessOk(tempPathName);
				switch(accessResult){
					case PATH_ACCESS_OK:
						envMap.put("PWD", tempPathName);
						break;
					case PATH_ACCESS_DENIED:
						strbuffer.append( String.format("Path %s access denied\n", pathname) );
						break;
					case PATH_NOT_EXIST:
					default:
						strbuffer.append( String.format("Path %s not exist\n", pathname) );
						break;
				}
			}else{
				strbuffer.append( "changedir error\n" );
			}
			appendCommandPromot();
			return true;
		}
		return false;
	}
	
	private boolean analyseCommandPwd( String cmdStr ){
		
		String pwdCheckPattern = "\\s*pwd\\s*";
		
		if(cmdStr.matches(pwdCheckPattern)){
			strbuffer.append( envMap.get("PWD")+"\n" );
			appendCommandPromot();
			return true;
		}
		return false;
	}
	
	private String[] envMapToArray(){
		String[] envArray = new String[envMap.size()];
		int count = 0;
		for(String k : envMap.keySet()){
			envArray[count++] = String.format("%s=%s", k, envMap.get(k));
		}
		return envArray;
	}

	public static void main(String[] args) throws Exception {
		
		LinuxShell cmdRunner = new LinuxShell(null);
		Thread a = new Thread(){
			public void run(){
				while(true){
					try {
						byte[] data = cmdRunner.procCommandOutput();
						System.out.print(new String(data));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(3);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		a.start();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			String cmdstr = in.readLine();
			if(cmdstr.trim().equals("exit")){
				break;
			}
			if(cmdstr!=null){
				if(cmdstr.length()==0){
					cmdstr = "\r\n";
				}
				cmdRunner.parseInputData(cmdstr.getBytes(), cmdstr.getBytes().length);
			}
		}
		
		System.exit(0);
		
		
	}

}
