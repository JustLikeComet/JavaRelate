import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class Main {
	

	public static void main(String[] args) {

		String netIfName = "\\Device\\NPF_VIRTUALBOX01"; // virtual box net driver
		byte[][] localMacAddr = {{(byte)0xfe, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04}};
		byte[] localIp = {(byte)192, (byte)168, (byte)56, (byte)113};
		byte[] ipMask = {(byte)255, (byte)255, (byte)255, (byte)0};
		byte[] targetIp = {(byte)192, (byte)168, (byte)56, (byte)1};
		TcpIpStack.setNetifName(localMacAddr[0], "myNetIf01");
		TcpIpStack.addNetifIp(localMacAddr[0], localIp, ipMask);
		TcpIpStack.netdriverSimulate = new NetDriverSimulate(netIfName, Arrays.asList(localMacAddr));
		if(false){
			if(false){
				Thread sendThread = new Thread(new Runnable(){
		
					@Override
					public void run() {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
						}
						InetSocketAddress localAddr = new InetSocketAddress("192.168.56.113", 12345);
						InetSocketAddress targetAddr = new InetSocketAddress("192.168.56.1", 12345);
						TcpIpStack.createUdpSocket(localAddr);
						byte[] text = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x0a};
						long startTime = System.currentTimeMillis();
						while(true){
							long currMs = System.currentTimeMillis();
							if((currMs-startTime)>3000){
								startTime = currMs;
								TcpIpStack.sendUdpData(localAddr, targetAddr, text, 0, text.length);
							}
						}
						
					}
					
				});
				sendThread.start();
			}else{
				Thread recevieThread = new Thread(new Runnable(){
		
					@Override
					public void run() {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
						}
						InetSocketAddress localAddr = new InetSocketAddress("192.168.56.113", 12345);
						TcpIpStack.createUdpSocket(localAddr);
						byte[] targetAddr = new byte[6];
						byte[] text = new byte[2048];
						while(true){
							int len = TcpIpStack.receivedUdpData(localAddr, targetAddr, text, 0, text.length);
							if(len>0){
								System.out.println("recevied "+new String(text, 0, len));
							}
						}
						
					}
					
				});
				recevieThread.start();
			}
		}else{
			if(false){
				Thread sendThread = new Thread(new Runnable(){
		
					@Override
					public void run() {
						//try {
						//	Thread.sleep(1000);
						//} catch (InterruptedException e) {
						//}
						InetSocketAddress localAddr = new InetSocketAddress("192.168.56.113", 12345);
						InetSocketAddress targetAddr = new InetSocketAddress("192.168.56.1", 1688);
						TcpIpStack.createTcpSocket(localAddr);
						TcpIpStack.connectTcpSocket(localAddr, targetAddr);
						byte[] text = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x0a};
						long startTime = System.currentTimeMillis();
						while(true){
							long currMs = System.currentTimeMillis();
							if((currMs-startTime)>3000){
								startTime = currMs;
								TcpIpStack.sendTcpData(localAddr, targetAddr, text, 0, text.length);
							}
						}
						
					}
					
				});
				sendThread.start();
			}else{
				Thread recevieThread = new Thread(new Runnable(){
		
					@Override
					public void run() {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						InetSocketAddress localAddr = new InetSocketAddress("192.168.56.113", 12345);
						TcpIpStack.createTcpSocket(localAddr);
						byte[] targetAddr = new byte[6];
						byte[] text = new byte[2048];
						while(true){
							int len = TcpIpStack.receivedTcpData(localAddr, targetAddr, text, 0, text.length);
							if(len>0){
								System.out.println("recevied "+new String(text, 0, len));
							}
						}
						
					}
					
				});
				recevieThread.start();
			}
		}
		

		long startTime = System.currentTimeMillis();
		TcpIpStack.queryIpMacMap();
		while(true){
			long currMs = System.currentTimeMillis();
			if((currMs-startTime)>60000){
				startTime = currMs;
				//TcpIpStack.queryIpMacMap();
				//TcpIpStack.showArpMap();
			}
			TcpIpStack.procInputPackage();
			TcpIpStack.procOutputTcpData();
		}

	}

}
