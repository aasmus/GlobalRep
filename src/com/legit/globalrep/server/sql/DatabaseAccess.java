/*
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
package com.legit.globalrep.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.legit.globalrep.Rep;
import com.legit.globalrep.util.Message;
import com.legit.globalrep.util.UUIDFetcher;

public class DatabaseAccess {
	private String DB_NAME;
	private DatabaseConnection dbConn;
	private Connection connection;

	public DatabaseAccess(String databaseIp, int databasePort, String databaseName, String username, String password) {
		this.DB_NAME = databaseName;
		dbConn = new DatabaseConnection(databaseIp, databasePort, databaseName, username, password);
		connection = dbConn.getConnection();
	}
	
	public void generateTables() {
		try {
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'User'";
			
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				Message.databaseError(e);
			}
			String table;
			if(!rs.next()){
				table = "CREATE TABLE User "
				+ "(userId INT auto_increment NOT NULL PRIMARY KEY, "
				+ "UUID varchar(40) NOT NULL UNIQUE, "
				+ "username varchar(16) NOT NULL)";

				try {
					ps = connection.prepareStatement(table);
					ps.setQueryTimeout(5);
					ps.executeUpdate();
					Message.tableCreated("User");
				} catch (SQLException e) {
					Message.databaseError(e);
				}
			}
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'Rep'";
			
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				Message.databaseError(e);
			}
			
			if(!rs.next()) {
				table = "CREATE TABLE Rep "
						+ "(repId INT auto_increment NOT NULL PRIMARY KEY, "
						+ "date varchar(10) NOT NULL, "
						+ "repAmount INT NOT NULL, "
						+ "giverId INT NOT NULL, "
						+ "comment varchar(255), "
						+ "userId INT NOT NULL, "
						+ "FOREIGN KEY (giverId) REFERENCES User (userId), "
						+ "FOREIGN KEY (userId) REFERENCES User (userId))";

				try {
					ps = connection.prepareStatement(table);
					ps.setQueryTimeout(5);
					ps.executeUpdate();
					Message.tableCreated("Rep");
				} catch (SQLException e) {
					Message.databaseError(e);
				}
			}
			DatabaseConnection.cleanConnection(ps, rs);

		} catch (SQLException e) {
			Message.genericErrorSystem(e);
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void checkDatabase(String name, String uuid) {
		try {
			if(connection == null || connection.isClosed()) {
				 this.connection = dbConn.getConnection();
			}
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String query = "SELECT username FROM User WHERE UUID = (?)";
		try {
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
				    try {
				    	ps = connection.prepareStatement(query);
				    	ps.setQueryTimeout(5);
						ps.setString(1, name);
						ps.setString(2, uuid);
						ps.executeUpdate();
				    } catch(SQLException e) {
						Message.databaseError(e);
				    }
				}
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
		DatabaseConnection.cleanConnection(ps, rs);
		
	}
	
	@SuppressWarnings("deprecation")
	public synchronized void getRep(Player player, String username, int page) {
		try {
			if(connection == null || connection.isClosed()) {
				 this.connection = dbConn.getConnection();
			}
		} catch (SQLException e) {
			Message.databaseError(e);
		}
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		String uuid = null;
		String query;
		if(Bukkit.getServer().getPlayer(username) != null) {
			uuid = Bukkit.getServer().getPlayer(username).getUniqueId().toString();
		} else {
			OfflinePlayer op = Bukkit.getOfflinePlayer(username);
			if (op.hasPlayedBefore()) {
			    uuid = op.getUniqueId().toString();
			} else {
				try {
					uuid = UUIDFetcher.getUUIDOf(username).toString();
					query = "SELECT userId FROM User WHERE UUID = (?)";
					try {
						ps = connection.prepareStatement(query);
						ps.setQueryTimeout(5);
						ps.setString(1, uuid);
						rs = ps.executeQuery();
						if(!rs.next()) {
							Message.noPlayer(player);
							return;
						}
					} catch (Exception e) {
						Message.databaseError(e);
						Message.genericErrorPlayer(player);
					}
					
					
				} catch(Exception e) {
					Message.noPlayer(player);
					return;
				}
			}
		}
		
		int resultAmount = page * 10;
		query = "SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))";
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
			if(!rs.next()) {
				Message.noRep(player, username);
				return;
			} else {
				List<Rep> reps = Collections.synchronizedList( new ArrayList<Rep>());
				query = "SELECT r.date, r.repAmount, r.comment, u.username FROM Rep r JOIN  User u ON r.giverId = u.userId WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) ORDER BY repId DESC";
				
				try {
					ps = connection.prepareStatement(query);
					ps.setQueryTimeout(5);
					ps.setString(1, uuid);
					rs = ps.executeQuery();
					while(rs.next()){
						Rep rep = new Rep(rs.getInt("r.repAmount"), rs.getString("r.date"), rs.getString("u.username"), rs.getString("r.comment"));
						reps.add(rep);
					}
					int totalPages = (reps.size() + 10 - 1) / 10;
					if(page <= totalPages && page > 0) {
						Message.repHeader(player, username);
						if(reps.size()-resultAmount <= 0) {
							if(page == 1) {
								for(int i = reps.size()-1; i >= 0; i--) {
									if(reps.get(i).getAmount() > 0){
										Message.repPositive(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
									} else {
										Message.repNegative(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
									}
								}
							} else {
								int remaining = reps.size()%(resultAmount-11);
								for(int i = reps.size()-1; i > reps.size()-remaining; i--) {
									if(reps.get(i).getAmount() > 0){
										Message.repPositive(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
									} else {
										Message.repNegative(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
									}
								}
							}
						} else {
							for(int i = resultAmount-1; i >= resultAmount-10; i--) {
								if(reps.get(i).getAmount() > 0){
									Message.repPositive(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
								} else {
									Message.repNegative(player, reps.get(i).getAmount(), reps.get(i).getDate(), reps.get(i).getUsername(), reps.get(i).getComment());
								}
							}
						}
						
						Message.navigate(player, username, page, totalPages);
						
						int positiveRep = 0;
						int negativeRep = 0;
						for(int i = 0; i < reps.size(); i++){
							if(reps.get(i).getAmount() > 0) {
								positiveRep += reps.get(i).getAmount();
							} else if (reps.get(i).getAmount() < 0) {
								negativeRep += reps.get(i).getAmount();
							}
						}
						
						if(positiveRep + negativeRep > 0) {
							Message.repTotalPositive(player, positiveRep, negativeRep);
						} else if(positiveRep + negativeRep < 0) {
							Message.repTotalNegative(player, positiveRep, negativeRep);
						} else {
							Message.repTotalZero(player, positiveRep, negativeRep);
						}
						Message.repFooter(player);
					} else {
						Message.pageOutOfBounds(player);
					}
				} catch (SQLException e) {
					Message.databaseError(e);
					Message.genericErrorPlayer(player);
				} catch (Exception e) {
					Message.genericErrorSystem(e);
				}
			}
		} catch (SQLException e) {
			Message.databaseError(e);
			Message.genericErrorPlayer(player);
		}
		DatabaseConnection.cleanConnection(ps, rs);
		return;
	}
	
	public synchronized void addRep(String[] args, Player player, int rep) {
		if(player.getName().equalsIgnoreCase(args[0])) {
			Message.repSelf(player);
			return;
		}
		
		try {
			if(connection == null || connection.isClosed()) {
				 this.connection = dbConn.getConnection();
			}
		} catch (SQLException e) {
			Message.databaseError(e);
			Message.genericErrorPlayer(player);
		}
		
		String comment = "";	
		if(args.length >= 3) {
			StringBuilder builder = new StringBuilder();
			for(int i = 2; i < args.length; i++){
				builder.append(args[i]);
				builder.append(" ");	
			}
			comment = builder.toString();
		}
		try {
			PreparedStatement ps = null;
			ResultSet rs = null;
			

			String query = "SELECT repId FROM Rep WHERE giverId = "
					+ "(SELECT userId FROM User WHERE UUID = ?)"
					+ " AND userId = "
					+ "(SELECT userId FROM User WHERE UUID = "
					+ "(SELECT uuid FROM User WHERE username = ?))";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, player.getUniqueId().toString());
				ps.setString(2, args[0]);
				rs = ps.executeQuery();
				
			} catch (SQLException e) {
				Message.databaseError(e);
				Message.genericErrorPlayer(player);
			}
			if(rs.next()){
				int repId = rs.getInt("repId");
				String delete = "DELETE FROM Rep WHERE repId = ?";
				try {
					ps = connection.prepareStatement(delete);
					ps.setQueryTimeout(5);
					ps.setInt(1, repId);
					ps.executeUpdate();
				} catch (SQLException e) {
					Message.databaseError(e);
					Message.genericErrorPlayer(player);
				}
			}
			LocalDateTime currentDate = LocalDateTime.now();
			String date = currentDate.toString();
			String[] time = date.split("T");
			date = time[0];
			String giverUUID = player.getUniqueId().toString();
			
			query = "INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES "
					+ "(?, ?, (SELECT userId FROM User WHERE uuid = (?)), ?, (SELECT userId FROM User WHERE uuid = (SELECT uuid FROM User WHERE username = ?)))";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(5);
				ps.setString(1, date);
				ps.setInt(2, rep);
				ps.setString(3, giverUUID);
				ps.setString(4, comment);
				ps.setString(5, args[0]);
				ps.executeUpdate();
					
				Message.repAddedOther(player, args[0]);
				try {
					Player reciever = Bukkit.getPlayer(args[0]);
					Message.repAddedSelf(reciever);
				} catch(NullPointerException e) {
					//no handling necessary if player isn't online
				}
			} catch (SQLException e) {
				Message.noPlayer(player);
			} catch(NullPointerException e) {
				Message.genericErrorSystem(e);
			}
			DatabaseConnection.cleanConnection(ps, rs);
		} catch (SQLException e) {
			Message.genericErrorSystem(e);
		}
		return;
	}
	
	public synchronized void deleteRep(Player player, String[] args) {
		try {
			if(connection == null || connection.isClosed()) {
				 this.connection = dbConn.getConnection();
			}
		} catch (SQLException e) {
			Message.databaseError(e);
			Message.genericErrorPlayer(player);
		}
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		String query = "SELECT repId FROM Rep WHERE giverId = "
				+ "(SELECT userId FROM User WHERE UUID = "
				+ "(SELECT uuid FROM User WHERE username = ?)) AND userId = "
				+ "(SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?))";
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(5);
			ps.setString(1, args[2]);
			ps.setString(2, args[1]);
			rs = ps.executeQuery();
			if(rs.next()) {
				int repId = rs.getInt("repId");
				String delete = "DELETE FROM Rep WHERE repId = ?";
				try {
					ps = connection.prepareStatement(delete);
					ps.setQueryTimeout(5);
					ps.setInt(1, repId);
					ps.executeUpdate();
					Message.repRemoved(player);
				} catch (SQLException e) {
					Message.databaseError(e);
					Message.genericErrorPlayer(player);
				}
			} else {
				Message.noRecord(player);
			}
		} catch (SQLException e) {
			Message.noRecord(player);
		}
		DatabaseConnection.cleanConnection(ps, rs);
	}
}
