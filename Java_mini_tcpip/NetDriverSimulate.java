import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


public class NetDriverSimulate {
	
	//private Map<String, byte[]> macAddrs;
	private String pcapDeviceName;
	private Pointer handle;
	private boolean threadRunning = false;
	private Thread innerReceiveThread = null;
	private Thread innerSendThread = null;
	
	private final static int maxOutputBufferSize = (int)(0.25*1024*1024);
	private static ByteBuffer netifOutputBuffer = ByteBuffer.allocate(maxOutputBufferSize);
	private final static int maxInputBufferSize = (int)(0.25*1024*1024);
	private static ByteBuffer netifInputBuffer = ByteBuffer.allocate(maxInputBufferSize);
	
	private String generateMacFilterString(List<byte[]> macAddrs){
		StringBuilder strb = new StringBuilder(" and (");
		int count = 0;
		for(byte[] val : macAddrs){
			if(count==0){
				strb.append(String.format("ether dst %02X:%02X:%02X:%02X:%02X:%02X",
						val[0], val[1], val[2], val[3], val[4], val[5]));
			}else{
				strb.append(String.format(" or ether dst %02X:%02X:%02X:%02X:%02X:%02X",
						val[0], val[1], val[2], val[3], val[4], val[5]));
			}
			++ count;
		}
		strb.append(")");
		return strb.toString();
	}
	
	public NetDriverSimulate(String deviceName, List<byte[]> addrs){
		//macAddrs = addrs;
		pcapDeviceName = deviceName;
		NativeMapping.PcapErrbuf errbuf = new NativeMapping.PcapErrbuf();
		handle = NativeMapping.pcap_open_live(pcapDeviceName, 65536, 1, 1000, errbuf);
		if (handle == null) {
			System.out.println("Open device failed");
			System.exit(-1);
		}
		
		/*
		NativeMapping.bpf_program prog = new NativeMapping.bpf_program();
		String filterStr = String.format("ether dst %02X:%02X:%02X:%02X:%02X:%02X", 
				localMacAddr[0], localMacAddr[1], localMacAddr[2], localMacAddr[3], localMacAddr[4], localMacAddr[5]);
		rc = NativeMapping.pcap_compile(handle, prog, filterStr, 1, -1);
		NativeMapping.pcap_freecode(prog);
		*/

		NativeMapping.bpf_program prog = new NativeMapping.bpf_program();
		String filterStr = "(arp or tcp or udp or icmp)"+generateMacFilterString(addrs);
		int rc = NativeMapping.pcap_compile(handle, prog, filterStr, 1, -1);
		NativeMapping.pcap_freecode(prog);
		
		threadRunning = true;
		
		innerReceiveThread = new Thread(new Runnable(){

			@Override
			public void run() {
				int rc ;
				PointerByReference headerPP = new PointerByReference();
				PointerByReference dataPP = new PointerByReference();
				while (threadRunning) {
					rc = NativeMapping.pcap_next_ex(handle, headerPP, dataPP);
					switch (rc) {
					case 0:
						// timeout
						break;
					case 1:
						Pointer headerP = headerPP.getValue();
						Pointer dataP = dataPP.getValue();
						if (headerP == null || dataP == null) {
						} else {

							final int len = NativeMapping.pcap_pkthdr.getLen(headerP);
							final byte[] buff = dataP.getByteArray(0, NativeMapping.pcap_pkthdr.getCaplen(headerP));
							
							synchronized(netifInputBuffer){
								if(netifInputBuffer.position()+len+4<=netifInputBuffer.capacity()){
									netifInputBuffer.putInt(len);
									netifInputBuffer.put(buff, 0, len);
								}
							}
						}
						break;
					case -1:
						System.out.println("Error occurred in pcap_next_ex(): " + rc);
						System.exit(-1);
					case -2:
						System.out.println("end of file " + rc);
						System.exit(-1);
					default:
						System.out.println("Unexpected error occurred: " + rc);
						System.exit(-1);
					}
				}
				
			}});
		innerReceiveThread.start();
		
		
		innerSendThread = new Thread(new Runnable(){
			@Override
			public void run() {
				byte[] buff = new byte[2048];
				while(threadRunning){
					boolean hasData = true;
					synchronized(netifOutputBuffer){
						if(netifOutputBuffer.position()>0){
							netifOutputBuffer.flip();
							while(netifOutputBuffer.position()<netifOutputBuffer.limit()){
								int size = netifOutputBuffer.getInt();
								netifOutputBuffer.get(buff, 0, size);
								NativeMapping.pcap_sendpacket(handle, buff, size);
							}
							netifOutputBuffer.compact();
						}else{
							hasData = false;
						}
					}
					if(!hasData){
						try {
							Thread.sleep(3);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		});
		innerSendThread.start();
		
		
		
	}
	
	public void putDataToOutputBuffer(byte[] buff, int off, int len){
		synchronized(netifOutputBuffer){
			netifOutputBuffer.putInt(len);
			netifOutputBuffer.put(buff, off, len);
		}
	}
	
	public int getDataFromInputBuffer(byte[] buff, int off, int len){
		int size = 0;
		synchronized(netifInputBuffer){
			if(netifInputBuffer.position()>0){
				netifInputBuffer.flip();
				size = netifInputBuffer.getInt();
				netifInputBuffer.get(buff, off, size);
				netifInputBuffer.compact();
			}
		}
		return size;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
