package io.vos.stun.attribute;

import io.vos.stun.util.Bytes;
import io.vos.stun.util.InternetChecksum;

import static io.vos.stun.attribute.Attributes.ATTRIBUTE_DATA;

/**
 *
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Uid                                  |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Latitude                             |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Longitude                            |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          Locatetime                           |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *                 Figure: Format of UserLocation Attribute
 *
 */

public class LocationAttribute extends BaseAttribute {

	long uid;
	double latitude;
	double longitude;
	long locatetime;

	public LocationAttribute(int type, int length, byte[] valueData) {
		super(type, length, valueData);

		byte[] uidData = new byte[8];
		byte[] latiData = new byte[8];
		byte[] longiData = new byte[8];
		byte[] locatetimeData = new byte[8];
		// we can just copy to length, because the valueData array is already
		// initialized to 0 byte values
		System.arraycopy(valueData, 0, uidData, 0, 8);
		System.arraycopy(valueData, 8, latiData, 0, 8);
		System.arraycopy(valueData, 16, longiData, 0, 8);
		System.arraycopy(valueData, 24, locatetimeData, 0, 8);

		uid = Bytes.bytes2Long(uidData);
		latitude = Bytes.bytes2Double(latiData);
		longitude = Bytes.bytes2Double(longiData);
		locatetime = Bytes.bytes2Long(locatetimeData);
	}

	public long getUid() {
		return uid;
	}

	public double getLatitude() {
		return longitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public static LocationAttribute createAttribute(long uid, double latitude, double longitude, long locatetime) {

		byte[] valueData = Bytes.concat(
				Bytes.long2Bytes(uid),
				Bytes.double2Bytes(latitude),
				Bytes.double2Bytes(longitude),
				Bytes.long2Bytes(locatetime));
		return new LocationAttribute(
				ATTRIBUTE_DATA,
				valueData.length,
				Bytes.padTo4ByteBoundary(valueData));
	}

}