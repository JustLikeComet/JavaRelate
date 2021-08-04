
public class IpPackage {
	public int ipVersion;
	public int ipHeadLen;
	public int dscCodepoint; // differentiated services field codepoint
	public int dscEcn; // differentiated services field explictie congestion notification
	public int ipPkgTotalLen;
	public int identification;
	public boolean dontFragment;
	public boolean moreFraments;
	public int fragmentOffset;
	public int timeToLive;
	public int protocolType;
	public int headerChecksum;
	public byte[] sourceIP;
	public byte[] destinationIp;
	//public byte[] extraData;
	
	//public static IpPackage newIpPackage(int ipVersion, int ipHeadLen, int dscCodepoint, int dscEcn, int ipPkgTotalLen, 
	//		int identification, boolean dontFragment, boolean moreFraments, int fragmentOffset, int timeToLive,
	//		int protocolType, int headerChecksum, byte[] sourceIP, byte[] destinationIp, byte[] extraData){
	public static IpPackage newIpPackage(int ipVersion, int ipHeadLen, int dscCodepoint, int dscEcn, int ipPkgTotalLen, 
	int identification, boolean dontFragment, boolean moreFraments, int fragmentOffset, int timeToLive,
	int protocolType, int headerChecksum, byte[] sourceIP, byte[] destinationIp){
		IpPackage ipPackage = new IpPackage();
		ipPackage.ipVersion = ipVersion;
		ipPackage.ipHeadLen = ipHeadLen;
		ipPackage.dscCodepoint = dscCodepoint;
		ipPackage.dscEcn = dscEcn;
		ipPackage.ipPkgTotalLen = ipPkgTotalLen;
		ipPackage.identification = identification;
		ipPackage.dontFragment = dontFragment;
		ipPackage.moreFraments = moreFraments;
		ipPackage.fragmentOffset = fragmentOffset;
		ipPackage.timeToLive = timeToLive;
		ipPackage.protocolType = protocolType;
		ipPackage.headerChecksum = headerChecksum;
		ipPackage.sourceIP = sourceIP;
		ipPackage.destinationIp = destinationIp;
		//ipPackage.extraData = extraData;
		return ipPackage;
	}
	
	public String toString(){
		return "ip package "+String.format(" ipVersion %d ipHeadLen %d dscCodepoint %d dscEcn %d ipPkgTotalLen %d"
				+ " identification %d dontFragment %b moreFraments %b fragmentOffset %d timeToLive %d protocolType %d headerChecksum %04X"
				+ " sourceIP %d.%d.%d.%d destinationIp %d.%d.%d.%d ",
			ipVersion,
			ipHeadLen,
			dscCodepoint,
			dscEcn,
			ipPkgTotalLen,
			identification,
			dontFragment,
			moreFraments,
			fragmentOffset,
			timeToLive,
			protocolType,
			headerChecksum,
			sourceIP[0]&0xff,sourceIP[1]&0xff,sourceIP[2]&0xff,sourceIP[3]&0xff,
			destinationIp[0]&0xff,destinationIp[1]&0xff,destinationIp[2]&0xff,destinationIp[3]&0xff);
	}

	
	//public static int packToIpBuff(int ipVersion, int ipHeadLen,
	//		int dscCodepoint,int dscEcn,int ipPkgTotalLen, int identification,
	//		boolean dontFragment, boolean moreFraments, int fragmentOffset,
	//		int timeToLive,	int protocolType, int headerChecksum, 
	//		byte[] sourceIP, byte[] destinationIp,byte[] extraData, byte[] ipBuff, int off, int len){
	public static int packToIpBuff(int ipVersion, int ipHeadLen,
			int dscCodepoint,int dscEcn,int ipPkgTotalLen, int identification,
			boolean dontFragment, boolean moreFraments, int fragmentOffset,
			int timeToLive,	int protocolType, int headerChecksum, 
			byte[] sourceIP, byte[] destinationIp, byte[] ipBuff, int off, int len){
		
		//byte[] ipBuff = new byte[ipHeadLen+extraData.length];
		ipHeadLen/=4;
		int pos = off;
		ipBuff[pos++] = (byte)(((ipVersion<<4)&0xf0) + ipHeadLen );
		ipBuff[pos++] = (byte)(((dscCodepoint<<2)&0xfc) + dscEcn );
		ipBuff[pos++] = (byte)((ipPkgTotalLen>>8)&0xff);
		ipBuff[pos++] = (byte)(ipPkgTotalLen&0xff);
		ipBuff[pos++] = (byte)((identification>>8)&0xff);
		ipBuff[pos++] = (byte)(identification&0xff);
		ipBuff[pos++] = (byte)((dontFragment?(0x40):0)+(moreFraments?0x20:0)+(fragmentOffset>>8));
		ipBuff[pos++] = (byte)(fragmentOffset&0xff);
		ipBuff[pos++] = (byte)(timeToLive&0xff);
		ipBuff[pos++] = (byte)(protocolType&0xff);
		ipBuff[pos++] = (byte)((headerChecksum>>8)&0xff);
		ipBuff[pos++] = (byte)(headerChecksum&0xff);
		System.arraycopy(sourceIP, 0, ipBuff, pos, 4);
		pos+=4;
		System.arraycopy(destinationIp, 0, ipBuff, pos, 4);
		pos+=4;
		
		//if(extraData!=null){
		//	if(pos+extraData.length<=len){
		//		if(extraData!=null){
		//			System.arraycopy(extraData, 0, ipBuff, pos, extraData.length);
		//			pos+=extraData.length;
		//		}
		//	}
		//}
		
		
		//return ipBuff;
		return pos-off;
	}

	
	public static int packToIpBuff(IpPackage pkg, byte[] ipBuff, int off, int len){
		
		//byte[] ipBuff = new byte[ipHeadLen+extraData.length];
		int ipHeadLen= pkg.ipHeadLen/4;
		int pos = off;
		ipBuff[pos++] = (byte)(((pkg.ipVersion<<4)&0xf0) + ipHeadLen );
		ipBuff[pos++] = (byte)(((pkg.dscCodepoint<<2)&0xfc) + pkg.dscEcn );
		ipBuff[pos++] = (byte)((pkg.ipPkgTotalLen>>8)&0xff);
		ipBuff[pos++] = (byte)(pkg.ipPkgTotalLen&0xff);
		ipBuff[pos++] = (byte)((pkg.identification>>8)&0xff);
		ipBuff[pos++] = (byte)(pkg.identification&0xff);
		ipBuff[pos++] = (byte)((pkg.dontFragment?(0x40):0)+(pkg.moreFraments?0x20:0)+(pkg.fragmentOffset>>8));
		ipBuff[pos++] = (byte)(pkg.fragmentOffset&0xff);
		ipBuff[pos++] = (byte)(pkg.timeToLive&0xff);
		ipBuff[pos++] = (byte)(pkg.protocolType&0xff);
		ipBuff[pos++] = (byte)((pkg.headerChecksum>>8)&0xff);
		ipBuff[pos++] = (byte)(pkg.headerChecksum&0xff);
		System.arraycopy(pkg.sourceIP, 0, ipBuff, pos, 4);
		pos+=4;
		System.arraycopy(pkg.destinationIp, 0, ipBuff, pos, 4);
		pos+=4;
		//if(pkg.extraData!=null){
		//	if(pos+pkg.extraData.length<=len){
		//		if(pkg.extraData!=null){
		//			System.arraycopy(pkg.extraData, 0, ipBuff, pos, pkg.extraData.length);
		//			pos+=pkg.extraData.length;
		//		}
		//	}
		//}
		
		
		//return ipBuff;
		return pos-off;
	}
	
	public static int extraIpPack(IpPackage pkg, byte[] ipBuff, int off, int len){
		int pos = off;
		pkg.ipVersion = (ipBuff[pos]&0xff)>>4;
		pkg.ipHeadLen = (ipBuff[pos]&0x0f)<<2;
		pos++;
		pkg.dscCodepoint = ((ipBuff[pos]&0xff)>>2)&0x3f;
		pkg.dscEcn = ipBuff[pos]&0x3;
		pos++;
		pkg.ipPkgTotalLen = ((ipBuff[pos]&0xff)<<8) + (ipBuff[pos+1]&0xff);
		pos+=2;
		pkg.identification = ((ipBuff[pos]&0xff)<<8) + (ipBuff[pos+1]&0xff);
		pos+=2;
		pkg.dontFragment = (ipBuff[pos]&0x40)>0;
		pkg.moreFraments = (ipBuff[pos]&0x20)>0;
		pkg.fragmentOffset = ((ipBuff[pos]&0x1f)<<8)+(ipBuff[pos+1]&0xff);
		pos+=2;
		pkg.timeToLive = ipBuff[pos]&0xff;
		pos++;
		pkg.protocolType = ipBuff[pos]&0xff;
		pos++;
		pkg.headerChecksum = ((ipBuff[pos]&0xff)<<8) + (ipBuff[pos+1]&0xff);
		pos+=2;
		if(pkg.sourceIP==null){
			pkg.sourceIP = new byte[4];
		}
		System.arraycopy(ipBuff, pos, pkg.sourceIP, 0, 4);
		pos += 4;
		if(pkg.destinationIp==null){
			pkg.destinationIp = new byte[4];
		}
		System.arraycopy(ipBuff, pos, pkg.destinationIp, 0, 4);
		pos += 4;
		//pkg.extraData = new byte[pkg.ipPkgTotalLen-pkg.ipHeadLen];
		//System.arraycopy(ipBuff, pos, pkg.extraData, 0, pkg.extraData.length);
		//pos += pkg.extraData.length;
		return pos-off;
	}
	
	public static int calcIpHeaderChecksum(IpPackage pkg){
		byte[] tempBuff = new byte[20];
		packToIpBuff(pkg, tempBuff, 0, 20);
		return ByteUtils.calcBuffChecksum(tempBuff, 0, 20);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
