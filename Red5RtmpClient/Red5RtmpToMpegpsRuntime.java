
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.utils.ObjectMap;
import org.red5.server.event.IEvent;
import org.red5.server.event.IEventDispatcher;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.service.IPendingServiceCall;
import org.red5.server.service.IPendingServiceCallback;
import org.red5.server.stream.IStreamData;

import MpegParser;

/*
aligned(8) class AVCDecoderConfigurationRecord {
  unsigned int(8) configurationVersion = 1;
  unsigned int(8) AVCProfileIndication;
  unsigned int(8) profile_compatibility;
  unsigned int(8) AVCLevelIndication;
  bit(6) reserved = 111111b;
  unsigned int(2) lengthSizeMinusOne;
  bit(3) reserved = 111b;
  unsigned int(5) numOfSequenceParameterSets;// num of SPS 
  for (i=0; i< numOfSequenceParameterSets; i++) {
    unsigned int(16) sequenceParameterSetLength ;// SPS bytes length
    bit(8*sequenceParameterSetLength) sequenceParameterSetNALUnit;
  }
  unsigned int(8) numOfPictureParameterSets;// num of PPS 
  for (i=0; i< numOfPictureParameterSets; i++) {
    unsigned int(16) pictureParameterSetLength;// PPS 
    bit(8*pictureParameterSetLength) pictureParameterSetNALUnit; // PPS bit 
  }
  if( profile_idc == 100 || profile_idc == 110 ||
  profile_idc == 122 || profile_idc == 144 )
  {
    bit(6) reserved = 111111b;
    unsigned int(2) chroma_format;
    bit(5) reserved = 11111b;
    unsigned int(3) bit_depth_luma_minus8;
    bit(5) reserved = 11111b;
    unsigned int(3) bit_depth_chroma_minus8;
    unsigned int(8) numOfSequenceParameterSetExt;
    for (i=0; i< numOfSequenceParameterSetExt; i++) {
      unsigned int(16) sequenceParameterSetExtLength;
      bit(8*sequenceParameterSetExtLength) sequenceParameterSetExtNALUnit;
    }
  }
}

67 SPS data
68 PPS data
65 Key frame
61 B frame

 */

public class Red5RtmpToMpegpsRuntime implements IEventDispatcher, IPendingServiceCallback {
	private static final int STOPPED = 0;

	private static final int CONNECTING = 1;

	private static final int STREAM_CREATING = 2;
	private static final int PLAYING = 3;
	private String host;
	private int port;
	private String app;
	private String fileName;
	private String saveAsFileName;
	private int duration = 10000;
	private int start = 0;
	private int playLen = 10;

	private int streamId;
	private int state;
	private RTMPClient rtmpClient;
	
	private byte[] SPSData = null;
	private byte[] PPSData = null;
	
	private OutputStream dataOut = null;
	
	private int psTickCount = 0x00650001;
	
	private static final byte[] h264Prefix = {0, 0, 0, 1};
	
	public Red5RtmpToMpegpsRuntime(OutputStream dataOut){
		state = 0;
		this.dataOut = dataOut;
	}

	@Override
	public void resultReceived(IPendingServiceCall call) {
		System.out.println("resultReceived:> " + call.getServiceMethodName());
		if ("connect".equals(call.getServiceMethodName())) {
			this.state = 2;
			this.rtmpClient.createStream(this);
		} else if ("createStream".equals(call.getServiceMethodName())) {
			this.state = 3;
			Object result = call.getResult();
			if (result instanceof Integer) {
				Integer streamIdInt = (Integer) result;
				this.streamId = streamIdInt.intValue();
				System.out.println(String.format("Playing state %d", Integer.valueOf(this.state)));
				this.rtmpClient.play(streamIdInt.intValue(), this.fileName, this.start, this.duration);

				Ping ping = new Ping();
				ping.setEventType((short) 3);
				ping.setValue2(this.streamId);
				ping.setValue3(2000);
				this.rtmpClient.getConnManager().getConnection().ping(ping);
				this.rtmpClient.setServiceProvider(this);
			} else {
				this.rtmpClient.disconnect();
				this.state = 0;
			}
		}

	}
	
	private void writeFrameData(long dts, byte[] data, int off, int len){
		ByteBuffer h264Buffer = null;
		ByteBuffer psBuffer = ByteBuffer.allocate(len+2048);
		try {
			if(data[off]==0x65){
				h264Buffer = ByteBuffer.allocate(len+SPSData.length+PPSData.length+12);
				h264Buffer.put(h264Prefix);
				h264Buffer.put(SPSData);
				h264Buffer.put(h264Prefix);
				h264Buffer.put(PPSData);
			}else{
				h264Buffer = ByteBuffer.allocate(len+4);
			}
			h264Buffer.put(h264Prefix);
			h264Buffer.put(data, off, len);
			h264Buffer.flip();
			

			MpegParser.packageOneFrame(h264Buffer, psBuffer, dts, psTickCount++);
			byte[] tempBuffer = new byte[psBuffer.position()];
			System.arraycopy(psBuffer.array(), 0, tempBuffer, 0, tempBuffer.length);
			dataOut.write(tempBuffer);
			dataOut.flush();
			h264Buffer.clear();
			h264Buffer = null;
			psBuffer.clear();
			psBuffer = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void dispatchEvent(IEvent event) {
		IRTMPEvent rtmpEvent = (IRTMPEvent)event;

		if (!(rtmpEvent instanceof IStreamData)) {
			System.out.println("skipping non stream data");
			return;
		}
		if (rtmpEvent.getHeader().getSize() == 0) {
			System.out.println("skipping event where size == 0");

			return;
		}
		//rtmpEvent.getDataType();
		if (rtmpEvent instanceof org.red5.server.net.rtmp.event.VideoData) {
			//tmpEvent.getTimestamp();
			byte[] data = new byte[((IStreamData)rtmpEvent).getData().limit()];
			((IStreamData)rtmpEvent).getData().get(data);
			// byte[0] bit 7-4 frame type 1 key frame 2 inter frame
			// byte[0] bit 3-0 codec id  7 AVC
			// byte[1] AVCPacketType 0 sequene header 1 AVC NALU 
			// byte[2-4] ComposeTime
			int frametype = (data[0]&0xf0)>>4;
			int datatype = data[0]&0x0f;
			if(frametype<3 && datatype==7){
				
				if(data[1]==0){
					int pos = 5;
					pos += 5;
					int numOfSPS = data[pos]&0x1f;
					pos ++;
					for(int i=0; i<numOfSPS; ++i){
						int len = (data[pos]&0xff)<<8 | (data[pos+1]&0xff);
						pos+=2;
						//writeFrameData(data, pos, len);
						SPSData = new byte[len];
						System.arraycopy(data, pos, SPSData, 0, len);
						pos+=len;
					}
					int numOfPPS = data[pos]&0xff;
					pos ++;
					for(int i=0; i<numOfPPS; ++i){
						int len = (data[pos]&0xff)<<8 | (data[pos+1]&0xff);
						pos+=2;
						//writeFrameData(data, pos, len);
						PPSData = new byte[len];
						System.arraycopy(data, pos, PPSData, 0, len);
						pos+=len;
					}
				}else{
					writeFrameData(rtmpEvent.getTimestamp(), data, 9, data.length-9);
				}
				
				//writeFrameData(data, 0, data.length);
			}
		} else if (rtmpEvent instanceof org.red5.server.net.rtmp.event.AudioData) {
			//rtmpEvent.getTimestamp();
		}

		if (rtmpEvent.getTimestamp() / 1000 > this.playLen) {
			System.out.println(String.format("play progress: %d seconds", Integer.valueOf(this.playLen)));
			this.playLen += 10;
		}
	}

	public void onStatus(Object obj) {
		ObjectMap map = (ObjectMap)obj;
		String code = (String)map.get("code");
		String description = (String)map.get("description");
		String details = (String)map.get("details");

		if ("NetStream.Play.Reset".equals(code)) {
			System.out.println(String.format("%s: %s", code, description ));
		} else if ("NetStream.Play.Start".equals(code)) {
			System.out.println("playing video by name: " + this.fileName);
			System.out.println(String.format("%s: %s", code, description ));
		} else if ("NetStream.Play.Stop".equals(code)) {
			this.state = 0;
			System.out.println(String.format("%s: %s", code, description ));
			System.out.println("Recording Complete");
			this.rtmpClient.disconnect();
			stop();
		} else if ("NetStream.Play.StreamNotFound".equals(code)) {
			this.state = 0;
			System.out.println(String.format("File %s Not found", details ));
			System.out.println(String.format("%s for %s", code, details ));
			this.rtmpClient.disconnect();
			stop();
		}
	}
	
	public void setHost(String value) {
		this.host = value;
	}

	public void setPort(int value) {
		this.port = value;
	}

	public void setApp(String value) {
		this.app = value;
	}

	public void setDuration(int value) {
		this.duration = value;
	}

	public void setStartTime(int value) {
		this.start = value;
	}

	public int getState() {
		return this.state;
	}

	public void start(String playFileName) {
		this.fileName = playFileName;
		this.rtmpClient = new RTMPClient();
		this.rtmpClient.setStreamEventDispatcher(this);
		this.state = 1;
		Map<String, Object> defParams = this.rtmpClient.makeDefaultConnectionParams(this.host, this.port, this.app);
	  
		this.rtmpClient.connect(this.host, this.port, defParams, this, null);
	}

	public boolean playRtmp(String rtmpUrl) {
		Pattern p1 = Pattern.compile("[rR][tT][mM][pP]:\\/\\/([z-zA-Z0-9_\\-\\.]+)(:\\d+)?\\/+([\\w\\d_]+)\\/+([\\w\\d_]+)");
		Matcher m = p1.matcher(rtmpUrl);
		if(m!=null && m.find()){
			String host = m.group(1);
			String port = m.group(2)!=null?m.group(2):"1935";
			String app = m.group(3);
			String meidaName = m.group(4);
			
			setHost(host);
			setPort(Integer.parseInt(port.substring(1)));
			setApp(app);

			start(meidaName);
			return true;
		}
		return false;
	}

	public void stop() {
		/*
		if(dataOut!=null){
			try {
				dataOut.close();
			} catch (IOException e) {
			}
			dataOut = null;
		}
		*/
	}

	public void disConnect(){
		rtmpClient.disconnect();
	}
	
	public static void main(String[] args) {
		DataOutputStream dataOut = null;
		try {
			dataOut = new DataOutputStream(new FileOutputStream(new File("testH264Record01.h264")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Failed to open file");
			System.exit(-1);
		}
		Red5RtmpTpMpegpsRuntime client = new Red5RtmpTpMpegpsRuntime(dataOut);
		
		//client.setHost("127.0.0.1");
		client.setHost("192.168.11.12");
		client.setPort(1935);
		client.setApp("live");

		client.start("Test001");
		
		//client.playRtmp("rtmp://127.0.0.1:1935/live/Test001");
	}

}
