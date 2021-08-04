import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.lang.model.type.UnknownTypeException;


public class ByteUtils {
	
	public static int calcBuff16BitsSum(byte[] buff, int off, int len){
		int checkSum = 0;
		for(int i=0; i<len;++i){
			checkSum += (i%2==0)?((buff[i+off]&0xff)<<8):(buff[i+off]&0xff);
		}
		return checkSum;
	}
	
	public static int calcBuffChecksum(byte[] buff, int off, int len){
		int checkSum = calcBuff16BitsSum(buff, off, len);
		return  ~(((checkSum>>16)&0xffff)+(checkSum&0xffff));
	}
	

	
	public static int indexOf(final byte[] src, final byte[] dest, final int offset, final int endpos){
		if(src.length>=dest.length){
			byte[] tempArray = new byte[dest.length];
			int tempEndpos = endpos<0?(src.length-dest.length+1):Math.min(src.length-dest.length+1, endpos);
			for(int i=offset; i<tempEndpos; ++i){
				if(src[i]==dest[0]){
					System.arraycopy(src, i, tempArray, 0, dest.length);
					if(Arrays.equals(dest, tempArray)){
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	public static boolean equals(byte[] src, byte[] dest){
		return Arrays.equals(dest, src);
	}
	
	public static boolean equals(final byte[] src, final int srcoff, final byte[] dest, final int destoff, final int len){
		for(int i=0; i<len; ++i){
			if(src[srcoff+i]!=dest[destoff+i]){
				return false;
			}
		}
		return true;
	}
	
	public static int compare(final byte[] src, final byte[] dest){
		int rval = 0;
		int minlen = Math.min(src.length, dest.length);
		for(int i=0; i<minlen; ++i){
			if(src[i]==dest[i]){
				
			}else{
				rval = (src[i]&0xff)-(dest[i]&0xff);
				break;
			}
		}
		if(rval==0){
			rval = src.length-dest.length;
		}
		return rval;
	}
	
	public static int compare(final byte[] src, final int srcoff, final byte[] dest, final int srclen, final int destoff, final int destlen){
		int rval = 0;
		int minlen = Math.min(srclen, destlen);
		for(int i=0; i<minlen; ++i){
			if(src[srcoff+i]==dest[destoff+i]){
				
			}else{
				rval = (src[srcoff+i]&0xff)-(dest[destoff+i]&0xff);
				break;
			}
		}
		if(rval==0){
			rval = srclen-destlen;
		}
		return rval;
	}
	
	public static byte[] replaceFirst(final byte[] raw, final byte[] src, final byte[] dest){
		int pos = indexOf( raw, src, 0, -1);
		if(pos>-1){
			byte[] tempArray = new byte[raw.length-src.length+dest.length];
			if(pos==0){
				System.arraycopy(dest, 0, tempArray, 0, dest.length);
				System.arraycopy(raw, src.length, tempArray, dest.length, raw.length-src.length);
			}else if(pos+src.length==raw.length){
				System.arraycopy(raw, 0, tempArray, 0, raw.length-src.length);
				System.arraycopy(dest, 0, tempArray, raw.length-src.length, dest.length);
			}else{
				System.arraycopy(raw, 0, tempArray, 0, pos);
				System.arraycopy(dest, 0, tempArray, pos, dest.length);
				System.arraycopy(raw, pos+src.length, tempArray, pos+dest.length, raw.length-pos-src.length);
			}
			return tempArray;
		}
		return raw;
	}
	
	public static byte[] replaceAll(final byte[] raw,final  byte[] src, final byte[] dest){
		int pos = indexOf( raw, src, 0, -1);
		if(pos<0){
			return raw;
		}
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		int prevPos = 0;
		while(true){
			if(pos<0){
				if(raw.length-prevPos>0){
					arrayOutputStream.write(raw,prevPos, raw.length-prevPos);
				}
				break;
			}else if(pos==prevPos){
				arrayOutputStream.write(dest,0, dest.length);
				prevPos = pos+src.length;
			}else{
				arrayOutputStream.write(raw,prevPos, pos-prevPos);
				prevPos = pos+src.length;
				arrayOutputStream.write(dest,0, dest.length);
			}
			pos = indexOf( raw, src, prevPos, -1);
		}
		return arrayOutputStream.toByteArray();
	}
	
	public static void toUpper(byte[] text){
		final byte[] upperChrs = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
		for(int i=0; i<text.length; ++i){
			if('a'<=text[i] && text[i]<='z'){
				text[i] = upperChrs[text[i]-'a'];
			}
		}
	}
	
	public static void toLowwer(byte[] text){
		final byte[] lowwerChrs = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
		for(int i=0; i<text.length; ++i){
			if('A'<=text[i] && text[i]<='Z'){
				text[i] = lowwerChrs[text[i]-'A'];
			}
		}
	}
	
	public static byte[] objToHexByteString(Object obj){
		final byte[] hexNumChrArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		return null;
	}
	
	public static int intToByteString(long val, byte[] chrArray, final int base, final boolean islow, final boolean withPositive ){
		final byte[] upperHexNumChrArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		final byte[] lowwerHexNumChrArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
		byte[] hexNumChrArray = upperHexNumChrArray;
		if(islow){
			hexNumChrArray = lowwerHexNumChrArray;
		}
		int chrpos = 0;
		if(val!=0){
			if(val<0){
				chrArray[chrpos++] = '-';
				val = -val;
			}
			for(int i=18; i>-1; --i){
				long tempVal = (long)Math.pow(base, i);
				int numVal =  (int)(val / tempVal);
				if(chrpos==0 && numVal==0){
					continue;
				}
				if(chrpos==0 && numVal>0){
					chrArray[chrpos++] = hexNumChrArray[numVal]; 
				}else{
					chrArray[chrpos++] = hexNumChrArray[numVal];
				}
				val -= tempVal*numVal;
			}
		}else{
			if(withPositive){
				chrArray[chrpos++] = '-';
			}
			chrArray[chrpos++] = hexNumChrArray[0];
		}
		return chrpos;
	}
	
	public static int doubleToByteString(double val, byte[] chrArray, int cut){
		final byte[] numChrArray = {'0','1','2','3','4','5','6','7','8','9'};
		long longVal = (long)val;
		boolean isPositive = val<0;
		int chrpos = intToByteString(longVal, chrArray, 10, false, isPositive);
		val -= longVal;
		if(val!=0){
			chrArray[chrpos++] = '.';
			if(val<0){
				val = -val;
			}
			while(val!=0 && chrpos<chrArray.length && cut>0){
				val *= 10;
				int numVal =  (int)(val);
				val -= numVal;
				chrArray[chrpos++] = numChrArray[numVal];
				--cut;
			}
		}
		return chrpos;
	}
	
	public static int sprintf(byte[] dest, final byte[] fmt, final Object... args) throws Exception{
		byte[] numbuff = new byte[20];
		int total=0;
		int argsPos = -1;
		for(int i=0; i<fmt.length-1 && total<dest.length; ++i){
			if((fmt[i]&0xff)=='%'){
				++i;
				switch(fmt[i]&0xff){
				case 's':
					++argsPos;
					if(argsPos>=args.length){
						throw new ArrayIndexOutOfBoundsException();
					}
					if(args[argsPos] instanceof byte[]){
						byte[] tempArray = (byte[])args[argsPos];
						if(tempArray.length+total>dest.length){
							throw new ArrayIndexOutOfBoundsException();
						}
						System.arraycopy(tempArray, 0, dest, total, tempArray.length);
						total += tempArray.length;
					}else{
						throw new UnknownTypeException(null, args[argsPos]);
					}
					break;
				case 'c':
					++argsPos;
					if(argsPos>=args.length){
						throw new ArrayIndexOutOfBoundsException();
					}
					if(args[argsPos] instanceof Byte || args[argsPos] instanceof Character || args[argsPos] instanceof Short || args[argsPos] instanceof Integer || args[argsPos] instanceof Long){
						byte tempVal = (byte)((byte)args[argsPos]&0xff);
						if(1+total>dest.length){
							throw new ArrayIndexOutOfBoundsException();
						}
						dest[total++] = tempVal;
					}else{
						throw new UnknownTypeException(null, args[argsPos]);
					}
					break;
				case 'x':
					++argsPos;
					{
						int len = intToByteString((Long)args[argsPos], numbuff, 16, false, false);
						if(argsPos>=len){
							throw new ArrayIndexOutOfBoundsException();
						}
						System.arraycopy(numbuff, 0, dest, total, len);
						total += len;
					}
					break;
				case 'X':
					++argsPos;
					{
						int len = intToByteString((long)args[argsPos], numbuff, 16, true, false);
						if(argsPos>=len){
							throw new ArrayIndexOutOfBoundsException();
						}
						System.arraycopy(numbuff, 0, dest, total, len);
						total += len;
					}
					break;
				case 'd':
					++argsPos;
					{
						long val = 0;
						if(args[argsPos] instanceof Long){
							val = (long)args[argsPos];
						}
						if(args[argsPos] instanceof Integer){
							val = (int)args[argsPos];
						}
						if(args[argsPos] instanceof Short){
							val = (short)args[argsPos];
						}
						if(args[argsPos] instanceof Byte){
							val = (byte)args[argsPos];
						}
						int len = intToByteString(val, numbuff, 10, false, false);
						if(argsPos>=len){
							throw new ArrayIndexOutOfBoundsException();
						}
						System.arraycopy(numbuff, 0, dest, total, len);
						total += len;
					}
					break;
				case 'f':
					++argsPos;
					{
						int len = doubleToByteString((double)args[argsPos], numbuff, dest.length-total);
						if(argsPos>=len){
							throw new ArrayIndexOutOfBoundsException();
						}
						System.arraycopy(numbuff, 0, dest, total, len);
						total += len;
					}
					break;
				case '%':
					dest[total++] = (byte)'%';
					break;
				default:
					break;
				}
			}else{
				if(1+total>dest.length){
					throw new ArrayIndexOutOfBoundsException();
				}
				dest[total++] = fmt[i];
			}
		}
		return total;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
