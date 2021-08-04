
public class ArpPackage {
	public static final int ARP_REQUEST = 1;
	public static final int ARP_REPLY = 2;
	public int hwtype;
	public int protoType;
	public int hwSize;
	public int protoSize;
	public int opcode; 
	public byte[] senderMacAddr;
	public byte[] sendIp;
	public byte[] targetMacAddr;
	public byte[] targeIp;
	
	public String toString(){
		return "arp package "+String.format(" hwtype %d protoType %d hwSize %d protoSize %d opcode %d"
				+ " senderMacAddr %02X:%02X:%02X:%02X:%02X:%02X sendIp %d.%d.%d.%d targetMacAddr %02X:%02X:%02X:%02X:%02X:%02X targeIp %d.%d.%d.%d",
				hwtype,
				protoType,
				hwSize,
				protoSize,
				opcode,
				senderMacAddr[0],senderMacAddr[1],senderMacAddr[2],senderMacAddr[3],senderMacAddr[4],senderMacAddr[5],
				sendIp[0]&0xff,sendIp[1]&0xff,sendIp[2]&0xff,sendIp[3]&0xff,
				targetMacAddr[0],targetMacAddr[1],targetMacAddr[2],targetMacAddr[3],targetMacAddr[4],targetMacAddr[5],
				targeIp[0]&0xff,targeIp[1]&0xff,targeIp[2]&0xff,targeIp[3]&0xff);
	}
	
	public static ArpPackage newArpPackage(int opcode, 
			byte[] senderMacAddr, byte[] sendIp,
			byte[] targetMacAddr, byte[] targeIp){
		ArpPackage arpPackage = new ArpPackage();
		arpPackage.hwtype = 1;
		arpPackage.protoType = 0x0800;
		arpPackage.hwSize = 6;
		arpPackage.protoSize = 4;
		arpPackage.opcode = opcode;
		arpPackage.senderMacAddr = senderMacAddr;
		arpPackage.sendIp = sendIp;
		arpPackage.targetMacAddr = targetMacAddr;
		arpPackage.targeIp = targeIp;
		return arpPackage;
		
	}
	
	public static int packToArpBuff(int opcode, 
			byte[] senderMacAddr, byte[] sendIp,
			byte[] targetMacAddr, byte[] targeIp,
			byte[] arpBuff, int off, int len){
		if(len<46) return -1;
		//byte[] arpBuff = new byte[46];
		int pos = off;
		arpBuff[pos++] = 0; // hw type
		arpBuff[pos++] = 1;
		arpBuff[pos++] = 8; // protocol type
		arpBuff[pos++] = 0;
		arpBuff[pos++] = 6; // hardware size
		arpBuff[pos++] = 4; // protocol size
		arpBuff[pos++] = (byte)((opcode>>8)&0xff); // opcode
		arpBuff[pos++] = (byte)(opcode&0xff);
		System.arraycopy(senderMacAddr, 0, arpBuff, pos, 6);
		pos+=6;
		System.arraycopy(sendIp, 0, arpBuff, pos, 4);
		pos+=4;
		System.arraycopy(targetMacAddr, 0, arpBuff, 6, 6);
		pos+=6;
		System.arraycopy(targeIp, 0, arpBuff, pos, 4);
		pos+=4;
		
		for(;pos<46+off;){
			arpBuff[pos++] = 0;
		}
		
		//return arpBuff;
		return pos-len;
	}
	
	public static int packToArpBuff(ArpPackage pkg,
			byte[] arpBuff, int off, int len){
		if(len<28) return -1;
		//byte[] arpBuff = new byte[46];
		int pos = off;
		arpBuff[pos++] = 0; // hw type
		arpBuff[pos++] = 1;
		arpBuff[pos++] = 8; // protocol type
		arpBuff[pos++] = 0;
		arpBuff[pos++] = 6; // hardware size
		arpBuff[pos++] = 4; // protocol size
		arpBuff[pos++] = (byte)((pkg.opcode>>8)&0xff); // opcode
		arpBuff[pos++] = (byte)(pkg.opcode&0xff);
		System.arraycopy(pkg.senderMacAddr, 0, arpBuff, pos, 6);
		pos+=6;
		System.arraycopy(pkg.sendIp, 0, arpBuff, pos, 4);
		pos+=4;
		System.arraycopy(pkg.targetMacAddr, 0, arpBuff, pos, 6);
		pos+=6;
		System.arraycopy(pkg.targeIp, 0, arpBuff, pos, 4);
		pos+=4;
		
		for(;pos<28+off;){
			arpBuff[pos++] = 0;
		}
		
		//return arpBuff;
		return pos-off;
	}
	
	public static int extraArpPack(ArpPackage pkg, byte[] arpBuff, int off, int len){
		int pos = off;
		pkg.hwtype = ((arpBuff[pos]&0xff)<<8)+arpBuff[pos+1]&0xff;
		pos += 2;
		pkg.protoType = ((arpBuff[pos]&0xff)<<8)+arpBuff[pos+1]&0xff;
		pos += 2;
		pkg.hwSize = arpBuff[pos]&0xff;
		++pos;
		pkg.protoSize = arpBuff[pos]&0xff;
		++pos;
		pkg.opcode = ((arpBuff[pos]&0xff)<<8)+arpBuff[pos+1]&0xff;
		pos += 2;
		if(pkg.senderMacAddr==null){
			pkg.senderMacAddr = new byte[6];
		}
		System.arraycopy(arpBuff, pos, pkg.senderMacAddr, 0, 6);
		pos += 6;
		if(pkg.sendIp==null){
			pkg.sendIp = new byte[4];
		}
		System.arraycopy(arpBuff, pos, pkg.sendIp, 0, 4);
		pos += 4;
		if(pkg.targetMacAddr==null){
			pkg.targetMacAddr = new byte[6];
		}
		System.arraycopy(arpBuff, pos, pkg.targetMacAddr, 0, 6);
		pos += 6;
		if(pkg.targeIp==null){
			pkg.targeIp = new byte[4];
		}
		System.arraycopy(arpBuff, pos, pkg.targeIp, 0, 4);
		pos += 4;
		return pos-off;
	}

	public static void main(String[] args) {
		byte[] localMacAddr = {(byte)0xfe, (byte)0x00, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08};
		byte[] arpPkt = {   (byte)0xfe, (byte)0x00, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0xfe, (byte)0x00,
				(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x01,
				(byte)0x08, (byte)0x00, (byte)0x06, (byte)0x04, (byte)0x00, (byte)0x01, (byte)0xfe, (byte)0x00, 
				(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0xc0, (byte)0xa8, (byte)0x0b, (byte)0x7b,
				(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xc0, (byte)0xa8,
				(byte)0x0b, (byte)0x0c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
				(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 
				};

		EtherNetPacket replyEtherNetPkg = new EtherNetPacket();
		int etherlen = EtherNetPacket.extraEthernetPack(replyEtherNetPkg, arpPkt, 0, arpPkt.length);
		if(replyEtherNetPkg.protType==0x0806 && ByteUtils.equals(replyEtherNetPkg.toMac, localMacAddr)){
			ArpPackage replyArpPackage = new ArpPackage();
			int arpPkgLen = ArpPackage.extraArpPack(replyArpPackage, arpPkt, etherlen, arpPkt.length-etherlen);
			String macAddrStr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", replyArpPackage.targetMacAddr[0], replyArpPackage.targetMacAddr[1],
					replyArpPackage.targetMacAddr[2], replyArpPackage.targetMacAddr[3], replyArpPackage.targetMacAddr[4], replyArpPackage.targetMacAddr[5]);
			System.out.println("arp return macAddrStr " + macAddrStr);
		}
		
	}

}
