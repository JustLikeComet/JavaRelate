
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

}
