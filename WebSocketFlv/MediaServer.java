import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class MediaServer extends WebSocketServer {
	
	
	private DataInputStream fileInStream = null;
	private WebSocket testConn = null;

	public MediaServer( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
	}

	public MediaServer( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connect server " );
		testConn = conn;
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		System.out.println( conn + " has left the server!" );
		if(fileInStream!=null){
			try {
				fileInStream.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		broadcast( message );
		System.out.println( conn + ": " + message );
	}
	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) {
		broadcast( message.array() );
		System.out.println( conn + ": " + message );
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
		if(fileInStream!=null){
			try {
				fileInStream.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void loopSendData(){
		if(testConn==null) return ;
		
		byte[] dataBuff = new byte[512*1024];
		int availableLen =  0;
		int buffPos = 0;
		try {
			buffPos = fileInStream.read(dataBuff, 0, 9);
			buffPos = 9;
		} catch (IOException e1) {
			e1.printStackTrace();
			testConn.close();
			return ;
		}
		if(dataBuff[0]!='F' || dataBuff[1]!='L' || dataBuff[2]!='V') {
			testConn.close();
			return ;
		}
		availableLen = ((dataBuff[5]&0xff)<<24)+((dataBuff[6]&0xff)<<16)+((dataBuff[7]&0xff)<<8)+dataBuff[8];
		if(availableLen>9){
			try {
				fileInStream.read(dataBuff, 9, availableLen-9);
				buffPos += 9;
			} catch (IOException e) {
				e.printStackTrace();
				testConn.close();
				return ;
			}
		}
		while(true){
			if(fileInStream!=null){
				availableLen =  0;
				try {
					availableLen = fileInStream.available();
				} catch (IOException e) {
				}
				if(availableLen<1){
					break;
				}
				try {
					fileInStream.read(dataBuff, buffPos, 15);
				} catch (IOException e) {
					e.printStackTrace();
					testConn.close();
					return ;
				}
				availableLen = ((dataBuff[buffPos+5]&0xff)<<16)+((dataBuff[buffPos+6]&0xff)<<8)+(dataBuff[buffPos+7]&0xff);
				buffPos += 15;
				try {
					fileInStream.read(dataBuff, buffPos, availableLen);
				} catch (IOException e) {
					e.printStackTrace();
					//testConn.close();
					try {
						fileInStream.reset();
					} catch (IOException e1) {
					}
					return ;
				}
				buffPos+=availableLen;
				//dataBuffer.flip();
				byte[] tempDatas = new byte[buffPos];
				System.arraycopy(dataBuff, 0, tempDatas, 0, buffPos);
				System.out.println("--- send ");
				testConn.send(tempDatas);
				//dataBuffer.compact();
				buffPos = 0;
				{
					try {
						Thread.sleep(3);
					} catch (InterruptedException e) {
					}
				}
			}
			{
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}
		
		
		/*
		int availableLen = 0;
		while(true){
			availableLen = 0;
			try {
				availableLen = fileInStream.available();
			} catch (IOException e) {
			}
			if(availableLen==0){
				break;
			}
			if(availableLen>16*1024){
				availableLen = 16*1024;
			}
			byte[] dataBuff = new byte[availableLen];
			try {
				fileInStream.read(dataBuff);
				testConn.send(dataBuff);
			} catch (IOException e) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		*/
		
		try {
			fileInStream.reset();
		} catch (IOException e1) {
		}
	}

	public static void main( String[] args ) throws InterruptedException , IOException {
		int port = 8889; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}
		MediaServer s = new MediaServer( port );
		s.start();
		System.out.println( "Media Server started on port: " + s.getPort() );
		
		/*
		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			//s.broadcast( in );
			if( in.equals( "exit" ) ) {
				s.stop(1000);
				break;
			}
		}
		*/
		while(true){
			s.loopSendData();
			Thread.sleep(100);
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
		try{
			fileInStream = new DataInputStream((InputStream)new FileInputStream(new File("G:\\test0001.flv")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
