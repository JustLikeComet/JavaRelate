import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MpegParser {
	private final int ProgramStartCode = 0x000001BA;
	private final int SystemHeaderStartCode = 0x000001BB;
	
	public static void analysePSHeader(byte[] buffer, int start, int len){
		long systemClockReferenceBase = ((long)(buffer[start]&0b00111000)<<27)
				                      | ((long)(buffer[start]&0b00000011)<<29)
				                      | ((long)(buffer[start+1]&0xff)<<20)
				                      | ((long)(buffer[start+2]&0xf8)<<12)
				                      | ((long)(buffer[start+2]&0x3)<<13)
				                      | ((long)(buffer[start+3]&0xff)<<5)
				                      | ((long)(buffer[start+4]&0xf8)>>3);
		int systemClockExtension = ((buffer[start+4]&0x3)<<7)
			    | ((buffer[start+5]&0xfe)>>1);
		int muxRate = ((buffer[start+6]&0xff)<<14)
				    | ((buffer[start+7]&0xff)<<6)
				    | ((buffer[start+8]&0xfc)>>2);
		int packStuffingLen = buffer[start+9]&0x7;

		System.out.println("Program Stream info :" );
		System.out.println("System Clock Reference base " + systemClockReferenceBase );
		System.out.println("System Clock Extension " + systemClockExtension );
		System.out.println("muxing rate " + muxRate );
		System.out.println("Pack Stuffing length " + packStuffingLen +" content : " );
		
		for(int i=0; i<packStuffingLen; ++i){
			System.out.print(String.format(" %02X ", buffer[start+10+i] ));
		}
		
		System.out.println();
	}
	
	public static void analyseSystemHeader(byte[] buffer, int start, int len){
		int rateBound = ((buffer[start+0]&0x7f)<<15)
				      | ((buffer[start+1]&0xff)<<7)
				      | ((buffer[start+2]&0xfe)>>1);
		int audioBound = ((buffer[start+3]&0x8c)>>2);
		int fixedFlag = (buffer[start+3]&0x2)>>1;
		int CSPSFlag = (buffer[start+3]&0x1)>>7;
		int systemAudioLockFlag = (buffer[start+4]&0x80)>>7;
		int systemVideoLockFlag = (buffer[start+4]&0x40)>>6;
		int videoBound = (buffer[start+4]&0x1f);
		int packetRateRestrictionFlag = (buffer[start+5]&0x80)>>7;
		System.out.println("System header info :" );
		System.out.println("Rate Bound "+rateBound );
		System.out.println("Audio Bound "+audioBound );
		System.out.println("Fixed Flag "+fixedFlag );
		System.out.println("CSPS Flag "+CSPSFlag );
		System.out.println("System Audio Lock Flag "+systemAudioLockFlag );
		System.out.println("System Video Lock Flag "+systemVideoLockFlag );
		System.out.println("Video Bound "+videoBound );
		System.out.println("Packet Rate Restriction flag "+packetRateRestrictionFlag );
		if(len>6){
			for(int i=6; i<len;){
				System.out.println(String.format("streamId %02X", buffer[start+i] ));
				int pstdBufferBoundScale = (buffer[start+i+1]&0x20)>>5;
				int pstdBufferSizeBound = (buffer[start+i+1]&0x1f)<<8 | (buffer[start+i+2]&0xff);
				i+=3;
			}
		}
	}
	
	public static void analyseSystemMap(byte[] buffer, int start, int len){
		int current_next_indicator = (buffer[start+0]&0x80)>>7;
		System.out.println("current_next_indicator "+current_next_indicator);
		int program_stream_map_version = (buffer[start+0]&0x1f);
		System.out.println("program_stream_map_version "+program_stream_map_version);
		int program_stream_info_length = ((buffer[start+2]&0xff)<<8) | ((buffer[start+3]&0xff));
		System.out.println("program_stream_info_length "+program_stream_info_length);
		int elementary_stream_map_length = ((buffer[start+program_stream_info_length+4]&0xff)<<8) |
				(buffer[start+program_stream_info_length+4+1]&0xff);
		System.out.println("elementary_stream_map_length "+elementary_stream_map_length);
		int esmlStart = start+program_stream_info_length+4+2;
		for(int esmlPos=0; esmlPos<elementary_stream_map_length;){
			int esilLen = ((buffer[esmlStart+esmlPos+2]&0xff)<<8) + (buffer[esmlStart+esmlPos+3]&0xff);
			System.out.println(String.format("Streamtype %02X \nelementary_stream_id %02X\nelementary_stream_info_length %d",
					(buffer[esmlStart+esmlPos+0]&0xff), (buffer[esmlStart+esmlPos+1]&0xff), esilLen));
			for(int i=0;i<esilLen;++i){
				System.out.print(String.format("%02X ", buffer[esmlStart+esmlPos+4+i]));
			}
			System.out.println();
			esmlPos+=4+esilLen;
		}
		int crcpos = start+program_stream_info_length+4+2+elementary_stream_map_length;
		System.out.println(String.format("crc %02X %02X %02X %02X", buffer[crcpos], buffer[crcpos+1], buffer[crcpos+2], buffer[crcpos+3]));
	}
	
	public static void analyseVideoStream(byte[] buffer, int start, int len){
		int flag1 = buffer[start+0]&0xff;
		int flag2 = buffer[start+1]&0xff;
		int PES_scrambling_control = ((flag1&0x30)>>4);
		int PES_priority = ((flag1&0x08)>>3);
		int data_alignment_indicator = ((flag1&0x04)>>2);
		int copyright = ((flag1&0x02)>>1);
		int original_or_copy = (flag1&0x01);
		System.out.println(String.format("mark1 %d , PES_scrambling_control %d PES_priority %d data_alignment_indicator %d copyright %d original_or_copy %d",
				((flag1&0xc0)>>6), PES_scrambling_control,
				PES_priority, data_alignment_indicator, copyright, original_or_copy));
		int PTS_DTS_flags= (flag2&0xc0)>>6;
		int ESCR_flag = (flag2&0x20)>>5; // not exist
		int ES_rate_flag = (flag2&0x10)>>4; // not exist
		int DSM_trick_mode_flag = (flag2&0x08)>>3; // not exist
		int additional_copy_info_flag = (flag2&0x04)>>2; // not exist
		int PES_CRC_flag = (flag2&0x02)>>1; // not exist
		int PES_extension_flag = (flag2&0x01); // not exist
		System.out.println(String.format("PTS_DTS_flags %d ESCR_flag %d ES_rate_flag %d DSM_trick_mode_flag %d additional_copy_info_flag %d PES_CRC_flag %d PES_extension_flag %d",
				PTS_DTS_flags, ESCR_flag, ES_rate_flag, DSM_trick_mode_flag, 
				additional_copy_info_flag,PES_CRC_flag, PES_extension_flag));
		int paramDataLen = buffer[start+2]&0xff;
		byte[] timestamp = new byte[paramDataLen];
		for(int i=0; i<paramDataLen; ++i){
			timestamp[i] = buffer[start+3+i];
		}
		if(paramDataLen>0){
			int i = 0;
			if(PTS_DTS_flags ==2){
				long pts = (((long)(timestamp[i+0]&0x0e))<<29)+
						(((long)(timestamp[i+1]&0xff))<<22)+
						(((long)(timestamp[i+2]&0xfe))<<14)+
						(((long)(timestamp[i+3]&0xff))<<7)+
						(((long)(timestamp[i+4]&0xfe))>>1);
				System.out.println(String.format("mark %d pts %d ", (timestamp[i+0]&0xf0)>>4,pts));
				i+=5;
			}else if(PTS_DTS_flags ==3){
				long pts = (((long)(timestamp[i+0]&0x0e))<<29)+
						(((long)(timestamp[i+1]&0xff))<<22)+
						(((long)(timestamp[i+2]&0xfe))<<14)+
						(((long)(timestamp[i+3]&0xff))<<7)+
						(((long)(timestamp[i+4]&0xfe))>>1);
				long dts = (((long)(timestamp[i+5]&0x0e))<<29)+
						(((long)(timestamp[i+6]&0xff))<<22)+
						(((long)(timestamp[i+7]&0xfe))<<14)+
						(((long)(timestamp[i+8]&0xff))<<7)+
						(((long)(timestamp[i+9]&0xfe))>>1);
				System.out.println(String.format("mark1 %d pts %d mark2 %d dts %d", 
						(timestamp[i+0]&0xf0)>>4,pts,(timestamp[i+5]&0xf0)>>4, dts));
				i+=10;
			}
			if(ESCR_flag==1){
				long ESCR_base = (((long)(timestamp[i+0]&0x38))<<27)+
						(((long)(timestamp[i+0]&0x03))<<28)+
						(((long)(timestamp[i+1]&0xff))<<20)+
						(((long)(timestamp[i+2]&0xf8))<<12)+
						(((long)(timestamp[i+2]&0x03))<<13)+
						(((long)(timestamp[i+3]&0xff))<<5)+
						(((long)(timestamp[i+4]&0xf8))>>3);
				int ESCR_extension = ((timestamp[i+4]&0x3)<<7)+((timestamp[i+5]&0xfe)>>1);
				System.out.println(String.format("ESCR_base %d ESCR_extension %d ", ESCR_base, ESCR_extension));
				i += 6;
			}
			if(ES_rate_flag==1){
				int ES_rate = ((timestamp[i+0]&0x7f)<<15)+((timestamp[i+1]&0xff)<<7)+((timestamp[i+2]&0xff)>>1);
				System.out.println(String.format("ES_rate %d ", ES_rate));
				i += 3;
			}
			if(DSM_trick_mode_flag==1){
				int trick_mode_control = ((timestamp[i+0]&0xe0)>>5);
				System.out.print("trick_mode_control "+trick_mode_control);
				i ++;
			}
		}
		System.out.println(" param data len "+paramDataLen);
		System.out.println(" video data len "+(len-3-paramDataLen));
		int displayLen = (len-3-paramDataLen)<10?(len-3-paramDataLen):10;
		for(int i=0; i<displayLen; ++i){
			System.out.print(String.format(" %02X ", 0xff&buffer[start+3+paramDataLen+i]));
		}
		System.out.println();
	}
	
	public static void analysePSData(ByteBuffer inByteBuffer){
		boolean stillChecking = true;
		while(stillChecking){
			if(inByteBuffer.position()>=inByteBuffer.limit()){
				stillChecking = false;
				continue;
			}
			if(inByteBuffer.get(inByteBuffer.position())==(byte)0
					&& inByteBuffer.get(inByteBuffer.position()+1)==(byte)0
					&& inByteBuffer.get(inByteBuffer.position()+2)==(byte)1 ){
				byte type = inByteBuffer.get(inByteBuffer.position()+3);
				System.out.println(String.format("000001%02X", (type&0xff)));
				switch(type&0xff){
					case 0xba:
					{
						int stuffingLen = inByteBuffer.get(inByteBuffer.position()+4+9)&0x7;
						analysePSHeader(inByteBuffer.array(), inByteBuffer.position()+4, 10+stuffingLen);
						int systemHeaderLen = 0;
						if(inByteBuffer.get( inByteBuffer.position()+4+10+stuffingLen )==(byte)0
								&& inByteBuffer.get(inByteBuffer.position()+4+11+stuffingLen)==(byte)0
								&& inByteBuffer.get(inByteBuffer.position()+4+12+stuffingLen)==(byte)1
								&& (inByteBuffer.get(inByteBuffer.position()+4+13+stuffingLen)&0xff)==0xbb ){
							byte byte1 = inByteBuffer.get(inByteBuffer.position()+4+14+stuffingLen);
							byte byte2 = inByteBuffer.get(inByteBuffer.position()+4+15+stuffingLen);
							systemHeaderLen = 4+2+(byte2&0xff) + ((byte1&0xff)<<8);
							System.out.println("000001BB");
							analyseSystemHeader(inByteBuffer.array(), inByteBuffer.position()+14+stuffingLen+6, systemHeaderLen-6);
						}
						inByteBuffer.position(inByteBuffer.position()+14+stuffingLen+systemHeaderLen);
						break;
					}
					case 0xbc:
					{
						byte byte1 = inByteBuffer.get(inByteBuffer.position()+4);
						byte byte2 = inByteBuffer.get(inByteBuffer.position()+5);
						int dataLen = 4+2+ (byte2&0xff) + (((byte1&0xff))<<8);
						analyseSystemMap(inByteBuffer.array(), inByteBuffer.position()+6, dataLen-6);
						inByteBuffer.position(inByteBuffer.position()+dataLen);
						break;
					}
					default:
					{
						byte byte1 = inByteBuffer.get(inByteBuffer.position()+4);
						byte byte2 = inByteBuffer.get(inByteBuffer.position()+5);
						int dataLen = 4+2+ (byte2&0xff) + (((byte1&0xff))<<8);
						if(dataLen>inByteBuffer.limit()-inByteBuffer.position())
						{
							stillChecking = false;
						}else{
							//System.out.println(String.format("000001%02X", (type&0xff)));
							if((type&0xf0)==0xe0){
								//inByteBuffer.array()[inByteBuffer.position()+3] = (byte)0xe0;
								analyseVideoStream(inByteBuffer.array(), inByteBuffer.position()+6, dataLen-6);
							}
							inByteBuffer.position(inByteBuffer.position()+dataLen);
						}
						break;
					}
				}
				/*
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
			}
		}
		inByteBuffer.compact();
	}
	
	public static void parserMpegData(ByteBuffer inByteBuffer, ByteBuffer outByteBuffer){
		while(inByteBuffer.position()<inByteBuffer.limit()){
			//for(int i=0; i<320; ++i){
			//	System.out.print(String.format(" %02X ", 0xff&inByteBuffer.get(i)));
			//}
			//System.out.println();
			//System.out.println(String.format("%d   %02X%02X%02X",inByteBuffer.position(), 
			//		0xff&inByteBuffer.get(inByteBuffer.position()),
			//		0xff&inByteBuffer.get(inByteBuffer.position()+1),
			//		0xff&inByteBuffer.get(inByteBuffer.position()+2)));
			if(inByteBuffer.get(inByteBuffer.position())==(byte)0
					&& inByteBuffer.get(inByteBuffer.position()+1)==(byte)0
					&& inByteBuffer.get(inByteBuffer.position()+2)==(byte)1 ){
				byte type = inByteBuffer.get(inByteBuffer.position()+3);
				System.out.println(String.format("000001%02X", (type&0xff)));
				switch(type&0xff){
					case 0xba:
					{
						int stuffingLen = inByteBuffer.get(inByteBuffer.position()+4+9)&0x7;
						int systemHeaderLen = 0;
						if(inByteBuffer.get( inByteBuffer.position()+4+10+stuffingLen )==(byte)0
								&& inByteBuffer.get(inByteBuffer.position()+4+11+stuffingLen)==(byte)0
								&& inByteBuffer.get(inByteBuffer.position()+4+12+stuffingLen)==(byte)1
								&& (inByteBuffer.get(inByteBuffer.position()+4+13+stuffingLen)&0xff)==0xbb ){
							byte byte1 = inByteBuffer.get(inByteBuffer.position()+4+14+stuffingLen);
							byte byte2 = inByteBuffer.get(inByteBuffer.position()+4+15+stuffingLen);
							systemHeaderLen = 4+2+(byte2&0xff) + ((byte1&0xff)<<8);
						}
						outByteBuffer.put(inByteBuffer.array(), inByteBuffer.position(), 14+stuffingLen);
						inByteBuffer.position(inByteBuffer.position()+14+stuffingLen+systemHeaderLen);
						break;
					}
					default:
					{
						byte byte1 = inByteBuffer.get(inByteBuffer.position()+4);
						byte byte2 = inByteBuffer.get(inByteBuffer.position()+5);
						int dataLen = 4+2+ (byte2&0xff) + (((byte1&0xff))<<8);
						if((type&0xf0)==0xe0){
							inByteBuffer.array()[inByteBuffer.position()+3] = (byte)0xe0;
							outByteBuffer.put(inByteBuffer.array(), inByteBuffer.position(), dataLen);
						}
						inByteBuffer.position(inByteBuffer.position()+dataLen);
						break;
					}
				}
				;
			}
		}
		inByteBuffer.compact();
		outByteBuffer.flip();
		/*
		DataOutputStream dout = null;
		try {
			dout = new DataOutputStream(new FileOutputStream(new File("test001.dat")));
			dout.write(outByteBuffer.array());
			dout.flush();
			dout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	private void addPaHeader(ByteBuffer byteBuffer, long systemRef){
	}

	public static void main(String[] args) throws Exception {
		ByteBuffer byteBuffer = ByteBuffer.allocate(512*1024);
		DataInputStream dataInStream = new DataInputStream(new FileInputStream(new File("G:\\testdata\\pgdata0001.ps")));
		while(dataInStream.available()>0){
			if(byteBuffer.remaining()>0){
				int readlen = dataInStream.read(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
				byteBuffer.position(byteBuffer.position()+readlen);
			}
			byteBuffer.flip();
			analysePSData(byteBuffer);
		}
		dataInStream.close();
	}

}
