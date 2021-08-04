
public class SocketStruct {
	public byte[] remoteAddr;
	public int    remotePort;
	public int    type; // udp 17 tcp 6
	public int    identifyId;
	public long   sequenceId;
	public long   acknowledgementId;
	public int    status; // 0 fin 1 send syn 2 recv syn 3 send syn ack 4 estable 5 send fin wait fin reply 
	// for input buffer
	private byte[] inBuffer;
	private int    inDataCurrSize;
	private int    inpos;
	// for output buffer
	private byte[] outBuffer;
	private int    outDataCurrSize;
	private int    outpos;
	private long   oprTime;
	
	public void initInputBuffer(int size){
		inBuffer = new byte[size];
	}
	
	public void initOutputBuffer(int size){
		outBuffer = new byte[size];
	}
	
	public int readInputData(byte[] buff, int off, int len){
		if(inBuffer!=null){
			if(type==6){
				if(inDataCurrSize>0){
					int cplen = inDataCurrSize>len?len:inDataCurrSize;
					System.arraycopy(inBuffer, 0, buff, off, cplen);
					if(inDataCurrSize-cplen>0){
						System.arraycopy(inBuffer, 0, inBuffer, 0, inDataCurrSize-cplen);
					}
					inDataCurrSize-=cplen;
					
					return cplen;
				}
				return 0;
			}else if(type==17){
				if(inDataCurrSize>0){
					int currDataSize = ((inBuffer[0]&0xff)<<8)+(inBuffer[0]&0xff);
					if(inpos==0){
						inpos = 2;
					}
					int cplen = currDataSize-inpos+2;
					if(cplen>len){
						cplen = len;
					}
					System.arraycopy(inBuffer, inpos, buff, off, cplen);
					inpos += cplen;
					if(inDataCurrSize-inpos+2>=currDataSize){
						if(inDataCurrSize-currDataSize-2>0){
							System.arraycopy(inBuffer, currDataSize+2, inBuffer, 0, inDataCurrSize-currDataSize-2);
						}
						inDataCurrSize -= currDataSize+2;
						inpos = 0;
					}
					
					return cplen;
				}
				return 0;
			}
		}
		return -1;
	}
	
	public int writeInputData(byte[] buff, int off, int len){
		if(inBuffer!=null){
			if(type==6){
				if(inBuffer.length-inDataCurrSize>0){
					int cplen = inBuffer.length-inDataCurrSize;
					if(cplen>len){
						cplen = len;
					}
					System.arraycopy(buff, off, inBuffer, inDataCurrSize, cplen);
					inDataCurrSize += cplen;
					return cplen;
				}
				return 0;
			}else if(type==17){
				if(inBuffer.length-inDataCurrSize>=len+2){
					inBuffer[inDataCurrSize] = (byte)((len>>8)&0xff);
					inBuffer[inDataCurrSize+1] = (byte)(len&0xff);
					System.arraycopy(buff, off, inBuffer, inDataCurrSize+2, len);
					inDataCurrSize += len+2;
					return len;
				}
				return 0;
			}
		}
		return -1;
	}
	
	public int readOutputData(byte[] buff, int off, int len){
		if(outBuffer!=null){
			if(type==6){
				if(outDataCurrSize>0){
					int cplen = outDataCurrSize>len?len:outDataCurrSize;
					System.arraycopy(outBuffer, 0, buff, off, cplen);
					if(outDataCurrSize-cplen>0){
						System.arraycopy(outBuffer, 0, outBuffer, 0, outDataCurrSize-cplen);
					}
					outDataCurrSize-=cplen;
					
					return cplen;
				}
				return 0;
			}else if(type==17){
				if(outDataCurrSize>0){
					int currDataSize = ((outBuffer[0]&0xff)<<8)+(outBuffer[0]&0xff);
					if(outpos==0){
						outpos = 2;
					}
					int cplen = currDataSize-outpos+2;
					if(cplen>len){
						cplen = len;
					}
					System.arraycopy(outBuffer, outpos, buff, off, cplen);
					outpos += cplen;
					if(outDataCurrSize-outpos+2>=currDataSize){
						if(outDataCurrSize-currDataSize-2>0){
							System.arraycopy(outBuffer, currDataSize+2, outBuffer, 0, outDataCurrSize-currDataSize-2);
						}
						outDataCurrSize -= currDataSize+2;
						outpos = 0;
					}
					
					return cplen;
				}
				return 0;
			}
		}
		return -1;
	}
	
	public int readTcpOutputData(byte[] buff, int off, int len){
		if(outBuffer!=null){
			if(type==6){
				if(outpos==0 && outDataCurrSize>0){
					outpos = outDataCurrSize>len?len:outDataCurrSize;
					System.arraycopy(outBuffer, 0, buff, off, outpos);
					if(outDataCurrSize-outpos>0){
						System.arraycopy(outBuffer, 0, outBuffer, 0, outDataCurrSize-outpos);
					}
					oprTime = System.currentTimeMillis();
					return outpos;
				}else if(outpos>0 && outDataCurrSize>0){
					if(System.currentTimeMillis()-oprTime>=1000){
						oprTime = System.currentTimeMillis();
						System.arraycopy(outBuffer, 0, buff, off, outpos);
						return outpos;
					}
				}
				return 0;
			}
		}
		return -1;
	}
	
	public int compactTcpOutputData(){
		if(outBuffer!=null){
			if(type==6){
				if(outpos>0 && outDataCurrSize>0){
					outDataCurrSize -= outpos;
					outpos = 0;
				}
				return 0;
			}
		}
		return -1;
	}
	
	public int getTcpOutputPos(){
		return outpos;
	}
	
	public int writeOutputData(byte[] buff, int off, int len){
		if(outBuffer!=null){
			if(type==6){
				if(outBuffer.length-outDataCurrSize>0){
					int cplen = outBuffer.length-outDataCurrSize;
					if(cplen>len){
						cplen = len;
					}
					System.arraycopy(buff, off, outBuffer, outDataCurrSize, cplen);
					outDataCurrSize += cplen;
					return cplen;
				}
				return 0;
			}else if(type==17){
				if(outBuffer.length-outDataCurrSize>=len+2){
					outBuffer[outDataCurrSize] = (byte)((len>>8)&0xff);
					outBuffer[outDataCurrSize+1] = (byte)(len&0xff);
					System.arraycopy(buff, off, outBuffer, outDataCurrSize+2, len);
					outDataCurrSize += len+2;
					return len;
				}
				return 0;
			}
		}
		return -1;
	}
}
