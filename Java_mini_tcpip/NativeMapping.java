import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class NativeMapping {
	
	public static final Inet4Address PCAP_NETMASK_UNKNOWN;

	static {
		try {
			PCAP_NETMASK_UNKNOWN = (Inet4Address) InetAddress.getByName("255.255.255.255");
		} catch (UnknownHostException e) {
			throw new AssertionError("never get here");
		}
	}
	  
	static final String PCAP_LIB_NAME = Platform.isWindows() ? "wpcap" : "pcap";
	static final File NPCAP_DIR = Paths.get(System.getenv("SystemRoot"), "System32", "Npcap").toFile();

	static {
		if (Platform.isWindows() && System.getProperty("jna.library.path") == null) {
			if (NPCAP_DIR.exists()) {
				NativeLibrary.addSearchPath("wpcap", NPCAP_DIR.getAbsolutePath());
			}
		}
	}

	static final Pointer ERRNO_P = Platform.isSolaris() ? NativeLibrary.getInstance(PCAP_LIB_NAME)
			.getGlobalVariableAddress("errno") : null;

	static {
		Native.register(NativeMapping.class, NativeLibrary.getInstance(PCAP_LIB_NAME));
	}

	// pcap_t *pcap_open_live(
	// const char *device, int snaplen, int promisc, int to_ms, char *errbuf
	// )
	static native Pointer pcap_open_live(String device, int snaplen, int promisc, int to_ms, PcapErrbuf errbuf);

	// int pcap_loop(pcap_t *p, int cnt, pcap_handler callback, u_char *user)
	static native int pcap_loop(Pointer p, int cnt, pcap_handler callback, Pointer user);

	static native int pcap_loop(Pointer p, int cnt, Function callback, Pointer user);

	// void pcap_breakloop(pcap_t *p)
	static native void pcap_breakloop(Pointer p);

	// u_char *pcap_next(pcap_t *p, struct pcap_pkthdr *h)
	static native Pointer pcap_next(Pointer p, pcap_pkthdr h);

	// int pcap_next_ex(pcap_t *p, struct pcap_pkthdr **h, const u_char **data)
	static native int pcap_next_ex(Pointer p, PointerByReference h, PointerByReference data);

	// int pcap_sendpacket(pcap_t *p, const u_char *buf, int size)
	static native int pcap_sendpacket(Pointer p, byte[] buf, int size);
	
	// int pcap_compile(
	//   pcap_t *p, struct bpf_program *fp, char *str,
	//   int optimize, bpf_u_int32 netmask
	// )
	static native int pcap_compile(Pointer p, bpf_program fp, String str, int optimize, int netmask);
	
	// int pcap_setfilter(pcap_t *p, struct bpf_program *fp)
	static native int pcap_setfilter(Pointer p, bpf_program fp);

	// void pcap_freecode(struct bpf_program *fp)
	static native void pcap_freecode(bpf_program fp);

	// void pcap_close(pcap_t *p)
	static native void pcap_close(Pointer p);

	static interface pcap_handler extends Callback {

		// void got_packet(
		// u_char *args, const struct pcap_pkthdr *header, const u_char *packet
		// );
		public void got_packet(Pointer args, Pointer header, Pointer packet);
	}

	public static class pcap_pkthdr extends Structure {

		public static final int TS_OFFSET;
		public static final int CAPLEN_OFFSET;
		public static final int LEN_OFFSET;

		public timeval ts; // struct timeval
		public int caplen; // bpf_u_int32
		public int len; // bpf_u_int32

		static {
			pcap_pkthdr ph = new pcap_pkthdr();
			TS_OFFSET = ph.fieldOffset("ts");
			CAPLEN_OFFSET = ph.fieldOffset("caplen");
			LEN_OFFSET = ph.fieldOffset("len");
		}

		public pcap_pkthdr() {
		}

		public pcap_pkthdr(Pointer p) {
			super(p);
			read();
		}

		public static class ByReference extends pcap_pkthdr implements Structure.ByReference {
		}

		@Override
		protected List<String> getFieldOrder() {
			List<String> list = new ArrayList<String>();
			list.add("ts");
			list.add("caplen");
			list.add("len");
			return list;
		}

		static NativeLong getTvSec(Pointer p) {
			return p.getNativeLong(TS_OFFSET + timeval.TV_SEC_OFFSET);
		}

		static NativeLong getTvUsec(Pointer p) {
			return p.getNativeLong(TS_OFFSET + timeval.TV_USEC_OFFSET);
		}

		static int getCaplen(Pointer p) {
			return p.getInt(CAPLEN_OFFSET);
		}

		static int getLen(Pointer p) {
			return p.getInt(LEN_OFFSET);
		}
	}

	public static class timeval extends Structure {

		public static final int TV_SEC_OFFSET;
		public static final int TV_USEC_OFFSET;

		public NativeLong tv_sec; // long
		public NativeLong tv_usec; // long

		static {
			timeval tv = new timeval();
			TV_SEC_OFFSET = tv.fieldOffset("tv_sec");
			TV_USEC_OFFSET = tv.fieldOffset("tv_usec");
		}

		public timeval() {
		}

		@Override
		protected List<String> getFieldOrder() {
			List<String> list = new ArrayList<String>();
			list.add("tv_sec");
			list.add("tv_usec");
			return list;
		}
	}

	public static Instant buildTimestamp(Pointer header, int timestampPrecision) {
		long epochSecond = pcap_pkthdr.getTvSec(header).longValue();
		switch (timestampPrecision) {
		case 0:
			return Instant.ofEpochSecond(epochSecond, pcap_pkthdr.getTvUsec(header).intValue() * 1000);
		case 1:
			return Instant.ofEpochSecond(epochSecond, pcap_pkthdr.getTvUsec(header).intValue());
		default:
			throw new AssertionError("Never get here.");
		}
	}

	public static class PcapErrbuf extends Structure {

		public byte[] buf = new byte[PCAP_ERRBUF_SIZE()];

		public PcapErrbuf() {
		}

		private static int PCAP_ERRBUF_SIZE() {
			return 256;
		}

		public int length() {
			return toString().length();
		}

		@Override
		protected List<String> getFieldOrder() {
			List<String> list = new ArrayList<String>();
			list.add("buf");
			return list;
		}

		@Override
		public String toString() {
			return Native.toString(buf);
		}
	}

	public static class bpf_insn extends Structure {

		public short code; // u_short
		public byte jt; // u_char
		public byte jf; // u_char
		public int k; // bpf_u_int32

		public bpf_insn() {
			setAutoSynch(false);
		}

		public static class ByReference extends bpf_insn implements Structure.ByReference {
		}

		@Override
		protected List<String> getFieldOrder() {
			List<String> list = new ArrayList<String>();
			list.add("code");
			list.add("jt");
			list.add("jf");
			list.add("k");
			return list;
		}
	};
	  
	public static class bpf_program extends Structure {

		public int bf_len; // u_int
		public bpf_insn.ByReference bf_insns; // struct bpf_insn *

		public bpf_program() {
			setAutoSynch(false);
		}

		@Override
		protected List<String> getFieldOrder() {
			List<String> list = new ArrayList<String>();
			list.add("bf_len");
			list.add("bf_insns");
			return list;
		}
	}
}
