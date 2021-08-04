
public class EtherNetPacket {
	public byte[] fromMac;
	public byte[] toMac;
	public int protType;
	//public byte[] extraData;
	
	public String toString(){
		String fmt = "%s fromMac %02X:%02X:%02X:%02X:%02X:%02X toMac %02X:%02X:%02X:%02X:%02X:%02X protType %d ";
		return String.format(fmt,"ether package ",
				fromMac[0],fromMac[1],fromMac[2],fromMac[3],fromMac[4],fromMac[5],
				toMac[0],toMac[1],toMac[2],toMac[3],toMac[4],toMac[5],
				protType);
	}
	
	//public static EtherNetPacket newEtherNetPacket(byte[] fromMac, byte[] toMac, int protType, byte[] extraData){
	public static EtherNetPacket newEtherNetPacket(byte[] fromMac, byte[] toMac, int protType){
		EtherNetPacket etherNetPacket = new EtherNetPacket();
		etherNetPacket.fromMac = fromMac;
		etherNetPacket.toMac = toMac;
		etherNetPacket.protType = protType;
		//etherNetPacket.extraData = extraData;
		return etherNetPacket;
	}
	
	//public static int packToEthernetBuff(byte[] fromMac, byte[] toMac, int protType, byte[] extraData, byte[] etherPackageBuff, int off, int len){
	public static int packToEthernetBuff(byte[] fromMac, byte[] toMac, int protType, byte[] etherPackageBuff, int off, int len){
		int pos = off;
		System.arraycopy(toMac, 0, etherPackageBuff, pos, 6);
		pos += 6;
		System.arraycopy(fromMac, 0, etherPackageBuff, pos, 6);
		pos += 6;
		etherPackageBuff[12] = (byte)((protType>>8)&0xff);
		etherPackageBuff[13] = (byte)(protType&0xff);
		pos += 2;
		//if(extraData!=null){
		//	System.arraycopy(extraData, 0, etherPackageBuff, 14, extraData.length);
		//	pos += extraData.length;
		//}
		return pos-off;
	}
	
	public static int packToEthernetBuff(EtherNetPacket pkg, byte[] etherPackageBuff, int off, int len){
		int pos = off;
		System.arraycopy(pkg.toMac, 0, etherPackageBuff, pos, 6);
		pos += 6;
		System.arraycopy(pkg.fromMac, 0, etherPackageBuff, pos, 6);
		pos += 6;
		etherPackageBuff[12] = (byte)((pkg.protType>>8)&0xff);
		etherPackageBuff[13] = (byte)(pkg.protType&0xff);
		pos += 2;
		
		//if(pkg.extraData!=null){
		//	System.arraycopy(pkg.extraData, 0, etherPackageBuff, 14, pkg.extraData.length);
		//	pos += pkg.extraData.length;
		//}
		return pos-off;
	}
	
	public static int extraEthernetPack(EtherNetPacket pkg, byte[] etherBuff, int off, int len){
		if(pkg.toMac==null){
			pkg.toMac = new byte[6];
		}
		if(pkg.fromMac==null){
			pkg.fromMac = new byte[6];
		}
		System.arraycopy(etherBuff, off, pkg.toMac, 0, 6);
		System.arraycopy(etherBuff, off+6, pkg.fromMac, 0, 6);
		pkg.protType = ((etherBuff[off+12]&0xff)<<8) + (etherBuff[off+13]&0xff);
		//pkg.extraData = new byte[len-14];
		//System.arraycopy(etherBuff, off+14, pkg.extraData, 0, len-14);
		return 14;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
