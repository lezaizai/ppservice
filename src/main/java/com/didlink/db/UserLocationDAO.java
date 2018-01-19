package com.didlink.db;

import com.didlink.models.UserLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UserLocationDAO {

	private static final Logger LOGGER = Logger
			.getLogger(UserLocationDAO.class.getName());

	private static final String SQL_SAVE_LOCATION = "insert into user_location(uid,latitude,longitude,locatetime) values(?,?,?,?)";

	public UserLocationDAO() throws Exception {
	}

	public Connection getConnection() throws Exception {

		Connection con = null;
		try {

			con = MysqlDBManager.getInstance().getConnection(
					"DIDLINK");

			LOGGER.finest("DB Connection Opened");
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "ERROR getting DB Connection", ex);

			throw ex;
		}
		return con;
	}

	public void closeConnection(Connection connection,
			PreparedStatement statement, ResultSet resultSet) throws Exception {

		try {
			try {
				try {
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					if (resultSet != null) {
						resultSet.close();
						LOGGER.finest("DB ResultSet Closed");
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (statement != null) {
					statement.close();
					LOGGER.finest("DB Statement Closed");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.log(Level.INFO, "ERROR Closing DB Connection", ex);
		} finally {
			if (connection != null) {
				connection.close();
				LOGGER.finest("DB Connection Closed");
			}

		}

	}

	public void saveLocation(UserLocation oLocation)
			throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		try {

			con = getConnection();

			String sQry = SQL_SAVE_LOCATION;

			LOGGER.finest("Save Qry::[" + sQry + "]");

			statement = con.prepareStatement(sQry);

			statement.setLong(1, oLocation.getUid());
			statement.setDouble(2, oLocation.getLatitude());
			statement.setDouble(3, oLocation.getLongtitude());
			statement.setLong(4, oLocation.getLocatetime());

			statement.executeUpdate();

		} catch (Exception ex) {

			LOGGER.log(Level.INFO, "ERROR saving user location information", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, null);
		}
	}

/*
	public void savePreviewRecord(PreviewRecord oPreviewRecord)
			throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		try {

			con = getConnection();

			String sQry = (m_dbProperties.getProperty(PROPERTY_SAVE_QUERY));

			LOGGER.finest("Save Qry::[" + sQry + "]");

			statement = con.prepareStatement(sQry);

			statement.setString(1, oPreviewRecord.getSource());
			statement.setString(2, oPreviewRecord.getRecId());
			statement.setString(3, oPreviewRecord.getObjId());
			statement.setString(4, oPreviewRecord.getStatus());
			statement.setInt(5, oPreviewRecord.getQueue());
			statement.setInt(6, oPreviewRecord.getPriority());
			statement.setString(7, oPreviewRecord.getPath());

			String sMessage = oPreviewRecord.getMessage();
			if (sMessage != null) {
				if (sMessage.length() > 1998) {
					statement.setString(8, sMessage.substring(0, 1998));
				} else {
					statement.setString(8, sMessage);
				}
			} else {
				statement.setString(8, sMessage);
			}

			statement.executeUpdate();

		} catch (Exception ex) {

			LOGGER.log(Level.INFO, "ERROR saving Preview Record", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, null);
		}
	}

	public ArrayList<PreviewRecord> retrieveNextManualUpdateBatch()
			throws Exception {
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		ArrayList<PreviewRecord> alPreviewRecords = new ArrayList<PreviewRecord>();
		try {

			con = getConnection();
			String sQry = null;

			// Get from all queues
			sQry = (m_dbProperties.getProperty(PROPERTY_MANUAL_RETRIEVE_QUERY));

			LOGGER.finest("Manual Retrieve Qry::[" + sQry + "]");

			statement = con.prepareStatement(sQry);
			statement.setString(1,
					m_dbProperties.getProperty(PROPERTY_SOURCE_SYSTEM));

			rs = statement.executeQuery();

			int n = 0;
			while (rs.next()) {
				PreviewRecord pRec = new PreviewRecord();

				pRec.setId(rs.getInt(ID));
				pRec.setRecId(rs.getString(RECORD_ID));
				pRec.setObjId(rs.getString(OBJ_ID));
				pRec.setSource(rs.getString(SOURCE));
				pRec.setStatus(rs.getString(STATUS));
				pRec.setLastModifiedDate(rs.getTimestamp(MODIFIED_DATE));
				pRec.setPriority(rs.getInt(PRIORITY));

				alPreviewRecords.add(pRec);

				n++;
			}

			LOGGER.info("Retrieved [" + n + "] records for Manual ReTries");

		} catch (Exception ex) {

			LOGGER.log(Level.INFO, "ERROR retrieving Manual Update Batch", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, rs);
		}

		return alPreviewRecords;
	}

	public PreviewRecord retrievePreviewRecord(int nId) throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		PreviewRecord pRec = null;

		try {

			con = getConnection();
			String sQry = (m_dbProperties.getProperty(PROPERTY_RETRIEVE_BY_ID));

			LOGGER.finest("Retrieve Qry::[" + sQry + "]");

			statement = con.prepareStatement(sQry);
			statement.setInt(1, nId);
			rs = statement.executeQuery();

			while (rs.next()) {
				pRec = new PreviewRecord();

				pRec.setId(rs.getInt(ID));
				pRec.setRecId(rs.getString(RECORD_ID));
				pRec.setObjId(rs.getString(OBJ_ID));
				pRec.setSource(rs.getString(SOURCE));
				pRec.setStatus(rs.getString(STATUS));
				pRec.setPath(rs.getString(PATH));

			}

		} catch (Exception ex) {

			LOGGER.log(Level.INFO,
					"ERROR retrieving Next Preview Record Batch", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, rs);
		}

		return pRec;
	}

	public void deletePreviewRecordFromStaging(int nId) throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		try {

			con = getConnection();

			String sQry = (m_dbProperties.getProperty(PROPERTY_DELETE_QUERY));

			LOGGER.finest("Delete Qry::[" + sQry + "]");

			statement = con.prepareStatement(sQry);

			statement.setInt(1, nId);

			statement.executeUpdate();

		} catch (Exception ex) {

			LOGGER.log(Level.INFO,
					"ERROR deleting Preview Record From Staging", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, null);
		}
	}

	public void updatePreviewRecordFromStagingStatus(int nId, String sStatus,
			String sPath, String sMessage) throws Exception {

		Connection con = null;
		PreparedStatement statement = null;
		try {

			con = getConnection();

			String sQry = null;

			if ((sPath == null) || (sPath.equals(""))) {
				sQry = (m_dbProperties
						.getProperty(PROPERTY_STATUS_UPDATE_QUERY));

				statement = con.prepareStatement(sQry);
				statement.setString(1, sStatus);
				if (sMessage != null) {
					if (sMessage.length() > 1998) {
						statement.setString(2, sMessage.substring(0, 1998));
					} else {
						statement.setString(2, sMessage);
					}
				} else {
					statement.setString(2, sMessage);
				}

				statement.setInt(3, nId);
			} else {
				sQry = (m_dbProperties.getProperty(PROPERTY_PATH_UPDATE_QUERY));

				statement = con.prepareStatement(sQry);
				statement.setString(1, sStatus);
				statement.setString(2, sPath);

				if (sMessage != null) {
					if (sMessage.length() > 1998) {
						statement.setString(3, sMessage.substring(0, 1998));
					} else {
						statement.setString(3, sMessage);
					}
				} else {
					statement.setString(3, sMessage);
				}

				statement.setInt(4, nId);
			}

			LOGGER.finest("Update Qry::[" + sQry + "]");

			statement.executeUpdate();

		} catch (Exception ex) {

			LOGGER.log(Level.INFO, "ERROR updating Preview record Status", ex);
			throw ex;
		} finally {
			closeConnection(con, statement, null);
		}
	}
*/
}
