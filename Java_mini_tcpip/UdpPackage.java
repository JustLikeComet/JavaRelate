
public class UdpPackage {
	public int sourcePort ;
	public int destinationPort;
	public int length;
	public int checksum;
	public byte[] extraData;
	
	public String toString(){
		return "udp package "+String.format(" sourcePort %d destinationPort %d length %d checksum %04X",
				sourcePort,destinationPort, length, checksum&0xffff);
	}
	
	public static UdpPackage newUdpPackage(int sourcePort ,
			int destinationPort,
			int length,
			int checksum,byte[] extraData){
		UdpPackage pkg = new UdpPackage();
		pkg.sourcePort = sourcePort;
		pkg.destinationPort = destinationPort;
		pkg.length = length;
		pkg.checksum = checksum;
		pkg.extraData = extraData;
		return pkg;
	}

	public static int packToUdpBuff(int sourcePort ,
			int destinationPort,
			int length,
			int checksum,byte[] extraData, byte[] udpBuff, int off, int len){
		
		int pos = off;
		udpBuff[pos++] = (byte)((sourcePort>>8)&0xff);
		udpBuff[pos++] = (byte)(sourcePort&0xff);
		udpBuff[pos++] = (byte)((destinationPort>>8)&0xff);
		udpBuff[pos++] = (byte)(destinationPort&0xff);
		udpBuff[pos++] = (byte)((length>>8)&0xff);
		udpBuff[pos++] = (byte)(length&0xff);
		udpBuff[pos++] = (byte)((checksum>>8)&0xff);
		udpBuff[pos++] = (byte)(checksum&0xff);
		System.arraycopy(extraData, 0, udpBuff, pos, 4);
		pos+=extraData.length;
		
		
		//return ipBuff;
		return pos-off;
	}

	public static int packToUdpBuff(UdpPackage pkg, byte[] udpBuff, int off, int len){
		
		int pos = off;
		udpBuff[pos++] = (byte)((pkg.sourcePort>>8)&0xff);
		udpBuff[pos++] = (byte)(pkg.sourcePort&0xff);
		udpBuff[pos++] = (byte)((pkg.destinationPort>>8)&0xff);
		udpBuff[pos++] = (byte)(pkg.destinationPort&0xff);
		udpBuff[pos++] = (byte)((pkg.length>>8)&0xff);
		udpBuff[pos++] = (byte)(pkg.length&0xff);
		udpBuff[pos++] = (byte)((pkg.checksum>>8)&0xff);
		udpBuff[pos++] = (byte)(pkg.checksum&0xff);
		if(pkg.extraData!=null){
			System.arraycopy(pkg.extraData, 0, udpBuff, pos, 4);
			pos+=pkg.extraData.length;
		}
		
		
		//return ipBuff;
		return pos-off;
	}
	
	public static int extraUdpPack(UdpPackage pkg, boolean extraPayload, byte[] udpBuff, int off, int len){
		int pos = off;
		pkg.sourcePort = ((udpBuff[pos]&0xff)<<8)+(udpBuff[pos+1]&0xff);
		pos += 2;
		pkg.destinationPort = ((udpBuff[pos]&0xff)<<8)+(udpBuff[pos+1]&0xff);
		pos += 2;
		pkg.length = ((udpBuff[pos]&0xff)<<8)+(udpBuff[pos+1]&0xff);
		pos += 2;
		pkg.checksum = ((udpBuff[pos]&0xff)<<8)+(udpBuff[pos+1]&0xff);
		pos += 2;
		if(extraPayload){
			if(pkg.length-pos+off>0){
				pkg.extraData = new byte[pkg.length-pos+off];
				System.arraycopy(udpBuff,pos, pkg.extraData, 0, pkg.extraData.length);
				pos+=pkg.extraData.length;
			}
		}
		return pos-off;
	}
	
	public static int calcUdpCheckSum(UdpPackage pkg, byte[] srcAddr, byte[] destAddr, int udpLen, byte[] payload, int off, int len){
		int checkSum = 0;
		checkSum += ((srcAddr[0]&0xff)<<8)+(srcAddr[1]&0xff) + ((srcAddr[2]&0xff)<<8)+(srcAddr[3]&0xff);
		checkSum+=((destAddr[0]&0xff)<<8)+(destAddr[1]&0xff) + ((destAddr[2]&0xff)<<8)+(destAddr[3]&0xff);
		checkSum += 17;
		checkSum += udpLen;
		checkSum += pkg.sourcePort;
		checkSum += pkg.destinationPort;
		checkSum += pkg.length;
		if(payload!=null){
			for(int i=0; i<len;++i){
				checkSum += (i%2==0)?((payload[i+off]&0xff)<<8):(payload[i+off]&0xff);
			}
		}
		return  ~(((checkSum>>16)&0xffff)+(checkSum&0xffff));
	}
	
	public static void main(String[] args) {
		{
			byte[] client2server_01 = {
					(byte)0x04, (byte)0xfe, (byte)0x8d, (byte)0xbe, (byte)0xd6, (byte)0x2f, (byte)0x6c, (byte)0x2b, (byte)0x59, (byte)0x56, (byte)0x6a, (byte)0xfe, (byte)0x08, (byte)0x00, (byte)0x45, (byte)0x00,
					(byte)0x00, (byte)0x20, (byte)0x20, (byte)0x8c, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x11, (byte)0x81, (byte)0xf0, (byte)0xc0, (byte)0xa8, (byte)0x0b, (byte)0xf4, (byte)0xc0, (byte)0xa8,
					(byte)0x0b, (byte)0x0c, (byte)0xf9, (byte)0x1d, (byte)0x30, (byte)0x39, (byte)0x00, (byte)0x0c, (byte)0xd9, (byte)0xf1, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x0a, (byte)0x00, (byte)0x00,
					(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
					};
	
	
			EtherNetPacket replyEtherNetPkg = new EtherNetPacket();
			int etherLen = EtherNetPacket.extraEthernetPack(replyEtherNetPkg, client2server_01, 0, client2server_01.length);
			System.out.println(replyEtherNetPkg.toString());
			if(replyEtherNetPkg.protType==0x0800){
				IpPackage ipPackage =new IpPackage();
				int ipPkgLen = IpPackage.extraIpPack(ipPackage, client2server_01, etherLen, client2server_01.length-etherLen);
				System.out.println( ipPackage );
				
				UdpPackage udpPackage = new UdpPackage();
				UdpPackage.extraUdpPack(udpPackage, true, client2server_01, etherLen+ipPkgLen, client2server_01.length-etherLen-ipPkgLen);
				System.out.println(udpPackage);
				
				//System.out.println(String.format("ip checksum %04X", IpPackage.calcIpHeaderChecksum(ipPackage)));
				System.out.println(ipPackage.ipPkgTotalLen+"  "+ipPackage.ipHeadLen);
				System.out.println(String.format("udp checksum %04X", calcUdpCheckSum(udpPackage, ipPackage.sourceIP, ipPackage.destinationIp,
						ipPackage.ipPkgTotalLen-ipPackage.ipHeadLen, udpPackage.extraData, 0, udpPackage.extraData.length)&0xffff));
				
			}
		}
		{
			byte[] tempbuff = new byte[64];
			byte[] localMacAddr = {(byte)0x6C,(byte)0x2B,(byte)0x59,(byte)0x56,(byte)0x6A,(byte)0xFE};
			byte[] targetMacAddr = {(byte)0x04,(byte)0xfe,(byte)0x8d,(byte)0xbe,(byte)0xd6,(byte)0x2f};
			byte[] localIp = {(byte)192, (byte)168, (byte)11, (byte)244};
			byte[] targetIp = {(byte)192, (byte)168, (byte)11, (byte)12};
			byte[] text = {(byte)0x31, (byte)0x32, (byte)0x33, (byte)0x0a};
			UdpPackage udpPackage = UdpPackage.newUdpPackage(63773, 12345, 12, 0, null);
			IpPackage ipPackage = IpPackage.newIpPackage(4, 20, 0, 0, 32, 8332, false, false, 0, 128, 17, 0, localIp, targetIp);
			EtherNetPacket replyEtherNetPkg = EtherNetPacket.newEtherNetPacket(localMacAddr, targetMacAddr, 2048);
			int etherLen = EtherNetPacket.packToEthernetBuff(replyEtherNetPkg, tempbuff, 0, 64);
			int ipPackLen = IpPackage.packToIpBuff(ipPackage, tempbuff, etherLen, 64-etherLen);
			int udpPackLen = UdpPackage.packToUdpBuff(udpPackage, tempbuff, etherLen+ipPackLen, 64-etherLen-ipPackLen);
			for(int i=0; i<etherLen+ipPackLen+udpPackLen; ++i){
				System.out.print(String.format(" %02X", tempbuff[i]));
				if(((i+1)%16)==0)System.out.println();
			}
			System.out.println();
			udpPackage.extraData = text;
			System.out.println(String.format("%04X", 
					UdpPackage.calcUdpCheckSum(udpPackage, ipPackage.sourceIP, ipPackage.destinationIp, udpPackLen+text.length,
							udpPackage.extraData, 0, udpPackage.extraData.length)&0xffff));
			System.out.println(String.format("before add checksum %04X", IpPackage.calcIpHeaderChecksum(ipPackage)&0xffff));
			ipPackage.headerChecksum = IpPackage.calcIpHeaderChecksum(ipPackage);
			System.out.println(String.format("after add checksum %04X", IpPackage.calcIpHeaderChecksum(ipPackage)&0xffff));
		}
		
	}

}
