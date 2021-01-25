import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

public class MpegParser {
	private static final int pesMediaSplitValue = 8168;
	
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
		System.out.println("System Clock Reference base " + systemClockReferenceBase +" seconds : "+calcReferenceClockToSeconds(systemClockReferenceBase, 0));
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
		System.out.println("program_stream_info data");
		int psilStart = start+4;
		for(int psilPos=0; psilPos<program_stream_info_length;psilPos++){
			System.out.print(String.format("%02X ", buffer[psilStart+psilPos]));
		}
		System.out.println();
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
				System.out.println(String.format("mark %d pts %d seconds %d ", (timestamp[i+0]&0xf0)>>4,pts,  calcReferenceClockToSeconds(pts, 0)));
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
			System.out.print("Stuffer bytes:");
			for(; i<paramDataLen; ++i){
				System.out.print(String.format("%02X ", buffer[start+3+i]));
			}
			System.out.println();
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
						System.out.println("dataLen :"+dataLen);
						analyseSystemMap(inByteBuffer.array(), inByteBuffer.position()+6, dataLen-6);
						inByteBuffer.position(inByteBuffer.position()+dataLen);
						break;
					}
					default:
					{
						byte byte1 = inByteBuffer.get(inByteBuffer.position()+4);
						byte byte2 = inByteBuffer.get(inByteBuffer.position()+5);
						int dataLen = 4+2+ (byte2&0xff) + (((byte1&0xff))<<8);
						System.out.println("dataLen :"+dataLen);
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
						int dataLen = 4+2+ (inByteBuffer.get(inByteBuffer.position()+5)&0xff) + (((inByteBuffer.get(inByteBuffer.position()+4)&0xff))<<8);
						System.out.println("dataLen:"+dataLen);
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
						System.out.println("dataLen:"+dataLen);
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
	
	private static int crc32(int crc, byte[] buff, int off, int len){
		final int[] crcTable = { 0x00000000, 0xB71DC104, 0x6E3B8209, 0xD926430D, 0xDC760413, 0x6B6BC517,
		        0xB24D861A, 0x0550471E, 0xB8ED0826, 0x0FF0C922, 0xD6D68A2F, 0x61CB4B2B,
		        0x649B0C35, 0xD386CD31, 0x0AA08E3C, 0xBDBD4F38, 0x70DB114C, 0xC7C6D048,
		        0x1EE09345, 0xA9FD5241, 0xACAD155F, 0x1BB0D45B, 0xC2969756, 0x758B5652,
		        0xC836196A, 0x7F2BD86E, 0xA60D9B63, 0x11105A67, 0x14401D79, 0xA35DDC7D,
		        0x7A7B9F70, 0xCD665E74, 0xE0B62398, 0x57ABE29C, 0x8E8DA191, 0x39906095,
		        0x3CC0278B, 0x8BDDE68F, 0x52FBA582, 0xE5E66486, 0x585B2BBE, 0xEF46EABA,
		        0x3660A9B7, 0x817D68B3, 0x842D2FAD, 0x3330EEA9, 0xEA16ADA4, 0x5D0B6CA0,
		        0x906D32D4, 0x2770F3D0, 0xFE56B0DD, 0x494B71D9, 0x4C1B36C7, 0xFB06F7C3,
		        0x2220B4CE, 0x953D75CA, 0x28803AF2, 0x9F9DFBF6, 0x46BBB8FB, 0xF1A679FF,
		        0xF4F63EE1, 0x43EBFFE5, 0x9ACDBCE8, 0x2DD07DEC, 0x77708634, 0xC06D4730,
		        0x194B043D, 0xAE56C539, 0xAB068227, 0x1C1B4323, 0xC53D002E, 0x7220C12A,
		        0xCF9D8E12, 0x78804F16, 0xA1A60C1B, 0x16BBCD1F, 0x13EB8A01, 0xA4F64B05,
		        0x7DD00808, 0xCACDC90C, 0x07AB9778, 0xB0B6567C, 0x69901571, 0xDE8DD475,
		        0xDBDD936B, 0x6CC0526F, 0xB5E61162, 0x02FBD066, 0xBF469F5E, 0x085B5E5A,
		        0xD17D1D57, 0x6660DC53, 0x63309B4D, 0xD42D5A49, 0x0D0B1944, 0xBA16D840,
		        0x97C6A5AC, 0x20DB64A8, 0xF9FD27A5, 0x4EE0E6A1, 0x4BB0A1BF, 0xFCAD60BB,
		        0x258B23B6, 0x9296E2B2, 0x2F2BAD8A, 0x98366C8E, 0x41102F83, 0xF60DEE87,
		        0xF35DA999, 0x4440689D, 0x9D662B90, 0x2A7BEA94, 0xE71DB4E0, 0x500075E4,
		        0x892636E9, 0x3E3BF7ED, 0x3B6BB0F3, 0x8C7671F7, 0x555032FA, 0xE24DF3FE,
		        0x5FF0BCC6, 0xE8ED7DC2, 0x31CB3ECF, 0x86D6FFCB, 0x8386B8D5, 0x349B79D1,
		        0xEDBD3ADC, 0x5AA0FBD8, 0xEEE00C69, 0x59FDCD6D, 0x80DB8E60, 0x37C64F64,
		        0x3296087A, 0x858BC97E, 0x5CAD8A73, 0xEBB04B77, 0x560D044F, 0xE110C54B,
		        0x38368646, 0x8F2B4742, 0x8A7B005C, 0x3D66C158, 0xE4408255, 0x535D4351,
		        0x9E3B1D25, 0x2926DC21, 0xF0009F2C, 0x471D5E28, 0x424D1936, 0xF550D832,
		        0x2C769B3F, 0x9B6B5A3B, 0x26D61503, 0x91CBD407, 0x48ED970A, 0xFFF0560E,
		        0xFAA01110, 0x4DBDD014, 0x949B9319, 0x2386521D, 0x0E562FF1, 0xB94BEEF5,
		        0x606DADF8, 0xD7706CFC, 0xD2202BE2, 0x653DEAE6, 0xBC1BA9EB, 0x0B0668EF,
		        0xB6BB27D7, 0x01A6E6D3, 0xD880A5DE, 0x6F9D64DA, 0x6ACD23C4, 0xDDD0E2C0,
		        0x04F6A1CD, 0xB3EB60C9, 0x7E8D3EBD, 0xC990FFB9, 0x10B6BCB4, 0xA7AB7DB0,
		        0xA2FB3AAE, 0x15E6FBAA, 0xCCC0B8A7, 0x7BDD79A3, 0xC660369B, 0x717DF79F,
		        0xA85BB492, 0x1F467596, 0x1A163288, 0xAD0BF38C, 0x742DB081, 0xC3307185,
		        0x99908A5D, 0x2E8D4B59, 0xF7AB0854, 0x40B6C950, 0x45E68E4E, 0xF2FB4F4A,
		        0x2BDD0C47, 0x9CC0CD43, 0x217D827B, 0x9660437F, 0x4F460072, 0xF85BC176,
		        0xFD0B8668, 0x4A16476C, 0x93300461, 0x242DC565, 0xE94B9B11, 0x5E565A15,
		        0x87701918, 0x306DD81C, 0x353D9F02, 0x82205E06, 0x5B061D0B, 0xEC1BDC0F,
		        0x51A69337, 0xE6BB5233, 0x3F9D113E, 0x8880D03A, 0x8DD09724, 0x3ACD5620,
		        0xE3EB152D, 0x54F6D429, 0x7926A9C5, 0xCE3B68C1, 0x171D2BCC, 0xA000EAC8,
		        0xA550ADD6, 0x124D6CD2, 0xCB6B2FDF, 0x7C76EEDB, 0xC1CBA1E3, 0x76D660E7,
		        0xAFF023EA, 0x18EDE2EE, 0x1DBDA5F0, 0xAAA064F4, 0x738627F9, 0xC49BE6FD,
		        0x09FDB889, 0xBEE0798D, 0x67C63A80, 0xD0DBFB84, 0xD58BBC9A, 0x62967D9E,
		        0xBBB03E93, 0x0CADFF97, 0xB110B0AF, 0x060D71AB, 0xDF2B32A6, 0x6836F3A2,
		        0x6D66B4BC, 0xDA7B75B8, 0x035D36B5, 0xB440F7B1, 0x00000001};

	    for (int i=0; i<len; ++i){
	    	crc = crcTable[(crc&0xff) ^ (buff[off+i]&0xff)] ^ ((crc>> 8)&0xffffff);
	    }
	    return crc;
	}
	
	private static long calcReferenceClock(long ms){
		return((27000 * ms) / 300)%(((long)1)<<33);
	}
	
	private static long calcReferenceClockToSeconds(long src, int ext){
		return (src*300+ext)/27000;
	}
	
	private static int calcReferenceClockExtension(long ms){
		return (int)(((27000 * ms) / 1)%300);
	}
	
	private static void addPsHeader(ByteBuffer buffer, long timems, int muxrate, int sequence){
		long scr = calcReferenceClock(timems);
		int extention = calcReferenceClockExtension(timems);
		buffer.putInt(0x1BA);
		buffer.put((byte)(0b01000100|((scr>>27)&0b00111000)|((scr>>28)&0b00000011)));// SCR 32~30 29~28
		buffer.put((byte)((scr>>20)&0xff)); // SCR 27~20
		buffer.put((byte)(0b00000100|((scr>>12)&0b11111000)|((scr>>13)&0b00000011))); // SCR 19~15 14~13
		buffer.put((byte)((scr>>5)&0xff)); // SCR 12~5
		buffer.put((byte)(0b00000100|((scr<<3)&0b11111000)|((extention>>7)&0b00000011))); // SCR 4~0 extention 8~7
		buffer.put((byte)(0b00000001|((extention<<1)&0b11111110))); // extention 6~0
		buffer.put((byte)((muxrate>>14)&0xff)); // muxrate 21~14
		buffer.put((byte)((muxrate>>6)&0xff)); // muxrate 13~6
		buffer.put((byte)(0b00000011|((muxrate<<2)&0b11111100))); // muxrate 5~0
		buffer.put((byte)(0xFE));
		buffer.put((byte)(0xFF));
		buffer.put((byte)(0xFF));
		buffer.putInt(sequence);
	}
	
	private static void addPSMap(ByteBuffer buffer, boolean nextIndicator, int version){
		/*
		program_stream_map_version 18
		program_stream_info_length 36
		program_stream_info data
		40 0E 48 4B 01 00 13 4E D2 3D 63 27 00 FF FF FF 41 12 48 4B 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 
		elementary_stream_map_length 48
		Streamtype 1B 
		elementary_stream_id E0
		elementary_stream_info_length 28
		42 0E 07 10 10 EA 02 C0 02 40 11 30 00 00 1C 20 2A 0A 7F FF 00 00 07 08 1F FE 58 48 
		Streamtype 90 
		elementary_stream_id C0
		elementary_stream_info_length 12
		43 0A 01 40 FE 00 7D 03 03 E8 03 FF 
		crc CF CA DB CA
		 */
		byte psInfoData[] = {0x40, 0x0E, 0x48, 0x4B, 0x01, 0x00, 0x13, 0x4E,
				(byte)0xD2, 0x3D, 0x63, 0x27, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF,
				0x41, 0x12, 0x48, 0x4B, 0x00, 0x01, 0x02, 0x03,
				0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B,
				0x0C, 0x0D, 0x0E, 0x0F };
		byte esMapData[] = {0x1B, (byte)0xE0,0x00, 0x1c, 0x42, 0x0E, 0x07, 0x10,
				0x10, (byte)0xEA, 0x02, (byte)0xC0, 0x02, 0x40, 0x11, 0x30,
				0x00, 0x00, 0x1C, 0x20, 0x2A, 0x0A, 0x7F, (byte)0xFF,
				0x00, 0x00, 0x07, 0x08, 0x1F, (byte)0xFE, 0x58, 0x48};
		buffer.putInt(0x1BC);
		buffer.putShort((short)(2+2+psInfoData.length+2+esMapData.length+4));
		byte indicatorAndVer = (byte)((nextIndicator?0b10000000:0)|(version&0b00011111));
		buffer.put(indicatorAndVer);
		buffer.put((byte)0b00000001);
		buffer.putShort((short)psInfoData.length);
		buffer.put(psInfoData);
		buffer.putShort((short)esMapData.length);
		buffer.put(esMapData);
		int crcval = crc32(-1,buffer.array(),0,buffer.position());
		//System.out.println(String.format("crc %08X", crcval));
		buffer.putInt(crcval);
	}
	
	/*
=================================================
000001BA
Program Stream info :
System Clock Reference base 4024486738
System Clock Extension 0
muxing rate 18777
Pack Stuffing length 6 content : 
 FF  FF  00  68  A9  9F 
000001BC
dataLen :100
current_next_indicator 1
program_stream_map_version 18
program_stream_info_length 36
program_stream_info data
40 0E 48 4B 01 00 13 4E D2 3D 63 27 00 FF FF FF 41 12 48 4B 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 
elementary_stream_map_length 48
Streamtype 1B 
elementary_stream_id E0
elementary_stream_info_length 28
42 0E 07 10 10 EA 02 C0 02 40 11 30 00 00 1C 20 2A 0A 7F FF 00 00 07 08 1F FE 58 48 
Streamtype 90 
elementary_stream_id C0
elementary_stream_info_length 12
43 0A 01 40 FE 00 7D 03 03 E8 03 FF 
crc CF CA DB CA
000001E0
dataLen :24
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 2 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
mark 2 pts 3219180370 
Stuffer bytes:FF FC 
 param data len 7
 video data len 8
 00  00  00  01  09  F0  00  00 
000001E0
dataLen :32
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FF FF FC 
 param data len 5
 video data len 18
 00  00  00  01  67  4D  00  1E  95  A8 
000001E0
dataLen :20
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FC 
 param data len 3
 video data len 8
 00  00  00  01  68  EE  3C  80 
000001E0
dataLen :20
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FC 
 param data len 2
 video data len 9
 00  00  00  01  06  E5  01  2D  80 
000001E0
dataLen :8180
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FF FF FD 
 param data len 5
 video data len 8166
 00  00  00  01  65  B8  00  00  0D  9C 
000001E0
dataLen :8180
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 0 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FF 
 param data len 3
 video data len 8168
 CE  3C  51  2F  DC  33  74  A4  67  05 
000001E0
dataLen :8180
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 0 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FF 
 param data len 3
 video data len 8168
 E7  79  89  31  92  00  67  C5  E9  1E 
000001E0
dataLen :5000
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 0 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FA 
 param data len 3
 video data len 4988
 0B  5D  1F  35  28  FA  D4  7B  A8  0F 
=================================================
000001BA
Program Stream info :
System Clock Reference base 4024490338
System Clock Extension 0
muxing rate 18777
Pack Stuffing length 6 content : 
 FF  FF  00  68  A9  A0 
000001E0
dataLen :24
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 2 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
mark 2 pts 3219183970 
Stuffer bytes:FF FC 
 param data len 7
 video data len 8
 00  00  00  01  09  F0  00  00 
000001E0
dataLen :608
mark1 2 , PES_scrambling_control 0 PES_priority 1 data_alignment_indicator 1 copyright 0 original_or_copy 0
PTS_DTS_flags 0 ESCR_flag 0 ES_rate_flag 0 DSM_trick_mode_flag 0 additional_copy_info_flag 0 PES_CRC_flag 0 PES_extension_flag 0
Stuffer bytes:FF FF FF FF F8 
 param data len 5
 video data len 594
 00  00  00  01  61  E0  22  6F  15  0C 



	 */
	private static void addPesData(ByteBuffer buffer, int streamid, boolean data_alignment_indicator,
			long dtsMs,ByteBuffer inbuffer, int off, int len){
		int startpos = buffer.position();
		buffer.putInt(0x100|streamid);
		int datalen = 3+(dtsMs>-1?5:0)+len;
		int padddatalen = ((datalen+6+3)/4)*4; // 64bits align
		int padsize = padddatalen-datalen-6;
		padddatalen -= 6;
		buffer.putShort((short)padddatalen);
		buffer.put((byte)(0b10001000|(data_alignment_indicator?0b00000100:0))); // indicater data is startwith 00000001
		buffer.put((byte)(dtsMs>-1?0x80:0));
		buffer.put((byte)(padsize+(dtsMs>-1?5:0))); 
		if(dtsMs>-1){
			long dtsRef = calcReferenceClock(dtsMs);
			buffer.put((byte)(0b00100001|((dtsRef>>29)&0x0E)));
			buffer.put((byte)((dtsRef>>22)&0xff));
			buffer.put((byte)(0b00000001|((dtsRef>>14)&0xfe)));
			buffer.put((byte)((dtsRef>>7)&0xff));
			buffer.put((byte)(0b00000001|((dtsRef<<1)&0xfe)));
		}
		for(int i=0; i<padsize; ++i){
			buffer.put((byte)0xff);
		}
		buffer.put(inbuffer.array(), off, len);
		if(buffer.position()-startpos!=padddatalen+6){
			System.out.println("data incorrect, padsize is "+padsize);
		}
	}
	
	private static boolean isKeyFrame(ByteBuffer bytebuff, List<Integer> posList){
		boolean is5mark = false, is7mark = false, is8mark=false;
		int i=0;
		for(; i<bytebuff.limit()-4; ++i){
			if(bytebuff.array()[i]==0 && bytebuff.array()[i+1]==0 && bytebuff.array()[i+2]==0 && bytebuff.array()[i+3]==1){
				posList.add(i);
				switch((bytebuff.array()[i+4]&0x0f)){
				case 5:
					is5mark = true;
					break;
				case 7:
					is7mark = true;
					break;
				case 8:
					is8mark = true;
					break;
				}
				i+=4;
			}
		}
		if(i>=bytebuff.limit()-4){
			posList.add(bytebuff.limit());
		}
		return (is5mark && is7mark && is8mark);
	}
	
	private static void convertH264ToPes(ByteBuffer h264buff, int start, int len, long dts, ByteBuffer outBuff){
		if(len>pesMediaSplitValue){
			int splitTotal = (len+pesMediaSplitValue-1)/pesMediaSplitValue;
			addPesData(outBuff, 0xe0, true, dts, h264buff, start, pesMediaSplitValue);
			for(int i=1;i<splitTotal; ++i){
				addPesData(outBuff, 0xe0, false, -1, h264buff, start+i*pesMediaSplitValue, 
						(i==splitTotal-1)?(len%pesMediaSplitValue):pesMediaSplitValue);
			}
		}else{
			addPesData(outBuff, 0xe0, true, dts, h264buff, start, len);
		}
	}
	
	public static void packageOneFrame(ByteBuffer inbuffer, ByteBuffer outBuffer, long dts, int tickcount){
		List<Integer> posList = new ArrayList<Integer>();
		addPsHeader(outBuffer, dts, 18777, tickcount);
		if(isKeyFrame(inbuffer, posList)){
			addPSMap(outBuffer, true, 1);
		}
		convertH264ToPes(inbuffer, posList.get(0), posList.get(1)-posList.get(0), dts, outBuffer);
		for(int i=0; i<posList.size()-1; ++i){
			convertH264ToPes(inbuffer, posList.get(i), posList.get(i+1)-posList.get(i), dts, outBuffer);
		}
	}

	public static void main(String[] args) throws Exception {
		/*
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
		*/
		
		/*
		ByteBuffer byteBuffer = ByteBuffer.allocate(512*1024);
		addPsHeader(byteBuffer, 10000, 18777, 0x00657401);
		addPSMap(byteBuffer, true, 1);
		byte[] pesTestData = {0x00,  0x00, 0x00, 0x01, 0x09, (byte)0xF0, 0x00, 0x00 };
		addPesData(byteBuffer, 0xe0, true, System.currentTimeMillis(),pesTestData);
		byteBuffer.flip();
		analysePSData(byteBuffer);
		*/
		
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(FFmpegInvoke.PARAM_reOption, "1");
		params.put(FFmpegInvoke.PARAM_bitStreamFilterVideo, "h264_mp4toannexb");
		//FFmpegInvoke invoke = FFmpegInvoke.getInstance("rtmp://192.168.7.119:1935/live/test001", null, null,"pipe:1", "copy", null, "mpegts", params);
		FFmpegInvoke invoke = FFmpegInvoke.getInstance("00000002.mp4", null, null,"pipe:1", "copy", null, "h264", params);
		invoke.runit();
		DataOutputStream dout = new DataOutputStream(new FileOutputStream(new File("testH264ToPs01.ps")));

		ByteBuffer pesBuffer = ByteBuffer.allocate(128*1024);
		ByteBuffer mpegtsBuffer = ByteBuffer.allocate(128*1024);
		int tickcount = 0x00650001;
		long dts = System.currentTimeMillis();
		while(invoke.checkProccessAlive()){
			int dataSize = invoke.getIn().available();
			if(dataSize>0){
				dataSize = invoke.getIn().read(mpegtsBuffer.array(), mpegtsBuffer.position(), mpegtsBuffer.remaining());
				mpegtsBuffer.position(mpegtsBuffer.position() + dataSize);
				mpegtsBuffer.flip();
				packageOneFrame(mpegtsBuffer, pesBuffer, dts, tickcount++);
				mpegtsBuffer.clear();
				dout.write(pesBuffer.array(), 0, pesBuffer.position());
				dout.flush();
				pesBuffer.clear();
				dts+=40;
			}
		}
		dout.close();
		invoke.close();
		
		/*
		byte[] b = {(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xBC, (byte) 0x00, (byte) 0x5E, (byte) 0xF6, (byte) 0xFF, (byte) 0x00, (byte) 0x24, (byte) 0x40, (byte) 0x0E, (byte) 0x48, (byte) 0x4B, (byte) 0x01, (byte) 0x00, (byte) 0x13, (byte) 0x83, (byte) 0xC2, (byte) 0xC6, (byte) 0xB4, (byte) 0xA7, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x41, (byte) 0x12, (byte) 0x48, (byte) 0x4B, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F, (byte) 0x00, (byte) 0x30, (byte) 0x1B, (byte) 0xE0, (byte) 0x00, (byte) 0x1C, (byte) 0x42, (byte) 0x0E, (byte) 0x07, (byte) 0x10, (byte) 0x10, (byte) 0xEA, (byte) 0x02, (byte) 0xC0, (byte) 0x02, (byte) 0x40, (byte) 0x11, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x1C, (byte) 0x21, (byte) 0x2A, (byte) 0x0A, (byte) 0x7F, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x08, (byte) 0x1F, (byte) 0xFE, (byte) 0x58, (byte) 0x48, (byte) 0x90, (byte) 0xC0, (byte) 0x00, (byte) 0x0C, (byte) 0x43, (byte) 0x0A, (byte) 0x01, (byte) 0x40, (byte) 0xFE, (byte) 0x00, (byte) 0x7D, (byte) 0x03, (byte) 0x03, (byte) 0xE8, (byte) 0x03, (byte) 0xFF};
		// //  0xF3, 0x6C, 0x19, 0x5F
		System.out.println(String.format("%08X",crc32(-1, b, 0, b.length) ));
		
		//
		System.out.println(calcReferenceClock(10000));
		ByteBuffer byteBuffer = ByteBuffer.allocate(20480);
		addPsHeader(byteBuffer, 10000, 18777, 0x00657401);
		int stuffingLen = byteBuffer.get(4+9)&0x7;
		analysePSHeader(byteBuffer.array(), 4, 10+stuffingLen);
		System.out.println(calcReferenceClock(10949012));
		*/
	}

}
