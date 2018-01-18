package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;
import io.vos.stun.util.InternetChecksum;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_DATA;

/**
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          SequenceNum                          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             Checksum            |           length            |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure: Format of Data Attribute
 *
 */

public class DataAttribute extends BaseAttribute {

	int sequenceNum;
	int checkSum;

	public DataAttribute(int type, int length, byte[] valueData) {
		super(type, length, valueData);
		sequenceNum = (valueData[0] << 24 | valueData[1] << 16 | valueData[2] << 8
				| valueData[3] & 0xFF);
		checkSum = (int) (valueData[4] << 8 | valueData[5] & 0xFF) & 0xFFFF;
	}

	public int getSequenceNum() {
		return sequenceNum;
	}
	
	public int getChecksum() {
		return checkSum;
	}
	
	public static DataAttribute createAttribute(int sequence, byte[] data) {

		byte[] valueData = Bytes.concat(
				Bytes.intToBytes(sequence),
				Bytes.intToBytes((int) InternetChecksum.calculateChecksum(data), 2 /* maxBytes */),
				Bytes.intToBytes(data.length, 2 /* maxBytes */),
				data);
		return new DataAttribute(
				ATTRIBUTE_DATA,
				valueData.length,
				Bytes.padTo4ByteBoundary(valueData));
	}

}