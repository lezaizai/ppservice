package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;
import io.vos.stun.util.InternetChecksum;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_DATA;

/**
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Latitude                             |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Longitude                            |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure: Format of Location Attribute
 *
 */

public class LocationAttribute extends BaseAttribute {

	double latitude;
	double longitude;

	public LocationAttribute(int type, int length, byte[] valueData) {
		super(type, length, valueData);

		byte[] latiData = new byte[8];
		byte[] longiData = new byte[8];
		// we can just copy to length, because the valueData array is already
		// initialized to 0 byte values
		System.arraycopy(valueData, 0, latiData, 0, 8);
		System.arraycopy(valueData, 8, longiData, 0, 8);

		latitude = Bytes.bytes2Double(latiData);
		longitude = Bytes.bytes2Double(longiData);
	}

	public double getLatitude() {
		return longitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public static LocationAttribute createAttribute(double latitude, double longitude) {

		byte[] valueData = Bytes.concat(
				Bytes.double2Bytes(latitude),
				Bytes.double2Bytes(longitude));
		return new LocationAttribute(
				ATTRIBUTE_DATA,
				valueData.length,
				Bytes.padTo4ByteBoundary(valueData));
	}

}