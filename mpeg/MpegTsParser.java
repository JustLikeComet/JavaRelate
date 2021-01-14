import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MpegTsParser {
	
	private ArrayList<Integer> videoStreams = new ArrayList<Integer>();
	private ArrayList<Integer> audioStreams = new ArrayList<Integer>();
	private ArrayList<Integer> pmtList = new ArrayList<Integer>();
	
	private void parsePesData(ByteBuffer inBuffer, int startPos, int adaptationMark, ByteBuffer outBuffer){
	}
	
	private void parsePmtHeader(ByteBuffer inBuffer, int startPos){
		int patId = 0xff&inBuffer.get(inBuffer.position()+startPos);
		int sectionLen = ((0xff&inBuffer.get(inBuffer.position()+startPos+1))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+2));
		sectionLen &= 0x0fff;
		int programNum = ((0xff&inBuffer.get(inBuffer.position()+startPos+3))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+4));
		inBuffer.get(inBuffer.position()+startPos+5);
		inBuffer.get(inBuffer.position()+startPos+6);
		inBuffer.get(inBuffer.position()+startPos+7);
		int pcrId = ((0xff&inBuffer.get(inBuffer.position()+startPos+1))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+9));
		pcrId &= 0x1fff;
		int programInfoLen = ((0xff&inBuffer.get(inBuffer.position()+startPos+10))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+11));
		programInfoLen &= 0x0fff;
		for(int i=9+programInfoLen; i<sectionLen-4;){
			int type = (0xff&inBuffer.get(inBuffer.position()+startPos+3+i));
			int elementaryId = ((0xff&inBuffer.get(inBuffer.position()+startPos+3+i+1))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+3+i+2));
			elementaryId &= 0x1fff;
			int esInfoLen = ((0xff&inBuffer.get(inBuffer.position()+startPos+3+i+3))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+3+i+4));
			esInfoLen &= 0x0fff;
			if(type==0x1b){
				videoStreams.add(elementaryId);
			}
			i+=5+esInfoLen;
		}
	}
	
	private void parsePatHeader(ByteBuffer inBuffer, int startPos){
		int patId = 0xff&inBuffer.get(inBuffer.position()+startPos);
		int sectionLen = ((0xff&inBuffer.get(inBuffer.position()+startPos+1))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+2));
		sectionLen &= 0x0fff;
		int tsStreamId = ((0xff&inBuffer.get(inBuffer.position()+startPos+3))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+4));
		int nextIndicator = inBuffer.get(inBuffer.position()+startPos+5)&1;
		int programInfoLen = sectionLen-9;
		for(int i=0; i<programInfoLen;){
			int programNum = ((0xff&inBuffer.get(inBuffer.position()+startPos+8+i))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+8+i+1));
			int pmtId = ((0xff&inBuffer.get(inBuffer.position()+startPos+8+i+2))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+8+i+3));
			if(programNum>0){
				pmtList.add( pmtId&0x1fff );
			}
			i+=4;
		}
	}

	private int analyseTSHeader(ByteBuffer inBuffer, int startPos, ByteBuffer outBuffer){
		int pid = ((0xff&inBuffer.get(inBuffer.position()+startPos+1))<<8)+(0xff&inBuffer.get(inBuffer.position()+startPos+2));
		byte byte1 = inBuffer.get(inBuffer.position()+startPos+3);
		int tsErrorIndicator = (pid&0x8000)>>15;
		int payloadUintStartIndicator = (pid&0x4000)>>14;
		int tsPriority = (pid&0x2000)>>13;
		pid &= 0x1fff;
		int tsScramblinCtrl = (byte1&0xc0)>>6;
		int adaptationFieldCtrl = (byte1&0x30)>>4;
		int continuityCounter = byte1&0xf;
		
		int offset = 0;
		if((adaptationFieldCtrl&2)>0){
			offset = (0xff&inBuffer.get(inBuffer.position()+startPos+4))+1;
		}
		
		if(pid==0){
			if(payloadUintStartIndicator!=0){
				offset += (0xff&inBuffer.get(inBuffer.position()+startPos+4+offset))+1;
			}
			parsePatHeader(inBuffer, startPos+4+offset);
		}
		else if(pmtList.contains(pid)){
			if(payloadUintStartIndicator!=0){
				offset += (0xff&inBuffer.get(inBuffer.position()+startPos+4+offset))+1;
			}
			parsePmtHeader(inBuffer, startPos+4+offset);
		}
		else if(videoStreams.contains(pid)){
			//parsePesData(inBuffer, startPos+4, adaptationFieldCtrl);
			outBuffer.put(inBuffer.array(), inBuffer.position()+startPos+4+offset, 188-4-offset);
			if(payloadUintStartIndicator==0 && offset>0){
				return 1;
			}
		}
		return 0;
	}
	
	public boolean exportTSToPes(ByteBuffer inBuffer, ByteBuffer pesBuffer){
		int startpos = 0;
		boolean isOnePackageDone = false;
		while(inBuffer.limit()-inBuffer.position()-startpos>=188 && !isOnePackageDone){
			if((0xff&inBuffer.get(inBuffer.position()+startpos))!=0x47){
				startpos ++;
				continue;
			}
			if(analyseTSHeader(inBuffer, startpos, pesBuffer)>0){
				isOnePackageDone = true;
			}
			startpos += 188;
		}
		inBuffer.position(inBuffer.position()+startpos);
		inBuffer.compact();
		return isOnePackageDone;
	}

	public static void main(String[] args) throws Exception {
		MpegTsParser parser = new MpegTsParser();
		ByteBuffer pesBuffer = ByteBuffer.allocate(128*1024);
		ByteBuffer tsBuffer = ByteBuffer.allocate(512*1024);
		DataOutputStream dataOutStream = new DataOutputStream(new FileOutputStream(new File("test001.pes")));
		DataInputStream dataInStream = new DataInputStream(new FileInputStream(new File("G:\\test001.ts")));
		while(dataInStream.available()>0){
			if(tsBuffer.remaining()>0){
				int readlen = dataInStream.read(tsBuffer.array(), tsBuffer.position(), tsBuffer.remaining());
				tsBuffer.position(tsBuffer.position()+readlen);
			}
			tsBuffer.flip();
			if(parser.exportTSToPes(tsBuffer, pesBuffer)){
				try {
					dataOutStream.write(pesBuffer.array(), 0, pesBuffer.position());
				} catch (IOException e) {
					e.printStackTrace();
				}
				pesBuffer.clear();
			}
		}
		dataInStream.close();
	}
}
