package com.iteye.weimingtom.chunktrans;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ChunkTrans {
	public final static boolean DEBUG = false;
	public int position = 0;
	
	private void readbuf(BufferedInputStream fin, byte[] bytes, int len) throws IOException {
		fin.read(bytes, 0, len);
		this.position += len;
//		if (this.position == 420) {
//			System.out.println(">>position=" + position + "<<");
//		}
		if (DEBUG) {
			System.out.println(">>position=" + position + "<<");
		}
	}
	
	private void tab(StringBuffer ostring, int n) {
		for (int i = 0; i < n; i++) {
			ostring.append("\t");
		}
	}
	
	private void readbyte(StringBuffer ostring, 
		BufferedInputStream fin, String name, int level) throws IOException {
		byte[] bytes = new byte[1];
		readbuf(fin, bytes, 1);
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":")
			.append(Integer.toString(bytes[0] & 0xff))
			.append(",").append("\n");
	}
	
	private void readmultibytes(StringBuffer ostring, 
		BufferedInputStream fin, String name, int length, int level) throws IOException {
		byte[] bytes = new byte[length];
		readbuf(fin, bytes, length);
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":").append("\"");
		for (int i = 0; i < length; i++) {
			if (Character.isSpaceChar((char)bytes[i]) || 
				(!Character.isDigit(bytes[i]) && 
				!Character.isLetter((char)bytes[i]))) {
				ostring.append("\\x").append(String.format("%02X", (bytes[i] & 0xff)));
			} else {
				ostring.append((char)bytes[i]);
			}
		}
		ostring.append("\"").append(",").append("\n");
	}
	
	private void readstring(StringBuffer ostring, 
		BufferedInputStream fin, String name, int level) throws IOException {
		byte[] bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		int length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		bytes = new byte[length];
		readbuf(fin, bytes, length);
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":").append("\"");
		for (int i = 0; i < length; i++) {
			if (Character.isSpaceChar((char)bytes[i]) || 
				(!Character.isDigit(bytes[i]) && 
				!Character.isLetter((char)bytes[i]))) {
				ostring.append("\\x").append(String.format("%02X", (bytes[i] & 0xff)));
			} else if (bytes[i] == '"') {
				ostring.append("\\\"");
			} else {
				ostring.append((char)bytes[i]);
			}
		}
		ostring.append("\"").append(",").append("\n");
	}
	
	private void readint32(StringBuffer ostring, 
		BufferedInputStream fin, String name, int level) throws IOException {
		byte[] bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		int length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");
	}
	
	//http://blog.csdn.net/deng0zhaotai/article/details/51830947
	private void readnumber(StringBuffer ostring, 
		BufferedInputStream fin, String name, int level) throws IOException {
		byte[] bytes = new byte[8];
		readbuf(fin, bytes, 8);
		double length = 0;
		long value = 0;
		value |= ((long) (bytes[0] & 0xff)) << (8 * 0); 
		value |= ((long) (bytes[1] & 0xff)) << (8 * 1); 
		value |= ((long) (bytes[2] & 0xff)) << (8 * 2); 
		value |= ((long) (bytes[3] & 0xff)) << (8 * 3); 
		value |= ((long) (bytes[4] & 0xff)) << (8 * 4); 
		value |= ((long) (bytes[5] & 0xff)) << (8 * 5); 
		value |= ((long) (bytes[6] & 0xff)) << (8 * 6); 
		value |= ((long) (bytes[7] & 0xff)) << (8 * 7); 
		length = Double.longBitsToDouble(value);  
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":")
			.append(Double.toString(length))
			.append(",").append("\n");
	}
	
	private void readcode(StringBuffer ostring, 
		BufferedInputStream fin, String name, int level) throws IOException {
		byte[] bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		int length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		bytes = new byte[length * 4];
		readbuf(fin, bytes, 4 * length);
		
		tab(ostring, level);
		ostring.append("\"").append(name).append("\"").append(":").append("\n");
		
		tab(ostring, level);
		ostring.append("[").append("\n");
		for (int i = 0; i < length; i++) {
			int code = 0;
			code |= (bytes[4 * i + 0] & 0xff);
			code |= (bytes[4 * i + 1] & 0xff) << 8;
			code |= (bytes[4 * i + 2] & 0xff) << 16;
			code |= (bytes[4 * i + 3] & 0xff) << 24;
			tab(ostring, level + 1);
			ostring.append("0x")
				.append(String.format("%08X", code))
				.append(",")
				.append("\n");
		}
		tab(ostring, level);
		ostring.append("]").append(",").append("\n");
	}
	
	private void readheader(StringBuffer ostring, 
		BufferedInputStream fin, int level) throws IOException {
		readmultibytes(ostring, fin, "header_signature", 4, level);
		readbyte(ostring, fin, "version", level);
		readbyte(ostring, fin, "format", level);
		readbyte(ostring, fin, "endianness", level);
		readbyte(ostring, fin, "size_of_int", level);
		readbyte(ostring, fin, "size_of_size_t", level);
		readbyte(ostring, fin, "size_of_Instruction", level);
		readbyte(ostring, fin, "size_of_number", level);
		readbyte(ostring, fin, "integral", level);
	}
	
	private void readdebug(StringBuffer ostring, 
		BufferedInputStream fin, int level) throws IOException {
		byte[] bytes = new byte[4]; 
		readbuf(fin, bytes,  4);
		int length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		
		tab(ostring, level);
		ostring.append("\"").append("sizelineinfo").append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");

		bytes = new byte[4 * length];
		readbuf(fin, bytes, 4 * length);

		tab(ostring, level);
		ostring.append("\"").append("lineinfo").append("\"").append(":").append("\n");
		
		tab(ostring, level);
		ostring.append("[").append("\n");
		
		//lineinfo
		for (int i = 0; i < length; ++i) {
			tab(ostring, level + 1);
			int code = 0;
			code |= (bytes[4 * i + 0] & 0xff);
			code |= (bytes[4 * i + 1] & 0xff) << 8;
			code |= (bytes[4 * i + 2] & 0xff) << 16;
			code |= (bytes[4 * i + 3] & 0xff) << 24;
			ostring.append("0x")
				.append(String.format("%08X", code))
				.append(",")
				.append("\n");
		}
		
		tab(ostring, level);
		ostring.append("]").append(",").append("\n");

		//sizelocvars
		bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;

		tab(ostring, level);
		ostring.append("\"").append("sizelocvars").append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");

		tab(ostring, level);
		ostring.append("\"").append("locvars").append("\"").append(":").append("\n");
		
		tab(ostring, level);
		ostring.append("[").append("\n");
		
		for (int j = 0; j < length; j++) {
			//readstring(fin, "varname");
			byte[] bytes2 = new byte[4]; 
			readbuf(fin, bytes2, 4);
			int length2 = 0;
			length2 |= (bytes2[0] & 0xff);
			length2 |= (bytes2[1] & 0xff) << 8;
			length2 |= (bytes2[2] & 0xff) << 16;
			length2 |= (bytes2[3] & 0xff) << 24;
			bytes2 = new byte[length2];
			readbuf(fin, bytes2, length2);

			tab(ostring, level + 1);
			ostring.append("{"); 
			ostring.append("\"").append("varname").append("\"").append(":").append("\"");
			for (int i = 0; i < length2; i++) {
				if (Character.isSpaceChar((char)bytes2[i]) || 
					(!Character.isDigit(bytes2[i]) && 
					!Character.isLetter((char)bytes2[i]))) {
					ostring.append("\\x").append(String.format("%02X", (bytes2[i] & 0xff)));
				} else if (bytes2[i] == '"') {
					ostring.append("\\\"");
				} else {
					ostring.append((char)bytes2[i]);
				}
			}
			ostring.append("\"").append(", ");
			bytes2 = new byte[4]; 
			readbuf(fin, bytes2, 4);
			length2 = 0;
			length2 |= (bytes2[0] & 0xff);
			length2 |= (bytes2[1] & 0xff) << 8;
			length2 |= (bytes2[2] & 0xff) << 16;
			length2 |= (bytes2[3] & 0xff) << 24;
			ostring.append("\"").append("startpc").append("\"").append(":").append(Integer.toString(length2)).append(", "); 
			bytes2 = new byte[4]; 
			readbuf(fin, bytes2, 4);
			length2 = 0;
			length2 |= (bytes2[0] & 0xff);
			length2 |= (bytes2[1] & 0xff) << 8;
			length2 |= (bytes2[2] & 0xff) << 16;
			length2 |= (bytes2[3] & 0xff) << 24;
			ostring.append("\"").append("endpc").append("\"").append(":").append(Integer.toString(length2)).append(", "); 
			ostring.append("},").append("\n");
			//readint32(fin, "startpc");
			//readint32(fin, "endpc");
		}
		
		tab(ostring, level);
		ostring.append("]").append(",").append("\n");
		
		//upvalues
		bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;

		tab(ostring, level);
		ostring.append("\"").append("sizeupvalues").append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");
		
		tab(ostring, level);
		ostring.append("\"").append("upvalues").append("\"").append(":").append("\n");
		
		tab(ostring, level);
		ostring.append("[").append("\n");
		
		for (int k = 0; k < length; k++) {
			byte[] bytes2 = new byte[4]; 
			readbuf(fin, bytes2, 4);
			int length2 = 0;
			length2 |= (bytes2[0] & 0xff);
			length2 |= (bytes2[1] & 0xff) << 8;
			length2 |= (bytes2[2] & 0xff) << 16;
			length2 |= (bytes2[3] & 0xff) << 24;
			bytes2 = new byte[length2];
			readbuf(fin, bytes2, length2);
			
			tab(ostring, level + 1);
			ostring.append("\"");
			
			for (int i = 0; i < length2; i++) {
				if (Character.isSpaceChar((char)bytes2[i]) || 
					(!Character.isDigit(bytes2[i]) && 
					!Character.isLetter((char)bytes2[i]))) {
					ostring.append("\\x").append(String.format("%02X", (bytes2[i] & 0xff)));
				} else if (bytes2[i] == '"') {
					ostring.append("\\\"");
				} else {
					ostring.append((char)bytes2[i]);
				}
			}
			ostring.append("\"").append(",").append("\n");
		}
		
		tab(ostring, level);
		ostring.append("]").append(",").append("\n");
	}
	
	private void readfunction(StringBuffer ostring, 
		BufferedInputStream fin, int level) throws IOException {
		readstring(ostring, fin, "source_name", level);
		readint32(ostring, fin, "line_defined", level);
		readint32(ostring, fin, "last_line_defined", level);
		readbyte(ostring, fin, "nups", level);
		readbyte(ostring, fin, "numparams", level);
		readbyte(ostring, fin, "is_vararg", level);
		readbyte(ostring, fin, "maxstacksize", level);

		readcode(ostring, fin, "code", level);
		readconstant(ostring, fin, level);
		readdebug(ostring, fin, level);
	}
	
	private void readconstant(StringBuffer ostring, 
		BufferedInputStream fin, int level) throws IOException {
		byte[] bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		int length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		
		tab(ostring, level);
		ostring.append("\"").append("sizek").append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");

		tab(ostring, level);
		ostring.append("\"").append("constant").append("\"").append(":").append("\n");

		tab(ostring, level);
		ostring.append("[").append("\n");

		for (int i = 0; i < length; i++) {
			byte[] type = new byte[1];
			readbuf(fin, type, 1);
			//ostring << "type=>" << (int)type << endl;
			switch (type[0]) {
			case 0: //LUA_TNIL:
				{
					tab(ostring, level + 1);
					ostring.append("{").append("\"").append("type").append("\"").append(":").append(Integer.toString((int)type[0])).append(", ")
						.append("\"").append("value").append("\"").append(":").append(Integer.toString(0)).append(",").append("}") 
						.append(",").append("\n");
				}
				break;
			
			case 1: //LUA_TBOOLEAN:
				{
					byte[] byte_ = new byte[1];
					readbuf(fin, byte_, 1);
					
					tab(ostring, level + 1);
					ostring.append("{").append("\"").append("type").append("\"").append(":").append(Integer.toString((int)type[0])).append(", ")
						.append("\"").append("value").append("\"").append(":").append(Integer.toString((int)(byte_[0] & 0xff))).append(",").append("}") 
						.append(",").append("\n");
				}
				break;
			
			case 3: //LUA_TNUMBER:
				{
					byte[] bytes_ = new byte[8];
					readbuf(fin, bytes_, 8);
					double length_ = 0;
					long value_ = 0;
					value_ |= ((long) (bytes_[0] & 0xff)) << (8 * 0); 
					value_ |= ((long) (bytes_[1] & 0xff)) << (8 * 1); 
					value_ |= ((long) (bytes_[2] & 0xff)) << (8 * 2); 
					value_ |= ((long) (bytes_[3] & 0xff)) << (8 * 3); 
					value_ |= ((long) (bytes_[4] & 0xff)) << (8 * 4); 
					value_ |= ((long) (bytes_[5] & 0xff)) << (8 * 5); 
					value_ |= ((long) (bytes_[6] & 0xff)) << (8 * 6); 
					value_ |= ((long) (bytes_[7] & 0xff)) << (8 * 7); 
					length_ = Double.longBitsToDouble(value_);  
					
					tab(ostring, level + 1);
					ostring.append("{").append("\"").append("type").append("\"").append(":").append(Integer.toString((int)type[0])).append(", ")
						.append("\"").append("value").append("\"").append(":").append(Double.toString(length_)).append(",}") 
						.append(",").append("\n");
				}
				break;

			case 4: //LUA_TSTRING:
				{
					byte[] bytes_ = new byte[4]; 
					readbuf(fin, bytes_, 4);
					int length_ = 0;
					length_ |= (bytes_[0] & 0xff);
					length_ |= (bytes_[1] & 0xff) << 8;
					length_ |= (bytes_[2] & 0xff) << 16;
					length_ |= (bytes_[3] & 0xff) << 24;
					bytes_ = new byte[length_];
					readbuf(fin, bytes_, length_);
					
					tab(ostring, level + 1);
					ostring.append("{").append("\"").append("type").append("\"").append(":").append(Integer.toString((int)type[0])).append(", ");
					ostring.append("\"").append("value").append("\"").append(":").append("\"");
					for (int i_ = 0; i_ < length_; i_++) {
						if (Character.isSpaceChar((char)bytes_[i_]) || 
							(!Character.isDigit(bytes_[i_]) && 
							!Character.isLetter((char)bytes_[i_]))) {
							ostring.append("\\x").append(String.format("%02X", (bytes_[i_] & 0xff)));
						} else if (bytes_[i_] == '"') {
							ostring.append("\\\"");
						} else {
							ostring.append((char)bytes_[i_]);
						}
					}
					ostring.append("\"").append(",") 
						.append("}, ").append("\n");
				}
				break;
			
			default:
				throw new RuntimeException("abort");
			}
		}

		tab(ostring, level);
		ostring.append("],").append("\n");

		bytes = new byte[4]; 
		readbuf(fin, bytes, 4);
		length = 0;
		length |= (bytes[0] & 0xff);
		length |= (bytes[1] & 0xff) << 8;
		length |= (bytes[2] & 0xff) << 16;
		length |= (bytes[3] & 0xff) << 24;
		
		tab(ostring, level);
		ostring.append("\"").append("sizep").append("\"").append(":")
			.append(Integer.toString(length))
			.append(",").append("\n");
		
		tab(ostring, level);
		ostring.append("\"").append("function").append("\"").append(":").append("\n");
		
		tab(ostring, level);
		ostring.append("[").append("\n");
		for (int j = 0; j < length; ++j) {
			tab(ostring, level + 1);
			ostring.append("{").append("\n");
			
			readfunction(ostring, fin, level + 2);
			
			tab(ostring, level + 1);
			ostring.append("}").append(",").append("\n");
		}
		tab(ostring, level);
		ostring.append("]").append(",").append("\n");
	}

	private int readfile(StringBuffer ostring, 
		String filename, int level) throws IOException {
		File input = new File(filename);
		if (!input.isFile() || !input.canRead()) {
			return -1;
		}
		InputStream istr = new FileInputStream(filename);
		BufferedInputStream fin = new BufferedInputStream(istr);
		tab(ostring, level); 
		ostring.append('{').append("\n");
		readheader(ostring, fin, level + 1);
		readfunction(ostring, fin, level + 1);
		tab(ostring, level);
		ostring.append('}').append("\n");
		fin.close();
		istr.close();
		return 0;
	}

	public ChunkTrans() {
		this.position = 0;
	}
	
	public int run() throws IOException {
		StringBuffer ostring = new StringBuffer();	
		ostring.append("<html>").append("\n")
			.append("<body>").append("\n")
			.append("<script type=\"application/javascript\">").append("\n");
		ostring.append("var luac_out = ").append("\n");
		int result = readfile(ostring, "luac.out", 0);
		ostring.append("document.write(\"hello, world!\");").append("\n");
		ostring.append("</script>").append("\n")
			.append("</body>").append("\n")
			.append("</html>").append("\n");
		if (false) {
			System.out.println(ostring.toString());
		} else {
			OutputStream ostr = new FileOutputStream("out.htm");
			Writer writer = new OutputStreamWriter(ostr, "UTF-8");
			BufferedWriter outf = new BufferedWriter(writer);
			outf.write(ostring.toString());
			outf.close();
			writer.close();
			ostr.close();
		}
		return 0;
	}
	
	public static void main(String[] args) throws IOException {
		new ChunkTrans().run();
	}
	
	public static byte[] double2Bytes(double d) {  
        long value = Double.doubleToRawLongBits(d);  
        byte[] byteRet = new byte[8];  
        for (int i = 0; i < 8; i++) {  
            byteRet[i] = (byte) ((value >> 8 * i) & 0xff);  
        }  
        return byteRet;  
    }
	
	public static double bytes2Double(byte[] arr) {  
        long value = 0;  
        for (int i = 0; i < 8; i++) {  
            value |= ((long) (arr[i] & 0xff)) << (8 * i);  
        }  
        return Double.longBitsToDouble(value);  
    } 
}
