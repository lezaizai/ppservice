package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_FILEINFO;

/**
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          File size                            |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             file name      ......                             |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure: Format of File Attribute
 *
 */

public class FileInfoAttribute extends BaseAttribute {

	long filesize;
	String path;

	public FileInfoAttribute(int type, int length, byte[] valueData) {
		super(type, length, valueData);
		filesize = Bytes.bytes2Long(valueData);

		path = "";
		for (int i = 8; i < valueData.length; i++) {
			char c = (char) valueData[i];

			if (c == '\0') break;

			path += c;
		}
	}

	public long getSize() {
		return filesize;
	}
	
	public String getPath() {
		return path;
	}
	
	public static FileInfoAttribute createAttribute(long filesize, byte[] path) {

		byte[] valueData = Bytes.concat(
				Bytes.long2Bytes(filesize),
				path);
		return new FileInfoAttribute(
				ATTRIBUTE_FILEINFO,
				valueData.length,
				Bytes.padTo4ByteBoundary(valueData));
	}
}