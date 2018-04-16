package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class SQLite {
	private boolean sqlDatabaseEnabled = true; // if changed to false, database will be unavailable (e.g. bad driver)
	private boolean doesDatabaseExists = false; // this will trigger the databse creation if need be 
	
	private Connection connection = null;
	public Connection getConnection() {
		if (connection == null) {
			OpenDatabase();
		}
		return connection;
	}
	
	// name and array of strings to create the database if it doesn't exist, each must include "if not exists" clauses
	private String databaseName = null;
	private String[] databaseSchema = null;
	
	// constructor
	public SQLite(String dbName, String[] dbSchema) {
		databaseName = dbName;
		databaseSchema = dbSchema;
	}
	
	//finalize method to ensure database closure upon garbage collection
	public void finalize() throws Throwable {
		log("finalize: closing database");
		CloseDatabase();
		log("finalize: database closed");
	    super.finalize();
	}
	
	// execute a SQL query, return the result
	public ResultSet executeQuery(String sqlStatement) throws SQLException {
		try {
			//PreparedStatement sql = getConnection().prepareStatement(sqlStatement);
			Statement sql = getConnection().createStatement();
			return sql.executeQuery(sqlStatement);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	// execute a SQL insert/update statement
	public int executeUpdate(String sqlStatement) throws SQLException {
		try {
			Statement sql = getConnection().createStatement();
			return sql.executeUpdate(sqlStatement);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	// create database schema if it doesn't already exist
	// TODO: abstract out database schema to standalone script
	private void CreateDatabaseIfNew() throws SQLException {
		try {
			Statement sql = getConnection().createStatement();
			sql.setQueryTimeout(30); // 30 second timeout
			for (String statement : databaseSchema) {
				sql.executeUpdate(statement);
			}
			log("CreateDatabaseIfNew: database created (if not previously created)");
			doesDatabaseExists = true;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			sqlDatabaseEnabled = false;
			throw ex;
		}
	}
	
	// load JDBC driver, open database, creating it if it doesn't already exist, including tables
	private void OpenDatabase() {
		if (sqlDatabaseEnabled) {
			try {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection("jdbc:sqlite:trafficController.db");
				if (!doesDatabaseExists)
					CreateDatabaseIfNew();
				log("OpenDatabase: Database successfully opened");
			}
			catch (Exception ex) {
				ex.printStackTrace();
				sqlDatabaseEnabled = false;
			}
		} else {
			log("OpenDatabase: TrafficController.sqlDatabaseEnabled is false, database is unavailable");
		}		
	}

	// closes the database connection, only if it exists
	public void CloseDatabase() {
		try {
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			sqlDatabaseEnabled = false;
		}
	}
	
	// log text messages to console
	private void log(String format, Object ... args) {
		System.out.println(String.format("%s %s %s", "SQLite:", Instant.now().toString(), String.format(format, args)));
	}
}
