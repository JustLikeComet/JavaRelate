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
		/*
		byte[] arpPkt = {   (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xfe, (byte)0x00,
				(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x01,
				(byte)0x08, (byte)0x00, (byte)0x06, (byte)0x04, (byte)0x00, (byte)0x01, (byte)0xfe, (byte)0x00, 
				(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0xc0, (byte)0xa8, (byte)0x0b, (byte)0x7b,
				(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xc0, (byte)0xa8,
				(byte)0x0b, (byte)0x0c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 
				};
		*/
		/*
		byte[] broadCastMacAddr = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
		byte[] localMacAddr = {(byte)0xfe, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04};
		int protType = 0x0806;
		byte[] localIp = {(byte)192, (byte)168, (byte)11, (byte)113};
		byte[] targetIp = {(byte)192, (byte)168, (byte)11, (byte)12};
		byte[] arpBuff = new byte[46];
		byte[] ethernetBuff = new byte[2048];
		ArpPackage arpPackage = ArpPackage.newArpPackage(ArpPackage.ARP_REQUEST, localMacAddr, localIp, broadCastMacAddr, targetIp);
		int arpBuffDataLen = ArpPackage.packToArpBuff(arpPackage, arpBuff, 0, 46);
		EtherNetPacket etherNetPacket = EtherNetPacket.newEtherNetPacket(localMacAddr, broadCastMacAddr, protType, arpBuff);
		int arpEtherPackLen = EtherNetPacket.packToEthernetBuff(etherNetPacket, ethernetBuff, 0, 2048);
		
		NativeMapping.PcapErrbuf errbuf = new NativeMapping.PcapErrbuf();
		Pointer handle = NativeMapping.pcap_open_live("\\Device\\NPF_{C495E380-1998-475A-85F0-AD141807F000}", 65536, 1, 1000, errbuf);
		if (handle == null) {
			System.out.println("Open device failed");
			System.exit(-1);
		}
		
		int rc = 0;
		NativeMapping.bpf_program prog = new NativeMapping.bpf_program();
		String filterStr = String.format("ether dst %02X:%02X:%02X:%02X:%02X:%02X", 
				localMacAddr[0], localMacAddr[1], localMacAddr[2], localMacAddr[3], localMacAddr[4], localMacAddr[5]);
		rc = NativeMapping.pcap_compile(handle, prog, filterStr, 1, -1);
		NativeMapping.pcap_freecode(prog);
		
		NativeMapping.pcap_sendpacket(handle, ethernetBuff, arpEtherPackLen);
		long timeStart = System.currentTimeMillis();
		int pktcount = 0;
		while (true) {
			PointerByReference headerPP = new PointerByReference();
			PointerByReference dataPP = new PointerByReference();
			rc = NativeMapping.pcap_next_ex(handle, headerPP, dataPP);
			switch (rc) {
			case 0:
				// timeout
				break;
			case 1:
				Pointer headerP = headerPP.getValue();
				Pointer dataP = dataPP.getValue();
				if (headerP == null || dataP == null) {
				} else {

					// return new PcapPacket(
					// dataP.getByteArray(0, pcap_pkthdr.getCaplen(headerP)),
					// dlt,
					// buildTimestamp(headerP),
					// pcap_pkthdr.getLen(headerP));
					

					final int len = NativeMapping.pcap_pkthdr.getLen(headerP);
					final byte[] buff = dataP.getByteArray(0, NativeMapping.pcap_pkthdr.getCaplen(headerP));
					
					//if (buff[14] == 0 && buff[15] == 1 && buff[16] == 8 && buff[17] == 0 && buff[20] == 0
					//		&& buff[21] == 2) {
					//	String macAddrStr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", buff[22], buff[23],
					//			buff[24], buff[25], buff[26], buff[27]);
					//	System.out.println("arp return macAddrStr " + macAddrStr);
					//}
					
					EtherNetPacket replyEtherNetPkg = new EtherNetPacket();
					EtherNetPacket.extraEthernetPack(replyEtherNetPkg, buff, 0, len);
					if(replyEtherNetPkg.protType==protType && Utils.equals(replyEtherNetPkg.toMac, localMacAddr)){
						ArpPackage replyArpPackage = new ArpPackage();
						ArpPackage.extraArpPack(replyArpPackage, replyEtherNetPkg.extraData, 0, replyEtherNetPkg.extraData.length);
						String macAddrStr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", replyArpPackage.senderMacAddr[0], replyArpPackage.senderMacAddr[1],
								replyArpPackage.senderMacAddr[2], replyArpPackage.senderMacAddr[3], replyArpPackage.senderMacAddr[4], replyArpPackage.senderMacAddr[5]);
						System.out.println("arp return macAddrStr " + macAddrStr);
					}
				}
				break;
			case -1:
				System.out.println("Error occurred in pcap_next_ex(): " + rc);
				System.exit(-1);
			case -2:
				System.out.println("end of file " + rc);
				System.exit(-1);
			default:
				System.out.println("Unexpected error occurred: " + rc);
				System.exit(-1);
			}
			++pktcount;
			if(pktcount>10){
				if(System.currentTimeMillis()-timeStart>3000){
					NativeMapping.pcap_sendpacket(handle, ethernetBuff, arpEtherPackLen);
					pktcount=0;
					timeStart = System.currentTimeMillis();
				}
			}
		}
		*/

		/*
		// send udp package 
		int rc = 0;
		byte[] ethernetBuff = new byte[2048];
		byte[] localMacAddr = {(byte)0xfe, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04};
		byte[] targetMacAddr = {(byte)0x04,(byte)0xfe,(byte)0x8d,(byte)0xbe,(byte)0xd6,(byte)0x2f};
		byte[] localIp = {(byte)192, (byte)168, (byte)11, (byte)113};
		byte[] targetIp = {(byte)192, (byte)168, (byte)11, (byte)12};
		byte[] text = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x0a};
		int ipCheckSum = 0;
		int udpCheckSum = 0;
		int ipIdentifyId = 7131;
		UdpPackage udpPackage = UdpPackage.newUdpPackage(58032, 12345, 8+text.length, udpCheckSum, text);
		IpPackage ipPackage = IpPackage.newIpPackage(4, 20, 0, 0, 32, ipIdentifyId, false, false, 0, 128, 17, 0, localIp, targetIp);
		EtherNetPacket replyEtherNetPkg = EtherNetPacket.newEtherNetPacket(localMacAddr, targetMacAddr, 2048);
		int etherLen = EtherNetPacket.packToEthernetBuff(replyEtherNetPkg, ethernetBuff, 0, 64);
		int ipPackLen = IpPackage.packToIpBuff(ipPackage, ethernetBuff, etherLen, 64-etherLen);
		int udpPackLen = UdpPackage.packToUdpBuff(udpPackage, ethernetBuff, etherLen+ipPackLen, 64-etherLen-ipPackLen);
		//System.arraycopy(text, 0, ethernetBuff, etherLen+ipPackLen+udpPackLen, text.length);
		
		udpCheckSum = UdpPackage.calcUdpCheckSum(udpPackage, ipPackage.sourceIP, ipPackage.destinationIp, udpPackLen, text, 0, text.length);
		ipCheckSum = IpPackage.calcIpHeaderChecksum(ipPackage);
		
		
		NativeMapping.PcapErrbuf errbuf = new NativeMapping.PcapErrbuf();
		Pointer handle = NativeMapping.pcap_open_live("\\Device\\NPF_{C495E380-1998-475A-85F0-AD141807F000}", 65536, 1, 1000, errbuf);
		if (handle == null) {
			System.out.println("Open device failed");
			System.exit(-1);
		}
		
		NativeMapping.bpf_program prog = new NativeMapping.bpf_program();
		String filterStr = String.format("ether dst %02X:%02X:%02X:%02X:%02X:%02X", 
				localMacAddr[0], localMacAddr[1], localMacAddr[2], localMacAddr[3], localMacAddr[4], localMacAddr[5]);
		rc = NativeMapping.pcap_compile(handle, prog, filterStr, 1, -1);
		NativeMapping.pcap_freecode(prog);

		ethernetBuff[24] = (byte)((ipCheckSum>>8)&0xff);
		ethernetBuff[25] = (byte)(ipCheckSum&0xff);
		ethernetBuff[40] = (byte)((udpCheckSum>>8)&0xff);
		ethernetBuff[41] = (byte)(udpCheckSum&0xff);
		NativeMapping.pcap_sendpacket(handle, ethernetBuff, etherLen+ipPackLen+udpPackLen);
		long timeStart = System.currentTimeMillis();
		while (true) {
			if(System.currentTimeMillis()-timeStart>3000){
				ipIdentifyId+=2;
				ethernetBuff[18] = (byte)((ipIdentifyId>>8)&0xff);
				ethernetBuff[19] = (byte)(ipIdentifyId&0xff);
				ipPackage.identification = ipIdentifyId;
				udpCheckSum = UdpPackage.calcUdpCheckSum(udpPackage, ipPackage.sourceIP, ipPackage.destinationIp, udpPackLen,
						udpPackage.extraData, 0, udpPackage.extraData.length);
				ipCheckSum = IpPackage.calcIpHeaderChecksum(ipPackage);
				ethernetBuff[24] = (byte)((ipCheckSum>>8)&0xff);
				ethernetBuff[25] = (byte)(ipCheckSum&0xff);
				ethernetBuff[40] = (byte)((udpCheckSum>>8)&0xff);
				ethernetBuff[41] = (byte)(udpCheckSum&0xff);
				NativeMapping.pcap_sendpacket(handle, ethernetBuff, etherLen+ipPackLen+udpPackLen);
				timeStart = System.currentTimeMillis();
			}
		}
		*/
		String netIfName = "\\Device\\NPF_{C8E924E8-3C99-411D-8A9B-5A13CCC5E393}"; // virtual box net driver
		//String netIfName = "\\Device\\NPF_{C495E380-1998-475A-85F0-AD141807F000}"; // actual net driver
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
