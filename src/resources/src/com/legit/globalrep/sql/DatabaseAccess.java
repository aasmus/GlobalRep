/*
 * MIT License
 * 
 * Copyright (c) 2017 Austin Asmus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.legit.globalrep.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.legit.globalrep.chat.Message;
import com.legit.globalrep.object.Rep;

public class DatabaseAccess {
	private DatabaseConnection dbConn;
	private Connection connection;
	private Message msg;
	
	private final String CREATE_USER = "CREATE TABLE IF NOT EXISTS User (userId INT auto_increment NOT NULL PRIMARY KEY, UUID varchar(40) NOT NULL UNIQUE, username varchar(16) NOT NULL)";
	private final String CREATE_REP = "CREATE TABLE IF NOT EXISTS Rep (repId INT auto_increment NOT NULL PRIMARY KEY, date varchar(10) NOT NULL, repAmount INT NOT NULL, giverId INT NOT NULL, comment varchar(255), userId INT NOT NULL, FOREIGN KEY (giverId) REFERENCES User (userId), FOREIGN KEY (userId) REFERENCES User (userId))";
	
	private final String GET_USERID_UUID_BY_USERNAME = "SELECT userId, UUID FROM User where username = (?)";
	private final String GET_USERNAME_BY_UUID = "SELECT username FROM User WHERE UUID = (?)";
	private final String GET_REP_BY_UUID = "SELECT r.date, r.repAmount, r.comment, u.username FROM Rep r JOIN  User u ON r.giverId = u.userId WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) ORDER BY repId DESC";
	private final String GET_RECIEVERID_BY_UUID = "SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))";
	private final String GET_REPID_BY_GIVERID = "SELECT repId FROM Rep WHERE giverId = (SELECT userId FROM User WHERE username = ?) AND userId = (SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?))";
	private final String GET_TOTAL_REP = "SELECT r.repAmount FROM Rep r WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?)";
	private final String GET_POSITIVE_REP = "SELECT r.repAmount FROM Rep r WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) AND r.repAmount > 0";
	private final String GET_NEGATIVE_REP = "SELECT r.repAmount FROM Rep r WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) AND r.repAmount < 0";
	
	private final String INSERT_UUID_USERNAME = "INSERT INTO User (UUID, username) VALUES (?, ?)";
	private final String INSERT_REP_BY_USERNAME = "INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES (?, ?, (SELECT userId FROM User WHERE uuid = (?)), ?, (SELECT userId FROM User WHERE uuid = (SELECT uuid FROM User WHERE username = ?)))";
	
	private final String UPDATE_USERNAME_BY_UUID = "UPDATE User SET username = ? WHERE UUID = ?";
	
	private final String DELETE_REP_BY_USERID = "DELETE FROM Rep WHERE userId = ?";
	private final String DELETE_REP_BY_REPID = "DELETE FROM Rep WHERE repId = ?";
	private final String DELETE_USER_BY_UUID = "DELETE FROM User WHERE UUID = ?";

	public DatabaseAccess(String databaseIp, int databasePort, String databaseName, String username, String password, Message msg) {
		dbConn = new DatabaseConnection(databaseIp, databasePort, databaseName, username, password);
		connection = dbConn.getConnection();
		this.msg = msg;
	}
	
	/**
	 * createTable: Called to create MySQL tables, if they don't already exist
	 * 
	 */
	public void createTables() {
		this.connection = dbConn.checkConnection(connection);
		if (connection == null) {
			return;
		}
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(CREATE_USER);
			ps.setQueryTimeout(5);
			ps.executeUpdate();
		} catch (SQLException e) {
			Message.databaseError(e);
		}
		try {
			ps = connection.prepareStatement(CREATE_REP);
			ps.setQueryTimeout(5);
			ps.executeUpdate();
		} catch (SQLException e) {
			Message.databaseError(e);
		}
	}
	
	/**
	 * checkDatabase: used to check database for an existing UUID/username combo
	 * 
	 * @param name - username of player having username checked
	 * @param uuid - UUID of player having username checked
	 */
	public void checkDatabase(String name, String uuid) {
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_USERID_UUID_BY_USERNAME);
			ps.setString(1, name);
			ps.setQueryTimeout(5);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!rs.getString("UUID").equals(uuid)) {
					ps = connection.prepareStatement(DELETE_REP_BY_USERID);
					ps.setInt(1, rs.getInt("userId"));
					ps.executeUpdate();
					ps = connection.prepareStatement(DELETE_USER_BY_UUID);
					ps.setString(1, rs.getString("UUID"));
					ps.executeUpdate();
				}
			}
			ps = connection.prepareStatement(GET_USERNAME_BY_UUID);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
			if (!rs.next()) {
				ps = connection.prepareStatement(INSERT_UUID_USERNAME);
				ps.setString(1, uuid);
				ps.setString(2, name);
				ps.executeUpdate();
			} else if (!rs.getString("username").equals(name)) {
				ps = connection.prepareStatement(UPDATE_USERNAME_BY_UUID);
				ps.setString(1, name);
				ps.setString(2, uuid);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
	}
	
	/**
	 * GetRep: used to retrieve a player's reputation records from the database
	 * 
	 * @param player - command sender
	 * @param username - name of the player who's rep is being looked up
	 * @param uuid - UUID of the player who's rep is being looked up
	 * @param page - rep page # that command sender is currently on
	 */
	public void getRep(Player player, String username, UUID uuid, int page) {
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_RECIEVERID_BY_UUID);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();
			rs.next();
			int userId = rs.getInt("userId");
			if (userId == 0) {
				msg.send(player, "NO_REP", username);
			} else {
				try {
					ps = connection.prepareStatement(GET_REP_BY_UUID);
					ps.setString(1, uuid.toString());
					rs = ps.executeQuery();
					List<Rep> reps = Collections.synchronizedList(new ArrayList<Rep>());
					int numRep = 0;
					while (rs.next()) {
						Rep rep = new Rep(rs.getInt("r.repAmount"), rs.getString("r.date"), rs.getString("u.username"), rs.getString("r.comment"));
						reps.add(rep);
						numRep++;
					}
					int totalPages = (numRep + 9) / 10;
					msg.displayRep(player, username, reps, page, totalPages);
				} catch (Exception e) {
					Message.databaseError(e);
				}
			}
		} catch (Exception e) {
			msg.send(player, "NO_REP", username);
		}
	}
	
	/**
	 * getTotalRep: used to retrieve a player's reputation records from the database
	 * 
	 * @param uuid - UUID of the player who's total rep is being looked up
	 */
	public int getTotalRep(UUID uuid) {
		int rep = 0;
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_TOTAL_REP);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				rep += rs.getInt("r.repAmount");
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rep;
	}
	
	/**
	 * getPositiveRep: used to retrieve a player's reputation records from the database
	 * 
	 * @param uuid - UUID of the player who's total rep is being looked up
	 */
	public int getPositiveRep(UUID uuid) {
		int rep = 0;
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_POSITIVE_REP);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				rep += rs.getInt("r.repAmount");
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rep;
	}
	
	/**
	 * getPositiveRep: used to retrieve a player's reputation records from the database
	 * 
	 * @param uuid - UUID of the player who's total rep is being looked up
	 */
	public int getNegativeRep(UUID uuid) {
		int rep = 0;
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_NEGATIVE_REP);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				rep += rs.getInt("r.repAmount");
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return rep;
	}
	
	/**
	 * addRep: used to add a reputation record to the database
	 * 
	 * @param player - command sender
	 * @param username - rep reciever's username
	 * @param rep - amount of rep given
	 * @param comment - comment given by the command sender to the rep reciever
	 */
	public void addRep(Player player, String username, int rep, String comment) {
		if (player.getName().equalsIgnoreCase(username)) {
			msg.send(player, "SELF_REP");
			return;
		}
		this.connection = dbConn.checkConnection(connection);
		removeRep(username, player.getName());
		LocalDateTime currentDate = LocalDateTime.now();
		String date = currentDate.toString();
		String[] time = date.split("T");
		date = time[0];
		try {
			PreparedStatement ps = connection.prepareStatement(INSERT_REP_BY_USERNAME);
			ps.setString(1, date);
			ps.setInt(2, rep);
			ps.setString(3, player.getUniqueId().toString());
			ps.setString(4, comment);
			ps.setString(5, username);
			ps.executeUpdate();
		} catch (SQLException e) {
			msg.send(player, "NO_PLAYER");
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		msg.send(player, "REP_GIVEN", username);
		try {
			Player reciever = Bukkit.getPlayer(username);
			msg.send(reciever, "REP_ADDED", reciever.getName());
		} catch (NullPointerException e) {
			// no handling necessary if player isn't online
		}
	}
	
	/**
	 * removeRep: used to remove a reputation record from the database
	 * 
	 * @param Player - command sender
	 * @param reciever - username of player who recieved the rep record
	 * @param giver - username of player who gave the rep record
	 */
	public void removeRep(Player player, String reciever, String giver) {
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_REPID_BY_GIVERID);
			ps.setQueryTimeout(5);
			ps.setString(1, giver);
			ps.setString(2, reciever);
			ResultSet rs = ps.executeQuery();
			if(!rs.next()) {
				msg.send(player, "NO_RECORD");
				return;
			}
			ps = connection.prepareStatement(DELETE_REP_BY_REPID);
			ps.setInt(1, rs.getInt("repId"));
			ps.executeUpdate();
			msg.send(player, "REP_REMOVED");
		} catch (SQLException e) {
			Message.databaseError(e);
		}
	}
	
	/**
	 * removeRep: used to remove a reputation record from the database
	 * 
	 * 
	 * @param reciever - username of player who recieved the rep record
	 * @param giver - username of player who gave the rep record
	 */
	public void removeRep(String reciever, String giver) {
		this.connection = dbConn.checkConnection(connection);
		try {
			PreparedStatement ps = connection.prepareStatement(GET_REPID_BY_GIVERID);
			ps.setQueryTimeout(5);
			ps.setString(1, giver);
			ps.setString(2, reciever);
			ResultSet rs = ps.executeQuery();
			ps = connection.prepareStatement(DELETE_REP_BY_REPID);
			while (rs.next()) {
				ps.setInt(1, rs.getInt("repId"));
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
	}
	
}
