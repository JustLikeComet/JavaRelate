import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class TcpIpStack {

	
	private final static int ArpEtherType = 0x0806;
	private final static int IpEtherType = 0x0800;
	
	private final static int TcpProtol = 6;
	private final static int UdpProtol = 17;
	
	private final static int SocketBufferSize = 256*1024;
	
	private final static byte[] broadCastMacAddr = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
	
	static class NetSettingNode {
		public byte[] ipAddr;
		public byte[] mask;
	}
	
	static class NetifNode {
		public String name;
		//public byte[] macAddr;
		public List<NetSettingNode> netSettingList;
	}

	private static long overTimeMS = 0;
	private final static int ipMacMapOneRecordSize = 16;
	private final static int ipMacMapMaxRecord = 1000;
	private final static int ipMacMapMaxRecordBufferSize = ipMacMapOneRecordSize*ipMacMapMaxRecord;
	// format buff[0] markused buff[1-4] ip buff[5-10] mac  
	private static byte[] ipMacMapArray = new byte[ipMacMapMaxRecordBufferSize];

	private final static int reouteTableOneRecordSize = 16;
	private final static int reouteTableMaxRecord = 1000;
	private final static int reouteTableMaxRecordBufferSize = reouteTableOneRecordSize*reouteTableMaxRecord;
	private static byte[] reouteTableArray = new byte[reouteTableMaxRecordBufferSize];
	
	private static Map<byte[], NetifNode> netIfMap = new HashMap<byte[], NetifNode>();

	private static Map<String, List<SocketStruct> > socketBufferMap = new HashMap<String, List<SocketStruct> >(); 
	
	private static EtherNetPacket remoteEtherNetPkg = new EtherNetPacket();
	private static ArpPackage remoteArpPackage = new ArpPackage();
	private static IpPackage remoteIpPackage = new IpPackage();
	private static UdpPackage remoteUdpPackage = new UdpPackage();
	private static TcpPackage remoteTcpPackage = new TcpPackage();
	
	public static NetDriverSimulate netdriverSimulate;
	private static byte[] driverInputBuffer = new byte[0x10000];
	
	private static Random random = new Random();
	
	private static SocketStruct findSocketStruct(byte[] localBindAddr, int localport, byte[] remoteAddr, int remoteport, int type){
		String mapId = null;
		if(type==17){
			mapId = String.format("UDP %d.%d.%d.%d %d", 
				localBindAddr[0]&0xff, localBindAddr[1]&0xff, localBindAddr[2]&0xff, localBindAddr[3]&0xff, localport);
		}else if(type==6){
			mapId = String.format("TCP %d.%d.%d.%d %d", 
					localBindAddr[0]&0xff, localBindAddr[1]&0xff, localBindAddr[2]&0xff, localBindAddr[3]&0xff, localport);
		}
		List<SocketStruct> socketList = Collections.synchronizedList(socketBufferMap.get(mapId));
		if(socketList!=null){
			for(SocketStruct s : socketList){
				if(ByteUtils.equals(s.remoteAddr, remoteAddr) && s.remotePort==remoteport && s.type==type){
					return s;
				}
			}
		}
		return null;
	}
	
	public static void setNetifName(byte[] macAddr, String name){
		if(!netIfMap.containsKey(macAddr)){
			NetifNode node = new NetifNode();
			node.netSettingList = new ArrayList<NetSettingNode>();
			netIfMap.put(macAddr, node);
		}
		netIfMap.get(macAddr).name = name;
	}
	
	private static boolean existIpSetting(byte[] ip, List<NetSettingNode> settingNodeList){
		for(NetSettingNode n : settingNodeList){
			if(n.ipAddr.equals(ip)){
				return true;
			}
		}
		return false;
	}
	
	public static void addNetifIp(byte[] macAddr, byte[] ip, byte[] mask){
		synchronized(netIfMap){
			if(!netIfMap.containsKey(macAddr)){
				NetifNode node = new NetifNode();
				node.netSettingList = new ArrayList<NetSettingNode>();
				netIfMap.put(macAddr, node);
			}
			if(!existIpSetting(ip, netIfMap.get(macAddr).netSettingList)){
				NetSettingNode n = new NetSettingNode();
				n.ipAddr = ip;
				n.mask = mask;
				netIfMap.get(macAddr).netSettingList.add(n);
			}
		}
	}
	
	private static void addIpMacToMapArray(byte[] ip, byte[] macAddr){
		int storePos = -1;
		boolean ipExist = false;
		synchronized(ipMacMapArray){
			//System.out.println("--- 1 ");
			for(int i=0; i<ipMacMapMaxRecordBufferSize; i+=ipMacMapOneRecordSize){
				if(storePos<0 && ipMacMapArray[i]==0){
					storePos = i;
				}
				if(ipMacMapArray[i]>0){
					//System.out.println(String.format("--- 2 %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X",
					//		ipMacMapArray[i], ipMacMapArray[i+1], ipMacMapArray[i+2], ipMacMapArray[i+3], ipMacMapArray[i+4], ipMacMapArray[i+5],
					//		ipMacMapArray[i+6], ipMacMapArray[i+7], ipMacMapArray[i+8], ipMacMapArray[i+9], ipMacMapArray[i+10]));
					if(ByteUtils.equals(ipMacMapArray, i+1, ip, 0, ip.length)){
						if(ByteUtils.equals(ipMacMapArray, i+5, macAddr, 0, macAddr.length)){
							storePos = -1;
							ipExist = true;
						}else{
							storePos = i;
						}
						break;
					}
				}
			}
			if(storePos>-1){
				//System.out.println("--- storePos "+storePos);
				ipMacMapArray[storePos] = 1;
				System.arraycopy(ip, 0, ipMacMapArray, storePos+1, ip.length);
				System.arraycopy(macAddr, 0, ipMacMapArray, storePos+5, macAddr.length);
			}
		}
	}
	
	private static boolean existIpMacInMapArray(byte[] ip){
		for(int i=0; i<ipMacMapMaxRecordBufferSize; i+=ipMacMapOneRecordSize){
			if(ipMacMapArray[i]>0 && ByteUtils.equals(ipMacMapArray, i+1, ip, 0, ip.length)){
				return true;
			}
		}
		return false;
	}
	
	private static byte[] findIpMacInMapArray(byte[] ip){
		for(int i=0; i<ipMacMapMaxRecordBufferSize; i+=ipMacMapOneRecordSize){
			if(ipMacMapArray[i]>0 && ByteUtils.equals(ipMacMapArray, i+1, ip, 0, ip.length)){
				byte[] macAddr = new byte[6];
				System.arraycopy(ipMacMapArray, i+5, macAddr, 0, macAddr.length);
				return macAddr;
			}
		}
		return null;
	}
	
	public static byte[] showArpMap(){
		System.out.println("arp :");
		for(int i=0; i<ipMacMapMaxRecordBufferSize; i+=ipMacMapOneRecordSize){
			if(ipMacMapArray[i]>0){
				System.out.println(String.format("IP %d.%d.%d.%d MAC %02X:%02X:%02X:%02X:%02X:%02X",
						ipMacMapArray[1]&0xff,ipMacMapArray[2]&0xff,ipMacMapArray[3]&0xff,ipMacMapArray[4]&0xff,
						ipMacMapArray[5],ipMacMapArray[6],ipMacMapArray[7],ipMacMapArray[8],ipMacMapArray[9],ipMacMapArray[10]));
			}
		}
		return null;
	}
	
	private static int procInputArpPackage(byte[] buff, int off, int len){
		ArpPackage.extraArpPack(remoteArpPackage, buff, off, len);
		if(remoteArpPackage.opcode==ArpPackage.ARP_REQUEST){
			
			byte[] localMacAddr = getIpMacAddr(remoteArpPackage.targeIp);
			if(localMacAddr==null) return -1;
			
			byte[] arpBuff = new byte[64];
			
			ArpPackage arpPackage = ArpPackage.newArpPackage(ArpPackage.ARP_REPLY, localMacAddr, remoteArpPackage.targeIp, remoteArpPackage.senderMacAddr, remoteArpPackage.sendIp);
			EtherNetPacket etherNetPacket = EtherNetPacket.newEtherNetPacket(localMacAddr, remoteEtherNetPkg.fromMac, ArpEtherType);
			int arpEtherPackLen = EtherNetPacket.packToEthernetBuff(etherNetPacket, arpBuff, 0, 46);
			int arpBuffDataLen = ArpPackage.packToArpBuff(arpPackage, arpBuff, arpEtherPackLen, 46-arpEtherPackLen);
			netdriverSimulate.putDataToOutputBuffer(arpBuff, 0, arpEtherPackLen+arpBuffDataLen);
			addIpMacToMapArray(remoteArpPackage.sendIp, remoteArpPackage.senderMacAddr);
		}else if(remoteArpPackage.opcode==ArpPackage.ARP_REPLY){
			byte[] localMacAddr = getIpMacAddr(remoteArpPackage.sendIp);
			if(localMacAddr!=null) return -1;
			addIpMacToMapArray(remoteArpPackage.sendIp, remoteArpPackage.senderMacAddr);
		}
		
		return -1;
	}
	
	private static void parseInMsgAndSendReplyTcpPackge(byte[] buff, SocketStruct destSocket, List<TcpPackage.TcpOption> optionList) {
		byte[] tcpbuff = new byte[128];
		System.arraycopy(buff, 6, tcpbuff, 0, 6);
		System.arraycopy(buff, 0, tcpbuff, 6, 6);
		System.arraycopy(buff, 12, tcpbuff, 12, 2);
		
		boolean syn = false, ack = true;
		if(optionList!=null){
			syn = true;
		}
		int tcpHdrLen = optionList!=null?(TcpPackage.calcTcpOptoinLen(optionList)+20):20;
		TcpPackage replyTcpPkg = TcpPackage.newTcpPackage(remoteTcpPackage.destinationPort, remoteTcpPackage.sourcePort,
				destSocket.sequenceId, destSocket.acknowledgementId,
				tcpHdrLen,
				false, false, false, false, ack, false, false, syn, false,
				0x00007fff, 0, 0, optionList, null);
		IpPackage replyIpPkg = IpPackage.newIpPackage(4, 20, 0, 0, 20+tcpHdrLen /*tcp data len*/, destSocket.identifyId, false, false, 0, 128, 6, 0, 
				remoteIpPackage.destinationIp, remoteIpPackage.sourceIP);
		

		int etherLen = 14;
		int ipPackLen = IpPackage.packToIpBuff(replyIpPkg, tcpbuff, etherLen, tcpbuff.length-etherLen);
		int tcpPackLen = TcpPackage.packToTcpBuff(replyTcpPkg, tcpbuff, etherLen+ipPackLen, tcpbuff.length-etherLen-ipPackLen);

		int tcpCheckSum = TcpPackage.calcTcpChecksum(replyTcpPkg, replyIpPkg.sourceIP, replyIpPkg.destinationIp, tcpPackLen, null, 0, 0);
		int ipCheckSum = IpPackage.calcIpHeaderChecksum(replyIpPkg);
		
		tcpbuff[24] = (byte)((ipCheckSum>>8)&0xff);
		tcpbuff[25] = (byte)(ipCheckSum&0xff);
		tcpbuff[50] = (byte)((tcpCheckSum>>8)&0xff);
		tcpbuff[51] = (byte)(tcpCheckSum&0xff);
		
		if(netdriverSimulate!=null){
			netdriverSimulate.putDataToOutputBuffer(tcpbuff, 0, etherLen+ipPackLen+tcpPackLen);
		}
	}
	
	private static int procInputIpPackage(byte[] buff, int off, int len){
		int ipPkgLen = IpPackage.extraIpPack(remoteIpPackage, buff, off, len);
		if((IpPackage.calcIpHeaderChecksum(remoteIpPackage)&0xff)!=0){
			return -1;
		}
		if(remoteIpPackage.protocolType==TcpProtol){
			int tcpPkgLen = TcpPackage.extraTcpPack(remoteTcpPackage, false, buff, off+ipPkgLen, len-ipPkgLen);
			int checkSum = TcpPackage.calcTcpBuffChecksum(remoteIpPackage.sourceIP, remoteIpPackage.destinationIp, remoteIpPackage.ipPkgTotalLen-ipPkgLen,
					buff, off+ipPkgLen, remoteIpPackage.ipPkgTotalLen-ipPkgLen)&0xffff;
			
			//if(checkSum!=remoteTcpPackage.checkSum){
			if(0!=checkSum){
				return -1;
			}
			

			String mapId = String.format("TCP %d.%d.%d.%d %d", remoteIpPackage.destinationIp[0]&0xff, remoteIpPackage.destinationIp[1]&0xff,
					 remoteIpPackage.destinationIp[2]&0xff, remoteIpPackage.destinationIp[3]&0xff,
					 remoteTcpPackage.destinationPort);
			
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}
			
			SocketStruct destSocket = findSocketStruct(remoteIpPackage.destinationIp, remoteTcpPackage.destinationPort, remoteIpPackage.sourceIP, remoteTcpPackage.sourcePort,TcpProtol);
			
			if(destSocket==null){
				if(TcpPackage.calcTcpMark(remoteTcpPackage)==TcpPackage.SYN_MARK){
					destSocket = new SocketStruct();
					destSocket.remoteAddr = remoteIpPackage.sourceIP;
					destSocket.remotePort = remoteTcpPackage.sourcePort;
					destSocket.identifyId = random.nextInt(1800)+6000;
					destSocket.initInputBuffer(SocketBufferSize);
					destSocket.initOutputBuffer(SocketBufferSize);
					destSocket.type = TcpProtol;
					destSocket.sequenceId = random.nextInt(1000)+2000;
					destSocket.acknowledgementId = remoteTcpPackage.sequenceNumber+1;
					socketBufferMap.get(mapId).add(destSocket);
					
					parseInMsgAndSendReplyTcpPackge( buff, destSocket, remoteTcpPackage.optionList);
					destSocket.status = 2;
					destSocket.identifyId+=2;
				}else{
					return -1;
				}
			}else{
				int tcpMark = TcpPackage.calcTcpMark(remoteTcpPackage);
				if(tcpMark==TcpPackage.SYN_ACK_MARK){
					if(destSocket.status<4 &&  destSocket.sequenceId==remoteTcpPackage.acknowledgementNumber-1){
						destSocket.sequenceId ++;
						destSocket.acknowledgementId = remoteTcpPackage.sequenceNumber+1;
						
						parseInMsgAndSendReplyTcpPackge( buff, destSocket, null);
						destSocket.status = 4;
						destSocket.identifyId+=2;
					}
				} else if(tcpMark==TcpPackage.ACK_MARK) {
					if(destSocket.status==2){
						//if(destSocket.sequenceId==remoteTcpPackage.acknowledgementNumber-1){
							destSocket.sequenceId = remoteTcpPackage.acknowledgementNumber;
							destSocket.acknowledgementId = remoteTcpPackage.sequenceNumber;
							destSocket.status = 4;
						//}
					}else if(destSocket.status==4) {
						if(destSocket.acknowledgementId == remoteTcpPackage.sequenceNumber
								&& destSocket.sequenceId<remoteTcpPackage.acknowledgementNumber){
							if(destSocket.getTcpOutputPos()==remoteTcpPackage.acknowledgementNumber-destSocket.sequenceId){
								destSocket.sequenceId = remoteTcpPackage.acknowledgementNumber;
								destSocket.compactTcpOutputData();
							}
						}
					}
				} else if(tcpMark==TcpPackage.PUSH_ACK_MARK) {
					if(destSocket.acknowledgementId == remoteTcpPackage.sequenceNumber
							&& destSocket.sequenceId==remoteTcpPackage.acknowledgementNumber){
						destSocket.acknowledgementId = remoteTcpPackage.sequenceNumber+remoteIpPackage.ipPkgTotalLen-ipPkgLen-tcpPkgLen;
						parseInMsgAndSendReplyTcpPackge( buff, destSocket, null);
						destSocket.writeInputData(buff, off+ipPkgLen+tcpPkgLen, remoteIpPackage.ipPkgTotalLen-ipPkgLen-tcpPkgLen);
					}
				} else if(remoteTcpPackage.fin){
					System.out.println("--- fin received");
					destSocket.sequenceId = remoteTcpPackage.acknowledgementNumber;
					destSocket.acknowledgementId = remoteTcpPackage.sequenceNumber;
					destSocket.status = 5;
					parseInMsgAndSendReplyTcpPackge( buff, destSocket, null);
					buildAndSendTcpPackage(remoteIpPackage.destinationIp, remoteTcpPackage.destinationPort, 
							remoteIpPackage.sourceIP, remoteTcpPackage.sourcePort, destSocket, false, true, null, -1, -1);
				}
			}
			
		}else if(remoteIpPackage.protocolType==UdpProtol){
			int udpPkgLen = UdpPackage.extraUdpPack(remoteUdpPackage, false, buff, off+ipPkgLen, len-ipPkgLen);
			int checkSum = UdpPackage.calcUdpCheckSum(remoteUdpPackage, remoteIpPackage.sourceIP, remoteIpPackage.destinationIp, remoteIpPackage.ipPkgTotalLen-ipPkgLen,
					buff, off+ipPkgLen+udpPkgLen, remoteIpPackage.ipPkgTotalLen-ipPkgLen-udpPkgLen)&0xffff;
			
			if(checkSum!=remoteUdpPackage.checksum){
				return -1;
			}
			

			String mapId = String.format("UDP %d.%d.%d.%d %d", remoteIpPackage.destinationIp[0]&0xff, remoteIpPackage.destinationIp[1]&0xff,
					 remoteIpPackage.destinationIp[2]&0xff, remoteIpPackage.destinationIp[3]&0xff,
					 remoteUdpPackage.destinationPort);
			
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}
			
			SocketStruct destSocket = findSocketStruct(remoteIpPackage.destinationIp, remoteUdpPackage.destinationPort, remoteIpPackage.sourceIP, remoteUdpPackage.sourcePort,UdpProtol);
			if(destSocket==null){
				destSocket = new SocketStruct();
				destSocket.remoteAddr = remoteIpPackage.sourceIP;
				destSocket.remotePort = remoteUdpPackage.sourcePort;
				destSocket.identifyId = random.nextInt(1800)+6000;
				destSocket.initInputBuffer(SocketBufferSize);
				//socketStruct.outBuffer = new byte[SocketBufferSize];
				destSocket.type = UdpProtol;
				socketBufferMap.get(mapId).add(destSocket);
			}
			
			if(destSocket!=null){
				synchronized(destSocket){
					destSocket.writeInputData(buff, off+udpPkgLen+ipPkgLen, remoteIpPackage.ipPkgTotalLen-udpPkgLen-ipPkgLen);
				}
			}
		}
		return -1;
	}
	
	public static void procInputPackage(){
		int len = 0;
		if(netdriverSimulate!=null){
			len = netdriverSimulate.getDataFromInputBuffer(driverInputBuffer, 0, driverInputBuffer.length);
		}
		if(len>0){
			int etherLen = EtherNetPacket.extraEthernetPack(remoteEtherNetPkg, driverInputBuffer, 0, len);
			if(remoteEtherNetPkg.protType==ArpEtherType){
				procInputArpPackage(driverInputBuffer, etherLen, len-etherLen);
			}else if(remoteEtherNetPkg.protType==IpEtherType){
				procInputIpPackage(driverInputBuffer, etherLen, len-etherLen);
			}
		}
	}
	
	private static void sendArpQueryPackage(byte[] targetIp, byte[] sendMacAddr, byte[] sendIp){
		byte[] buff = new byte[64];
		
		if(existIpMacInMapArray(targetIp)){
			return ;
		}

		ArpPackage arpPackage = ArpPackage.newArpPackage(ArpPackage.ARP_REQUEST, sendMacAddr, sendIp, broadCastMacAddr, targetIp);
		EtherNetPacket etherNetPacket = EtherNetPacket.newEtherNetPacket(sendMacAddr, broadCastMacAddr, ArpEtherType);
		
		int arpEtherPackLen = EtherNetPacket.packToEthernetBuff(etherNetPacket, buff, 0, buff.length);
		int arpBuffDataLen = ArpPackage.packToArpBuff(arpPackage, buff, arpEtherPackLen, buff.length-arpEtherPackLen);
		if(netdriverSimulate!=null){
			netdriverSimulate.putDataToOutputBuffer(buff, 0, arpEtherPackLen+arpBuffDataLen);
		}
		
	}
	
	private static byte[] getIpMacAddr(byte[] ip){
		for(byte[] mac : netIfMap.keySet()){
			List<NetSettingNode> settingNodes = netIfMap.get(mac).netSettingList;
			for(NetSettingNode n : settingNodes){
				if(ByteUtils.equals(n.ipAddr, ip)){
					return mac;
				}
			}
		}
		return null;
	}
	
	public static void queryIpMacMap(){
		for(byte[] macAddr : netIfMap.keySet()){
			for(NetSettingNode netif : netIfMap.get(macAddr).netSettingList){
				byte[] ip = netif.ipAddr.clone();
				int myself = ip[3]&0xff;
				for(int i=1; i<255; ++i){
					ip[3] = (byte)(i&0xff);
					if(i!=myself && !existIpMacInMapArray(ip)){
						sendArpQueryPackage(ip, macAddr, netif.ipAddr);
					}
				}
			}
		}
	}
	
	// tcp operation methods 
	public static int createUdpSocket(InetSocketAddress bindAddr){
		String mapId = String.format("UDP %s %d", bindAddr.getHostName(), bindAddr.getPort());
		if(socketBufferMap.containsKey(mapId)){
			return -1;
		}
		List<SocketStruct> list = new ArrayList<SocketStruct>();
		//SocketStruct socketStruct = new SocketStruct();
		//socketStruct.identifyId = random.nextInt(1800)+6000;
		//socketStruct.inBuffer = new byte[SocketBufferSize];
		//socketStruct.outBuffer = new byte[SocketBufferSize];
		//socketStruct.type = UdpProtol;
		//list.add(socketStruct);
		socketBufferMap.put(mapId, list);
		
		return 0;
	}
	
	public static int sendUdpData(InetSocketAddress fromAddr, InetSocketAddress toAddr, byte[] buff, int off, int len){
		if(netdriverSimulate!=null){
			byte[] udpbuff = new byte[2048];
			String mapId = String.format("UDP %s %d", fromAddr.getHostString(), fromAddr.getPort());
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}
			
			SocketStruct socketStruct =  findSocketStruct(fromAddr.getAddress().getAddress(), fromAddr.getPort(),
					toAddr.getAddress().getAddress(), toAddr.getPort(), UdpProtol);
			if(socketStruct==null){
				socketStruct = new SocketStruct();
				socketStruct.identifyId = random.nextInt(1800)+6000;
				socketStruct.initInputBuffer(SocketBufferSize);
				//socketStruct.outBuffer = new byte[SocketBufferSize];
				socketStruct.type = UdpProtol;
				socketBufferMap.get(mapId).add(socketStruct);
			}
			
			byte[] macAddr = getIpMacAddr(fromAddr.getAddress().getAddress());
			
			byte[] targetMacAddr = findIpMacInMapArray(toAddr.getAddress().getAddress());
			if(targetMacAddr==null){
				targetMacAddr = broadCastMacAddr;
			}
	
			UdpPackage udpPackage = UdpPackage.newUdpPackage(fromAddr.getPort(), toAddr.getPort(), 8+len, 0, null);
			IpPackage ipPackage = IpPackage.newIpPackage(4, 20, 0, 0, 20+8+len, socketStruct.identifyId, false, false, 0, 128, 17, 0, 
					fromAddr.getAddress().getAddress(), toAddr.getAddress().getAddress());
			EtherNetPacket replyEtherNetPkg = EtherNetPacket.newEtherNetPacket(macAddr, targetMacAddr, 2048);
	
			int etherLen = EtherNetPacket.packToEthernetBuff(replyEtherNetPkg, udpbuff, 0, udpbuff.length);
			int ipPackLen = IpPackage.packToIpBuff(ipPackage, udpbuff, etherLen, udpbuff.length-etherLen);
			int udpPackLen = UdpPackage.packToUdpBuff(udpPackage, udpbuff, etherLen+ipPackLen, udpbuff.length-etherLen-ipPackLen);
			System.arraycopy(buff, off, udpbuff, etherLen+ipPackLen+udpPackLen, len);
	
			int udpCheckSum = UdpPackage.calcUdpCheckSum(udpPackage, ipPackage.sourceIP, ipPackage.destinationIp, 8+len, buff, off, len);
			int ipCheckSum = IpPackage.calcIpHeaderChecksum(ipPackage);
			
			udpbuff[24] = (byte)((ipCheckSum>>8)&0xff);
			udpbuff[25] = (byte)(ipCheckSum&0xff);
			udpbuff[40] = (byte)((udpCheckSum>>8)&0xff);
			udpbuff[41] = (byte)(udpCheckSum&0xff);
			
			netdriverSimulate.putDataToOutputBuffer(udpbuff, 0, etherLen+ipPackLen+udpPackLen+len);
			
			socketStruct.identifyId += 2;
			return 0;
		}
		return -1;
	}
	
	public static int receivedUdpData(InetSocketAddress fromAddr, byte[] toAddr, byte[] buff, int off, int len){
		if(netdriverSimulate!=null){
			int size = 0;
			byte[] udpbuff = new byte[2048];
			String mapId = String.format("UDP %s %d", fromAddr.getHostString(), fromAddr.getPort());
			
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}
			
			List<SocketStruct> socketList = Collections.synchronizedList(socketBufferMap.get(mapId));

			for(SocketStruct s : socketList){
				synchronized(s){
					size = s.readInputData(buff, off, len);
					if(size>0){
						System.arraycopy(s.remoteAddr, 0, toAddr, 0, 4);
						toAddr[4] = (byte)((s.remotePort>>8)&0xff);
						toAddr[5] = (byte)(s.remotePort&0xff);
					}
				}
				if(size>0){
					break;
				}
			}
			return size;
		}
		return -1;
	}
	
	public static int closeUdpSocket(InetSocketAddress bindAddr){
		String mapId = String.format("UDP %s %d", bindAddr.getHostName(), bindAddr.getPort());
		if(!socketBufferMap.containsKey(mapId)){
			return -1;
		}
		synchronized(socketBufferMap){
			List<SocketStruct> socketList = socketBufferMap.remove(mapId);
			socketList.clear();
		}
		
		return 0;
	}
	
	// tcp operation methods
	
	private static int buildAndSendTcpPackage(byte[] localAddr, int localport, 
			byte[] remoteAddr, int remoteport, SocketStruct socketStruct, boolean isFirstPkg, boolean isFinPkg, byte[] buff, int off, int len){
		
		if((isFirstPkg && isFinPkg) || (buff!=null && (isFirstPkg || isFinPkg))){
			return -1;
		}
		
		boolean push = false, ack = false;
		if(buff!=null){
			push = true;
			ack = true;
		}
		
		byte[] tcpbuff = new byte[2048];
		byte[] macAddr = getIpMacAddr(localAddr);
		
		byte[] targetMacAddr = findIpMacInMapArray(remoteAddr);
		if(targetMacAddr==null){
			targetMacAddr = broadCastMacAddr;
		}
		List<TcpPackage.TcpOption> optionList = new ArrayList<TcpPackage.TcpOption>();
		if(isFirstPkg){
			TcpPackage.TcpOption option = TcpPackage.newMaximumSegmentSize(1460);
			optionList.add(option);
			//option = TcpPackage.newWindowScale(1<<5);
			//optionList.add(option);
		}
		int tcpHdrLen = isFirstPkg?20+TcpPackage.calcTcpOptoinLen(optionList):20;
		int tcpDataLen = buff!=null?tcpHdrLen+len:tcpHdrLen;
		TcpPackage tcpPackage = TcpPackage.newTcpPackage(localport, remoteport, socketStruct.sequenceId, socketStruct.acknowledgementId,
				tcpHdrLen, false, false, false, false, ack, push, false, isFirstPkg, isFinPkg,
				0x00007fff, 0, 0, isFirstPkg?optionList:null, null);
		IpPackage ipPackage = IpPackage.newIpPackage(4, 20, 0, 0, 20+tcpDataLen /*tcp data len*/, socketStruct.identifyId, false, false, 0, 128, 6, 0, 
				localAddr, remoteAddr);
		EtherNetPacket replyEtherNetPkg = EtherNetPacket.newEtherNetPacket(macAddr, targetMacAddr, 2048);

		int etherLen = EtherNetPacket.packToEthernetBuff(replyEtherNetPkg, tcpbuff, 0, tcpbuff.length);
		int ipPackLen = IpPackage.packToIpBuff(ipPackage, tcpbuff, etherLen, tcpbuff.length-etherLen);
		int tcpPackLen = TcpPackage.packToTcpBuff(tcpPackage, tcpbuff, etherLen+ipPackLen, tcpbuff.length-etherLen-ipPackLen);

		int tcpCheckSum = TcpPackage.calcTcpChecksum(tcpPackage, ipPackage.sourceIP, ipPackage.destinationIp, tcpDataLen, buff, off, len);
		int ipCheckSum = IpPackage.calcIpHeaderChecksum(ipPackage);
		
		if(buff!=null){
			System.arraycopy(buff, off, tcpbuff, etherLen+ipPackLen+tcpPackLen, len);
		}
		
		tcpbuff[24] = (byte)((ipCheckSum>>8)&0xff);
		tcpbuff[25] = (byte)(ipCheckSum&0xff);
		tcpbuff[50] = (byte)((tcpCheckSum>>8)&0xff);
		tcpbuff[51] = (byte)(tcpCheckSum&0xff);
		
		if(netdriverSimulate!=null){
			netdriverSimulate.putDataToOutputBuffer(tcpbuff, 0, etherLen+ipPackLen+tcpDataLen);
		}
		
		return 0;
	}
	
	public static int createTcpSocket(InetSocketAddress bindAddr){
		String mapId = String.format("TCP %s %d", bindAddr.getHostString(), bindAddr.getPort());
		if(socketBufferMap.containsKey(mapId)){
			return -1;
		}

		List<SocketStruct> list = new ArrayList<SocketStruct>();
		//SocketStruct socketStruct = new SocketStruct();
		//socketStruct.identifyId = random.nextInt(1800)+6000;
		//socketStruct.inBuffer = new byte[SocketBufferSize];
		//socketStruct.outBuffer = new byte[SocketBufferSize];
		//socketStruct.sequenceId = random.nextInt(1000)+2000;
		//socketStruct.type = TcpProtol;
		//list.add(socketStruct);
		socketBufferMap.put(mapId, list);
			
		return 0;
	}
	
	public static int connectTcpSocket(InetSocketAddress fromAddr, InetSocketAddress toAddr){
		if(netdriverSimulate!=null){
			byte[] tcpbuff = new byte[2048];
			String mapId = String.format("TCP %s %d", fromAddr.getHostString(), fromAddr.getPort());
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}

			SocketStruct socketStruct =  findSocketStruct(fromAddr.getAddress().getAddress(), fromAddr.getPort(),
					toAddr.getAddress().getAddress(), toAddr.getPort(), TcpProtol);
			if(socketStruct==null){
				socketStruct = new SocketStruct();
				socketStruct.identifyId = random.nextInt(1800)+6000;
				socketStruct.initInputBuffer(SocketBufferSize);
				socketStruct.initOutputBuffer(SocketBufferSize);
				socketStruct.sequenceId = random.nextInt(1000)+2000;
				socketStruct.type = TcpProtol;
				socketStruct.remoteAddr = toAddr.getAddress().getAddress();
				socketStruct.remotePort = toAddr.getPort();
				socketBufferMap.get(mapId).add(socketStruct);
			}
			
			buildAndSendTcpPackage(fromAddr.getAddress().getAddress(), fromAddr.getPort(), 
					toAddr.getAddress().getAddress(), toAddr.getPort(), socketStruct, true, false, null, -1, -1);
			
			socketStruct.identifyId += 2;

			socketStruct.status = 1;
			
			return 0;
		}
		return -1;
	}
	
	public static void procOutputTcpData(){
		byte[] buff = new byte[1460];
		List<SocketStruct> removeList = new ArrayList<SocketStruct>();
		synchronized(socketBufferMap){
			for( String str : socketBufferMap.keySet() ){
				if(str.startsWith("TCP ")){
					String[] tempArray = str.split(" ");
					InetSocketAddress addr = new InetSocketAddress(tempArray[1], Integer.parseInt(tempArray[2]));
					for(SocketStruct s : socketBufferMap.get(str)){
						if(s.status==4){
							int size = s.readTcpOutputData(buff, 0, 1460);
							if(size>0){
								buildAndSendTcpPackage(addr.getAddress().getAddress(), addr.getPort(), 
										s.remoteAddr, s.remotePort, s, false, false, buff, 0, size);
									
								s.identifyId += 2;
							}
						}else if(s.status==5){
							removeList.add(s);
						}
					}
					for(SocketStruct s : removeList){
						socketBufferMap.get(str).remove(s);
					}
					removeList.clear();
				}
			}
		}
	}
	
	public static int sendTcpData(InetSocketAddress fromAddr, InetSocketAddress toAddr, byte[] buff, int off, int len){
		if(netdriverSimulate!=null){
			byte[] tcpbuff = new byte[2048];
			String mapId = String.format("TCP %s %d", fromAddr.getHostString(), fromAddr.getPort());
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}

			SocketStruct socketStruct =  findSocketStruct(fromAddr.getAddress().getAddress(), fromAddr.getPort(),
					toAddr.getAddress().getAddress(), toAddr.getPort(), TcpProtol);
			if(socketStruct==null || socketStruct.status==5){
				return -1;
			}
			if(socketStruct.status==4){
				return socketStruct.writeOutputData(buff, off, len);
			}
			
			return 0;
		}
		return -1;
	}
	
	public static int receivedTcpData(InetSocketAddress fromAddr, byte[] toAddr, byte[] buff, int off, int len){
		if(netdriverSimulate!=null){
			int size = 0;
			String mapId = String.format("TCP %s %d", fromAddr.getHostString(), fromAddr.getPort());
			if(!socketBufferMap.containsKey(mapId)){
				return -1;
			}

			List<SocketStruct> socketList = Collections.synchronizedList(socketBufferMap.get(mapId));

			for(SocketStruct s : socketList){
				if(s.status==4){
					synchronized(s){
						if(s.status==4){
							size = s.readInputData(buff, off, len);
							if(size>0){
								System.arraycopy(s.remoteAddr, 0, toAddr, 0, 4);
								toAddr[4] = (byte)((s.remotePort>>8)&0xff);
								toAddr[5] = (byte)(s.remotePort&0xff);
							}
						}
					}
					if(size>0){
						break;
					}
				}
			}
			return size;
		}
		return -1;
	}
	
	

	

	
	public static int closeTcpSocket(InetSocketAddress bindAddr){
		String mapId = String.format("TCP %s %d", bindAddr.getHostName(), bindAddr.getPort());
		if(!socketBufferMap.containsKey(mapId)){
			return -1;
		}
		
		List<SocketStruct> socketList = Collections.synchronizedList(socketBufferMap.get(mapId));
		byte[] tcpbuff = new byte[128];
		if(socketList!=null){
			for(SocketStruct s : socketList){
				buildAndSendTcpPackage(bindAddr.getAddress().getAddress(), bindAddr.getPort(), 
						s.remoteAddr, s.remotePort, s, false, true, null, -1, -1);
				
				s.identifyId += 2;
				s.status = 5;
			}
		}
		
		return 0;
	}
	
	
}
