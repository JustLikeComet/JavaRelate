import java.util.ArrayList;
import java.util.List;


public class TcpPackage {
	
	public static final int SYN_MARK = 0x002;
	public static final int SYN_ACK_MARK = 0x002+0x010;
	public static final int ACK_MARK = 0x010;
	public static final int PUSH_ACK_MARK = 0x008+0x010;
	public static final int FIN_ACK_MARK = 0x001+0x010;
	
	public static final int EndOfOptionList = 0;
	public static final int NoOperation = 1;
	public static final int MaximumSegmentSize = 2;
	public static final int WidnowScale = 3;
	public static final int SackPermitted = 4;
	
	static class MaximumSegment{
		public int type;
		public int len;
		public int msValue;
	}
	static class TcpOption{
		public int kind;
		public int val;
	}
	public int sourcePort;
	public int destinationPort;
	public long sequenceNumber;
	public long acknowledgementNumber;
	public int headerlen;
	public boolean nonce;
	public boolean cwr;
	public boolean ecn;
	public boolean urg;
	public boolean ack;
	public boolean push;
	public boolean reset;
	public boolean syn;
	public boolean fin;
	public int windowSize;
	public int checkSum;
	public int urgentPointer;
	public List<TcpOption> optionList=new ArrayList<TcpOption>();
	public byte[] extraData;
	
	public String toString(){
		StringBuilder strb = new StringBuilder();
		for(int i=0; i<optionList.size(); ++i){
			TcpOption option = optionList.get(i);
			switch(option.kind){
			case MaximumSegmentSize:
				strb.append(String.format(" MaximumSegmentSize : %d", option.val));
				break;
			case WidnowScale:
				strb.append(String.format(" WidnowScale : %d", option.val));
				break;
			case SackPermitted:
				strb.append(String.format(" SackPermitted : true", option.val));
				break;
			}
		}
		return "tcp package "+String.format(" sourcePort %d destinationPort %d sequenceNumber %d acknowledgementNumber %d "
				+ "headerlen %d nonce %b cwr %b ecn %b urg %b ack %b push %b reset %b syn %b fin %b windowSize %d checkSum %04X urgentPointer %d options %s" ,
				sourcePort,destinationPort, sequenceNumber, acknowledgementNumber, headerlen,
				nonce,cwr, ecn, urg, ack, push, reset, syn, fin, windowSize, checkSum, urgentPointer, strb);
	}
	
	public static TcpPackage newTcpPackage(int sourcePort, int destinationPort, long sequenceNumber, long acknowledgementNumber,
			int headerlen, boolean nonce, boolean cwr, boolean ecn, boolean urg, boolean ack, boolean push, boolean reset, boolean syn, boolean fin,
			int windowSize, int checkSum, int urgentPointer, List<TcpOption> optionList, byte[] extraData){
		TcpPackage pkg = new TcpPackage();
		pkg.sourcePort = sourcePort;
		pkg.destinationPort = destinationPort;
		pkg.sequenceNumber = sequenceNumber;
		pkg.acknowledgementNumber = acknowledgementNumber;
		pkg.headerlen = headerlen;
		pkg.nonce = nonce;
		pkg.cwr = cwr;
		pkg.ecn = ecn;
		pkg.urg = urg;
		pkg.ack = ack;
		pkg.push = push;
		pkg.reset = reset;
		pkg.syn = syn;
		pkg.fin = fin;
		pkg.windowSize = windowSize;
		pkg.checkSum = checkSum;
		pkg.urgentPointer = urgentPointer;
		pkg.optionList = optionList;
		pkg.extraData = extraData;
		return pkg;
	}
	
	
	public static int calcTcpHeaderlen(TcpPackage tcpPkg){
		int pkgLen = 20+calcTcpOptoinLen(tcpPkg.optionList);
		return pkgLen;
	}



	public static int calcTcpOptoinLen(List<TcpOption> optionList) {
		int pkgLen = 0;
		for(int i=0; i<optionList.size(); i++){
			TcpOption option = optionList.get(i);
			switch(option.kind){
			case MaximumSegmentSize:
				pkgLen += 4;
				break;
			case WidnowScale:
				pkgLen += 3;
				break;
			case SackPermitted:
				pkgLen += 2;
				break;
			}
			if(i<optionList.size()-1){
				++pkgLen;
			}
		}
		if((pkgLen&0x3)>0){
			pkgLen += 4-(pkgLen&0x3);
		}
		return pkgLen;
	}



	public static int packToTcpBuff(TcpPackage tcpPkg, byte[] tcpBuff, int off, int len){
		
		int pos = off, hdrPos = 0;
		tcpBuff[pos++] = (byte)((tcpPkg.sourcePort>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.sourcePort&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.destinationPort>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.destinationPort&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.sequenceNumber>>24)&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.sequenceNumber>>16)&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.sequenceNumber>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.sequenceNumber&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.acknowledgementNumber>>24)&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.acknowledgementNumber>>16)&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.acknowledgementNumber>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.acknowledgementNumber&0xff);
		hdrPos = pos;
		tcpBuff[pos++] = (byte)((tcpPkg.headerlen<<2)&0xf0+(tcpPkg.nonce?1:0));
		tcpBuff[pos++] = (byte)((tcpPkg.cwr?0x80:0)+(tcpPkg.ecn?0x40:0)+(tcpPkg.urg?0x20:0)+(tcpPkg.ack?0x10:0)+(tcpPkg.push?0x08:0)+(tcpPkg.reset?0x04:0)+(tcpPkg.syn?0x02:0)+(tcpPkg.fin?0x01:0));
		tcpBuff[pos++] = (byte)((tcpPkg.windowSize>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.windowSize&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.checkSum>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.checkSum&0xff);
		tcpBuff[pos++] = (byte)((tcpPkg.urgentPointer>>8)&0xff);
		tcpBuff[pos++] = (byte)(tcpPkg.urgentPointer&0xff);
		if(tcpPkg.optionList!=null){
			for(int i=0; i<tcpPkg.optionList.size(); i++){
				TcpOption option = tcpPkg.optionList.get(i);
				switch(option.kind){
				case MaximumSegmentSize:
					tcpBuff[pos++] = (byte)option.kind;
					tcpBuff[pos++] = 4;
					tcpBuff[pos++] = (byte)((option.val>>8)&0xff);
					tcpBuff[pos++] = (byte)(option.val&0xff);
					break;
				case WidnowScale:
					tcpBuff[pos++] = (byte)option.kind;
					tcpBuff[pos++] = 3;
					tcpBuff[pos++] = (byte)((option.val>>5)&0xff);
					break;
				case SackPermitted:
					tcpBuff[pos++] = (byte)option.kind;
					tcpBuff[pos++] = 2;
					break;
				}
				if(i<tcpPkg.optionList.size()-1){
					tcpBuff[pos++] = NoOperation;
				}
			}
			if(((pos-off)&0x3)>0){
				int tmpLen = 4-((pos-off)&0x03);
				for(int i=0; i<tmpLen; ++i){
					tcpBuff[pos++] = NoOperation;
				}
			}
		}
		//tcpBuff[pos++] = EndOfOptionList;
		tcpPkg.headerlen = pos-off;
		tcpBuff[hdrPos] = (byte)((tcpPkg.headerlen<<2)&0xf0+(tcpPkg.nonce?1:0));
		if(tcpPkg.extraData!=null){
			System.arraycopy(tcpPkg.extraData, 0, tcpBuff, pos, 4);
			pos+=tcpPkg.extraData.length;
		}
		
		
		//return ipBuff;
		return pos-off;
	}
	
	public static int extraTcpPack(TcpPackage pkg, boolean extraPayload, byte[] tcpBuff, int off, int len){
		if(pkg.optionList==null){
			pkg.optionList = new ArrayList<TcpOption>();
		}
		if(pkg.optionList.size()>0){
			pkg.optionList.clear();
		}
		int pos = off;
		pkg.sourcePort = ((tcpBuff[pos]&0xff)<<8)+(tcpBuff[pos+1]&0xff);
		pos += 2;
		pkg.destinationPort = ((tcpBuff[pos]&0xff)<<8)+(tcpBuff[pos+1]&0xff);
		pos += 2;
		pkg.sequenceNumber = (((long)tcpBuff[pos]&0xff)<<24)+((tcpBuff[pos+1]&0xff)<<16)+((tcpBuff[pos+2]&0xff)<<8)+(tcpBuff[pos+3]&0xff);
		pos += 4;
		pkg.acknowledgementNumber = (((long)tcpBuff[pos]&0xff)<<24)+((tcpBuff[pos+1]&0xff)<<16)+((tcpBuff[pos+2]&0xff)<<8)+(tcpBuff[pos+3]&0xff);
		pos += 4;
		pkg.headerlen = (tcpBuff[pos]&0xff)>>2;
		pkg.nonce = (tcpBuff[pos]&0x01)>0;
		++pos;
		pkg.cwr = (tcpBuff[pos]&0x80)>0;
		pkg.ecn = (tcpBuff[pos]&0x40)>0;
		pkg.urg = (tcpBuff[pos]&0x20)>0;
		pkg.ack = (tcpBuff[pos]&0x10)>0;
		pkg.push = (tcpBuff[pos]&0x08)>0;
		pkg.reset = (tcpBuff[pos]&0x04)>0;
		pkg.syn = (tcpBuff[pos]&0x02)>0;
		pkg.fin = (tcpBuff[pos]&0x01)>0;
		++pos;
		pkg.windowSize = ((tcpBuff[pos]&0xff)<<8)+(tcpBuff[pos+1]&0xff);
		pos += 2;
		pkg.checkSum = ((tcpBuff[pos]&0xff)<<8)+(tcpBuff[pos+1]&0xff);
		pos += 2;
		pkg.urgentPointer = ((tcpBuff[pos]&0xff)<<8)+(tcpBuff[pos+1]&0xff);
		pos += 2;
		TcpOption option = null;
		for(;pos-off<pkg.headerlen;){
			switch(tcpBuff[pos]){
			case MaximumSegmentSize:
				option = new TcpOption();
				option.kind = MaximumSegmentSize;
				option.val = ((tcpBuff[pos+2]&0xff)<<8)+(tcpBuff[pos+3]&0xff);
				pkg.optionList.add(option);
				pos += 4;
				break;
			case WidnowScale:
				option = new TcpOption();
				option.kind = WidnowScale;
				option.val = ((tcpBuff[pos+2]&0xff)<<5);
				pkg.optionList.add(option);
				pos += 3;
				break;
			case SackPermitted:
				option = new TcpOption();
				option.kind = SackPermitted;
				pkg.optionList.add(option);
				pos += 2;
				break;
			default:
				++pos;
				break;
			}
		}
		if(extraPayload){
			if(tcpBuff.length-pos>0){
				pkg.extraData = new byte[tcpBuff.length-pos];
				System.arraycopy(tcpBuff,pos, pkg.extraData, 0, pkg.extraData.length);
				pos+=pkg.extraData.length;
			}
		}
		return pos-off;
	}
	
	public static TcpOption newMaximumSegmentSize(int val){
		TcpOption option = new TcpOption();
		option.kind = MaximumSegmentSize;
		option.val = val;
		return option;
	}
	
	public static TcpOption newWindowScale(int val){
		TcpOption option = new TcpOption();
		option.kind = WidnowScale;
		option.val = val;
		return option;
	}
	
	public static TcpOption newSackPermitted(){
		TcpOption option = new TcpOption();
		option.kind = SackPermitted;
		return option;
	}
	
	public static int calcTcpChecksum(TcpPackage pkg, byte[] srcAddr, byte[] destAddr, int tcpLen, byte[] payload, int off, int len){
		byte[] tcpBuff = new byte[2048];
		int checkSum = 0;
		checkSum += ((srcAddr[0]&0xff)<<8)+(srcAddr[1]&0xff) + ((srcAddr[2]&0xff)<<8)+(srcAddr[3]&0xff);
		checkSum+=((destAddr[0]&0xff)<<8)+(destAddr[1]&0xff) + ((destAddr[2]&0xff)<<8)+(destAddr[3]&0xff);
		checkSum += 6;
		checkSum += tcpLen;
		int templen = packToTcpBuff(pkg, tcpBuff, 0, 2048);
		for(int i=0; i<templen;++i){
			checkSum += (i%2==0)?((tcpBuff[i]&0xff)<<8):(tcpBuff[i]&0xff);
		}
		if(pkg.extraData==null && payload!=null){
			for(int i=0; i<len;++i){
				checkSum += (i%2==0)?((payload[i+off]&0xff)<<8):(payload[i+off]&0xff);
			}
		}
		return  ~(((checkSum>>16)&0xffff)+(checkSum&0xffff));
	}
	
	public static int calcTcpBuffChecksum(byte[] srcAddr, byte[] destAddr, int tcpLen, byte[] buff, int off, int len){
		int checkSum = 0;
		checkSum += ((srcAddr[0]&0xff)<<8)+(srcAddr[1]&0xff) + ((srcAddr[2]&0xff)<<8)+(srcAddr[3]&0xff);
		checkSum+=((destAddr[0]&0xff)<<8)+(destAddr[1]&0xff) + ((destAddr[2]&0xff)<<8)+(destAddr[3]&0xff);
		checkSum += 6;
		checkSum += tcpLen;
		for(int i=0; i<len;++i){
			checkSum += (i%2==0)?((buff[off+i]&0xff)<<8):(buff[off+i]&0xff);
		}
		return  ~(((checkSum>>16)&0xffff)+(checkSum&0xffff));
	}
	
	public static int calcTcpMark(TcpPackage pkg){
		return (pkg.nonce?0x100:0)+(pkg.cwr?0x080:0)+(pkg.ecn?0x040:0)+(pkg.urg?0x020:0)+(pkg.ack?0x010:0)
				+(pkg.push?0x008:0)+(pkg.reset?0x004:0)+(pkg.syn?0x002:0)+(pkg.fin?0x001:0);
	}
	

}
