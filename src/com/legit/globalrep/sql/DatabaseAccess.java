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
import com.legit.globalrep.commands.RepCommand;
import com.legit.globalrep.object.Rep;

public class DatabaseAccess {
	private final String DB_NAME;
	private DatabaseConnection dbConn;
	private Connection connection;

	public DatabaseAccess(String databaseIp, int databasePort, String databaseName, String username, String password) {
		this.DB_NAME = databaseName;
		dbConn = new DatabaseConnection(databaseIp, databasePort, databaseName, username, password);
		connection = dbConn.getConnection();
	}
	
	/**
	 * createTable: Called to create MySQL tables, if they don't already exist
	 * 
	 * @param name - name of the MySQL table to be created
	 */
	public void createTable(String name) {
		dbConn.checkConnection(connection);
		String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, DB_NAME);
			ps.setString(2, name);
			rs = ps.executeQuery();
			if(!rs.next()){
				if(name.equals("User")) {
			    	query = "CREATE TABLE User "
							+ "(userId INT auto_increment NOT NULL PRIMARY KEY, "
							+ "UUID varchar(40) NOT NULL UNIQUE, "
							+ "username varchar(16) NOT NULL)";
				} else if(name.equals("Rep")){
			    	query = "CREATE TABLE Rep "
							+ "(repId INT auto_increment NOT NULL PRIMARY KEY, "
							+ "date varchar(10) NOT NULL, "
							+ "repAmount INT NOT NULL, "
							+ "giverId INT NOT NULL, "
							+ "comment varchar(255), "
							+ "userId INT NOT NULL, "
							+ "FOREIGN KEY (giverId) REFERENCES User (userId), "
							+ "FOREIGN KEY (userId) REFERENCES User (userId))";
				} else {
					return;
				}
				try {
					ps = connection.prepareStatement(query);
					ps.setQueryTimeout(5);
					ps.executeUpdate();
					Message.tableCreated(name);
				} catch (SQLException e) {
					Message.databaseError(e);
				}
			}
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}	
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
	}
	
	/**
	 * checkDatabase: used to check database for an existing UUID/username combo
	 * 
	 * @param name - username of player having username checked
	 * @param uuid - UUID of player having username checked
	 */
	public synchronized void checkDatabase(String name, String uuid) {
		dbConn.checkConnection(connection);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String query = "SELECT username FROM User WHERE UUID = (?)";
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
			if(!rs.next()) {
				query = "INSERT INTO User (UUID, username) VALUES (?, ?)";
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, uuid);
				ps.setString(2, name);
				ps.executeUpdate();
			} else {
				if(!rs.getString("username").equals(name)) {
				    query = "UPDATE User SET username = ? WHERE UUID = ?";
				    ps = connection.prepareStatement(query);
				    ps.setQueryTimeout(5);
					ps.setString(1, name);
					ps.setString(2, uuid);
					ps.executeUpdate();
				}
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
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
		dbConn.checkConnection(connection);
		int userId = getUserId(uuid);
		PreparedStatement ps = null;
		ResultSet rs = null;
		if (userId == 0) {
			Message.noRep(player, username);
			return;
		} else {
			String query = "SELECT r.date, r.repAmount, r.comment, u.username FROM Rep r JOIN  User u ON r.giverId = u.userId WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) ORDER BY repId DESC";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, uuid.toString());
				rs = ps.executeQuery();
				List<Rep> reps = Collections.synchronizedList(new ArrayList<Rep>());
				int numRep = 0;
				while (rs.next()) {
					Rep rep = new Rep(rs.getInt("r.repAmount"), rs.getString("r.date"), rs.getString("u.username"),
							rs.getString("r.comment"));
					reps.add(rep);
					numRep++;
				}
				int totalPages = (numRep + 10 - 1) / 10;
				RepCommand.displayRep(player, username, reps, page, totalPages);
			} catch (Exception e) {
				Message.databaseError(e);
				Message.genericErrorPlayer(player);
			}
		}
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
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
			Message.repSelf(player);
			return;
		}

		dbConn.checkConnection(connection);
		int repId = getRepIdbyUUID(player.getUniqueId(), username);
		if (repId != 0) {
			removeRep(repId);
		}
		LocalDateTime currentDate = LocalDateTime.now();
		String date = currentDate.toString();
		String[] time = date.split("T");
		date = time[0];

		String query = "INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES "
				+ "(?, ?, (SELECT userId FROM User WHERE uuid = (?)), ?, (SELECT userId FROM User WHERE uuid = (SELECT uuid FROM User WHERE username = ?)))";
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, date);
			ps.setInt(2, rep);
			ps.setString(3, player.getUniqueId().toString());
			ps.setString(4, comment);
			ps.setString(5, username);
			ps.executeUpdate();

			Message.repAddedOther(player, username);
			try {
				Player reciever = Bukkit.getPlayer(username);
				Message.repAddedSelf(reciever);
			} catch (NullPointerException e) {
				// no handling necessary if player isn't online
			}
		} catch (SQLException e) {
			Message.noPlayer(player);
		} catch (NullPointerException e) {
			Message.genericErrorSystem(e);
		}
		try {
			ps.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		return;
	}
	
	
	/**
	 * removeRep: used to remove a reputation record from the database
	 * 
	 * @param repId - repId from the "Rep" table
	 */
	public boolean removeRep(int repId) {
		dbConn.checkConnection(connection);
		String delete = "DELETE FROM Rep WHERE repId = ?";
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(delete);
			ps.setQueryTimeout(5);
			ps.setInt(1, repId);
			ps.executeUpdate();
		} catch (SQLException e) {
			Message.databaseError(e);
			return false;
		}
		try {
			ps.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
			return false;
		}
		return true;
	}
	
	/**
	 * hasLoggedIn: used to see if a player has logged in previously by checking the database
	 * 
	 * @param uuid - UUID of player who is being checked for previous logins
	 * @return - true = has logged in, false = hasn't logged in
	 */
	public boolean hasLoggedIn(UUID uuid) {
		dbConn.checkConnection(connection);
		String query = "SELECT userId FROM User WHERE UUID = (?)";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			if (!rs.next()) {
				ps.close();
				rs.close();
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		return true;
	}
	
	/**
	 * getUserId - used to get the userId of a player from the "User" table
	 * 
	 * @param uuid - UUID of the player who's userId is being looked up
	 * @return - returns a userId or 0 if not found
	 */
	private int getUserId(UUID uuid) {
		dbConn.checkConnection(connection);
		String query = "SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))";
		PreparedStatement ps = null;
		ResultSet rs = null;
		int userId = 0;
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			rs.next();
			userId = rs.getInt("userId");
		} catch(Exception e) {
		}
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		return userId;
	}
	
	/**
	 * getRepIdByUUID - used to get a repId based on giver's UUID
	 * 
	 * @param uuid - UUID of player who gave the rep
	 * @param username - username of player who received the rep
	 * @return - returns repId if exists or 0 if doesn't exist
	 */
	private int getRepIdbyUUID(UUID uuid, String username) {
		dbConn.checkConnection(connection);
		int repId = 0;
		String query = "SELECT repId FROM Rep WHERE giverId = "
				+ "(SELECT userId FROM User WHERE UUID = ?)"
				+ " AND userId = "
				+ "(SELECT userId FROM User WHERE UUID = "
				+ "(SELECT uuid FROM User WHERE username = ?))";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid.toString());
			ps.setString(2, username);
			rs = ps.executeQuery();
			if(rs.next()){
				repId = rs.getInt("repId");
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		return repId;
	}
	
	/**
	 * getrepIdByUsername - used to get a repId by giver's username
	 * 
	 * @param player - command sender
	 * @param reciever - player who recieved rep
	 * @param giver - player who gave rep
	 * @return - returns repId if exists or 0 if doesn't exist
	 */
	public int getrepIdByUsername(Player player, String reciever, String giver) {
		dbConn.checkConnection(connection);
		int repId = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String query = "SELECT repId FROM Rep WHERE giverId = "
				+ "(SELECT userId FROM User WHERE UUID = "
				+ "(SELECT uuid FROM User WHERE username = ?)) AND userId = "
				+ "(SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?))";
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, giver);
			ps.setString(2, reciever);
			rs = ps.executeQuery();
			if(rs.next()) {
				repId = rs.getInt("repId");
			}
		} catch (SQLException e) {
			Message.noRecord(player);
		}
		return repId;
	}
	
}
