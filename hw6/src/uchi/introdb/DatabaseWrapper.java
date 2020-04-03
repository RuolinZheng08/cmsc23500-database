package uchi.introdb;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class DatabaseWrapper {


	//Keep connection open from openConnection to closeConnection
	private Connection conn = null;

	//How many records to batch together
	private int batchSize;
	// the number of inserts done for the current batch, once this counter reaches batchSize, conn.commit()
	private int insertCounter;

	private PreparedStatement insertRecord = null;
	private HashMap<String, PreparedStatement> strPreparedStatementMap;
	private String servReqNumColumnName;

	/**
	 * Constructor that takes the batchsize for inserting
	 */
	public DatabaseWrapper(int batchSize){
		this.batchSize = batchSize;
		servReqNumColumnName = "service_request_num";
		strPreparedStatementMap = new HashMap<>();
	}

	/*
	* Constructor that defaults batchsize to 1
	*/
	public DatabaseWrapper(){
		this(1);
	}

	/**
	 * Open a connection to the db in the conn object. Keep open until closed.
	 * Use the variables from DatabaseConstants.java
	 * returns a false if the connection cannot open or if the connection is already open
	 */
	public boolean openConnection(){
		System.out.println(String.format("Trying to open a connection to %s on host: %s", DatabaseConstants.DBNAME, DatabaseConstants.HOSTNAME));
		try {
			if (conn != null && !conn.isClosed()) {
				System.out.println("Connection is already open and hasn't been closed.");
				return false; // already open
			}
			Properties props = new Properties();
			props.setProperty("user", DatabaseConstants.USERNAME);
			props.setProperty("password", DatabaseConstants.PASSWORD);
			props.setProperty("tcpKeepAlive", "true");
			conn = DriverManager.getConnection(DatabaseConstants.HOSTNAME, props);
		} catch (SQLException se) {
			se.printStackTrace();
			return false;
		}
		System.out.println("Finished opening connection.");
		return true;
	}

	/**
	 * Close the conn object
	 */
	public void closeConnection() {
		//Implement this!
		try {
			// clean up
			if (insertRecord != null) {
				insertRecord.close();
			}
			for (PreparedStatement ps : strPreparedStatementMap.values()) {
				ps.close();
			}
			conn.close();
			insertRecord = null;
			strPreparedStatementMap = new HashMap<>();
			conn = null;
		} catch (SQLException se) {
			se.printStackTrace();
		}
		System.out.println("Finished closing connection.");
	}

	/**
	 * Drop the potholesTableName if it exists
	 */
	public void dropPotholeTable(){
		//hint use execute and not executeSQL
		//Implement this!
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS " + DatabaseConstants.POTHOLETABLENAME;
			stmt.executeUpdate(sql);
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}



	/**
	 * Create table to support the following attributes. Exact data type is up to you
	 * CREATION DATE (date),
	 * SERVICE REQUEST NUMBER (string),
	 * MOST RECENT ACTION (string),
	 * NUMBER OF POTHOLES FILLED ON BLOCK (number),
	 * STREET ADDRESS (string),
	 * ZIP (int),
	 * LATITUDE (float),
	 * LONGITUDE (float)
	 */
	public void createPotholeTable(){
		//hint use execute and not executeSQL
		//Implement this!
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			String sql = "CREATE TABLE " + DatabaseConstants.POTHOLETABLENAME +
					" (" +
					"creation_date DATE, " +
					servReqNumColumnName + " VARCHAR(255), " +
					"most_recent_action VARCHAR(255), " +
					"num_potholes_filled INT, " +
					"street_address VARCHAR(255), " +
					"zip INT, " +
					"latitude DOUBLE PRECISION, " +
					"longitude DOUBLE PRECISION" +
					");";
			stmt.executeUpdate(sql);
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add at least two indexes to potholesTableName to make the queries run faster
	 */
	public void createPotholeIndexes(){
		//hint use execute and not executeSQL. Assume the table already exists
		//Implement this!
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE INDEX lat_index ON " + DatabaseConstants.POTHOLETABLENAME + " USING btree (latitude)");
			stmt.executeUpdate("CREATE INDEX long_index ON " + DatabaseConstants.POTHOLETABLENAME + " USING btree (longitude)");
//			stmt.executeUpdate("CREATE INDEX action_index ON " + DatabaseConstants.POTHOLETABLENAME + " USING hash (action)");
//			stmt.executeUpdate("CREATE INDEX zip_index ON " + DatabaseConstants.POTHOLETABLENAME + " USING hash (zip)");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private String[] getFieldAndCompareStr(Predicate<?> p) {
		String[] ret = new String[2];
		switch (p.field) {
			case LONGITUDE:
				ret[0] = "longitude";
				break;
			case LATITUDE:
				ret[0] = "latitude";
				break;
			case ZIP:
				ret[0] = "zip";
				break;
			case NUM_POTHOLES:
				ret[0] = "num_potholes_filled";
				break;
			case RECENT_ACTIONS:
				ret[0] = "most_recent_action";
				break;
		}
		switch (p.compare) {
			case EQUALS:
				ret[1] = "=";
				break;
			case GREATERTHAN:
				ret[1] = ">";
				break;
			case LESSTHAN:
				ret[1] = "<";
				break;
		}
		return ret;
	}

	private String createSelectStr(Predicate<?> p1, Predicate<?> p2) {
		String[] p1Data = getFieldAndCompareStr(p1);
		String p1Str = p1Data[0] + " " + p1Data[1] + " ?"; // ex. zip = ?
		String p2Str;
		if (p2 == null) { // use '1' = '1' for the second condition
			p2Str = "'1' = '1'";
		} else {
			String[] p2Data = getFieldAndCompareStr(p2);
			p2Str = p2Data[0] + " " + p2Data[1] + " ?";
		}
		// sql injection attack?
		String selectStr = "SELECT " + servReqNumColumnName + " FROM " + DatabaseConstants.POTHOLETABLENAME +
				" WHERE " + p1Str + " AND " + p2Str + ";";
		return selectStr;
	}

	private PreparedStatement getPreparedStatement(Predicate<?> p1, Predicate<?> p2) throws SQLException {
		String key = "";
		String valType = "";
		switch (p1.field) {
			case ZIP:
				if (p2 == null) {
					key = "zip"; // zip =
				} else {
					key = "zipNum"; // zip = , num >
				}
				valType = "int";
				break;
			case LATITUDE:
				key = "lat"; // lat >, lat <
				valType = "double";
				break;
			case LONGITUDE:
				key = "long"; // long >, long <
				valType = "double";
				break;
			case RECENT_ACTIONS:
				key = "act"; // act =
				valType = "str";
		}
		PreparedStatement ps = null;
		if (!strPreparedStatementMap.containsKey(key)) {
			// create prepared statement afresh
			String selectStr = createSelectStr(p1, p2); // contains ?
			ps = conn.prepareStatement(selectStr);
			strPreparedStatementMap.put(key, ps);
		} else {
			ps = strPreparedStatementMap.get(key);
		}
		// populate the two placeholder values in ps using p1 and p2
		switch (valType) {
			case "int":
				ps.setInt(1, Integer.parseInt(p1.value.toString()));
				if (p2 != null) { // zip = ? AND '1' = '1'
					ps.setInt(2, Integer.parseInt(p2.value.toString()));
				}
				break;
			case "double":
				ps.setDouble(1, Double.parseDouble(p1.value.toString()));
				ps.setDouble(2, Double.parseDouble(p2.value.toString()));
				break;
			case "str":
				ps.setString(1, p1.value.toString());
		}

		return ps;
	}

	/**
	 * Execute a SQL query against your PSQL db based on the predicates.
	 * For full credit use PreparedStatements and using them for the queries.
	 * https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
	 *
	 * Additionally for full credit you should create multiple prepared statements once,
	 * and then reuse the prepared statement (see http://tutorials.jenkov.com/jdbc/preparedstatement.html)
	 *
	 * The universe of possible predicates is bound to:
	 *  - p1 ZIP =
	 *  - p1 LAT > , p2 LAT <
	 *  - p1 LONG >, p2 LONG <
	 *  - p1 ZIP =, p2 NUM POTHOLES >
	 *  - p1 MOST RECENT ACTION =
	 * @param p1 Required first predicate.
	 * @param p2 Optional second predicate
	 * @return a list of service request numbers that satisfy the query.
	 */
	public List<String> getServiceRequestNumbers(Predicate<?> p1, Predicate<?> p2){
		//Implement this!
		// in case the connection has dead
		List<String> ret = new LinkedList<>();
		ResultSet rs = null;
		try {
			PreparedStatement ps = getPreparedStatement(p1, p2); // all params populated with p1 and p2
			rs = ps.executeQuery();
			while (rs.next()) {
				ret.add(rs.getString(servReqNumColumnName));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}

	private PreparedStatement setIntOrNull(PreparedStatement ps, int psIndex, String value) throws SQLException {
		if ("".equals(value)) {
			ps.setNull(psIndex, Types.INTEGER);
		} else {
			try {
				ps.setInt(psIndex, Integer.parseInt(value));
			} catch (NumberFormatException | SQLException ne) { // not a valid int
				ps.setNull(psIndex, Types.INTEGER);
			}
		}
		return  ps;
	}

	private PreparedStatement setDoubleOrNull(PreparedStatement ps, int psIndex, String value) throws SQLException {
		if ("".equals(value)) {
			ps.setNull(psIndex, Types.DOUBLE);
		} else {
			try {
				ps.setDouble(psIndex, Double.parseDouble(value));
			} catch (NumberFormatException ne) { // not a valid int
				ps.setNull(psIndex, Types.DOUBLE);
			}
		}
		return ps;
	}

	private PreparedStatement populateInsertRecord(PreparedStatement ps, String[] vals) throws SQLException {
		if (vals.length >= 1) {
			java.util.Date myDate = new java.util.Date(vals[0]);
			java.sql.Date sqlDate = new java.sql.Date(myDate.getTime());
			ps.setDate(1, sqlDate);
		} else {
			ps.setNull(1, java.sql.Types.DATE);
		}
		if (vals.length >= 2) {
			ps.setString(2, vals[1]); // service_request_num
		} else {
			ps.setString(2, "");
		}
		if (vals.length >= 3) {
			ps.setString(3, vals[2]); // most_recent_action
		} else {
			ps.setString(3, "");
		}
		if (vals.length >= 4) {
			ps = setIntOrNull(ps,4, vals[3]); // num_potholes_filled
		} else {
			ps.setNull(4, Types.INTEGER);
		}
		if (vals.length >= 5) {
			ps.setString(5, vals[4]); // street_address
		} else {
			ps.setString(5, "");
		}
		if (vals.length >= 6) {
			ps = setIntOrNull(ps, 6, vals[5]); // zip
		} else {
			ps.setNull(6, Types.INTEGER);
		}
		if (vals.length >= 7) {
			ps = setDoubleOrNull(ps, 7, vals[6]); // latitude
		} else {
			ps.setNull(7, Types.DOUBLE);
		}
		if (vals.length >= 8) {
			ps = setDoubleOrNull(ps, 8, vals[7]); // longitude
		} else {
			ps.setNull(8, Types.DOUBLE);
		}
		return ps;
	}

	/**
	 * Split a CSV line into values, parse the values and load into the database
	 *
	 * I would suggest building PreparedStatements and using the insert query.
	 * https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
	 *
	 * Store all values for full credit
	 * If the line does not insert to due to faulty data or violating constraints, return a false
	 * If this occurs during a batched load (ie transaction) then you should rollback the transaction
	 * and all records associated with this batch are lost.
	 *
	 *
	 * @param tsvLine CREATION DATE       SERVICE REQUEST NUMBER  MOST RECENT ACTION      NUMBER OF POTHOLES FILLED ON BLOCK      STREET ADDRESS  ZIP    LATITUDE LONGITUDE
	 * @return
	 */
	public boolean loadPotholeRecord(String tsvLine){
		//Implement this!
		String[] vals = tsvLine.split("\t");

		try {
			conn.setAutoCommit(false);

			if (insertRecord == null) { // create
				// eight columns
				String insertStr = "INSERT INTO " + DatabaseConstants.POTHOLETABLENAME +
						" VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
				insertRecord = conn.prepareStatement(insertStr);
			}
			// populate the prepared statement insertRecord with values in vals
			insertRecord = populateInsertRecord(insertRecord, vals);

		} catch (SQLException e) {
			System.out.println("SQLException, Length: " + vals.length + " Data: " + Arrays.toString(vals));
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			System.out.println("Exception Length: " + vals.length + " Data: " + Arrays.toString(vals));
			return false;
		}
		// insert successfully prepared, check if this batch is full, if so, commit
		try {
			insertRecord.executeUpdate();
			insertCounter += 1;
			if (insertCounter == batchSize) {
				conn.commit();
				insertCounter = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	/*
	* Called after all data is loaded to process any records that do not align with the batch size
	*/
	public void finalizeLoading() {
		//Implement this!
		try {
			conn.commit(); // commit the entries in the last batch
			insertCounter = 0;
			insertRecord = null;
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
