package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;
import io.vos.stun.util.InternetChecksum;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_DATA;
import static io.vos.stun.attribute.Attributes.ATTRIBUTE_SIMPLE;

/**
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             type              |          ......
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure: Format of Data Attribute
 *
 */

public class SimpleAttribute extends BaseAttribute {

	int type;

	public SimpleAttribute(int type, int length, byte[] valueData) {
		super(type, length, valueData);
		type = (int) (valueData[0] << 8 | valueData[1] & 0xFF) & 0xFFFF;
	}


	public static SimpleAttribute createAttribute(int type, byte[] data) {

		byte[] valueData = Bytes.concat(
				Bytes.intToBytes(type, 2 /* maxBytes */),
				data);
		return new SimpleAttribute(
				ATTRIBUTE_SIMPLE,
				valueData.length,
				Bytes.padTo4ByteBoundary(valueData));
	}
}