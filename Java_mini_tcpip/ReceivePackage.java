

import java.time.Instant;
import com.sun.jna.Pointer;



public class ReceivePackage implements NativeMapping.pcap_handler {
	
	public ReceivePackage(){
	}

	@Override
	public void got_packet(Pointer args, Pointer header, Pointer packet) {
	      final Instant ts = NativeMapping.buildTimestamp(header, 0);
	      final int len = NativeMapping.pcap_pkthdr.getLen(header);
	      final byte[] buff = packet.getByteArray(0, NativeMapping.pcap_pkthdr.getCaplen(header));

          if (buff[14]==0 && buff[15]==1 && buff[16]==8 && buff[17]==0 && buff[20]==0 && buff[21]==2) {
              String macAddrStr = String.format("%02X:%02X:%02X:%02X:%02X:%02X", buff[22],buff[23],buff[24],buff[25],buff[26],buff[27]);
              System.out.println("arp return macAddrStr "+macAddrStr);


            }
	}

}
