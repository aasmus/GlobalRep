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
package com.austinasmus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import com.evilmidget38.UUIDFetcher;

public class AccessDatabase {

	private String DB_IP;
	private int DB_PORT;
	private String DB_NAME;
	private String user;
	private String pass;
	private Connection connection;

	public AccessDatabase(String databaseIp, int databasePort, String databaseName, String username, String password) {
		this.DB_IP = databaseIp;
		this.DB_PORT = databasePort;
		this.DB_NAME = databaseName;
		this.user = username;
		this.pass = password;
		this.connection = getConnection();
	}

	private Connection getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			System.out.println("InstantiationException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			System.out.println("IllegalAccessException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Connection conn = null;

		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + this.DB_IP + ":" + this.DB_PORT + "/" + this.DB_NAME, this.user, this.pass);

		} catch (SQLException e) {
			System.out.println("SQLException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return conn;
	}
	
	public void generateTables() {
		try {
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'User'";
			
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("[GlobalRep] An Error occured. Is the database down?");
			}
			String table;
			if(!rs.next()){
				table = "CREATE TABLE User "
				+ "(userId INT auto_increment NOT NULL PRIMARY KEY, "
				+ "UUID varchar(40) NOT NULL UNIQUE, "
				+ "username varchar(16) NOT NULL)";

				try {
					ps = connection.prepareStatement(table);
					ps.setQueryTimeout(15);
					ps.executeUpdate();
					System.out.println("[GlobalRep] User table created");
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("[GlobalRep] An Error occured. Is the database down?");
				}
			}
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'Rep'";
			
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("[GlobalRep] An Error occured. Is the database down?");
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
					ps.setQueryTimeout(15);
					ps.executeUpdate();
					System.out.println("[GlobalRep] Rep table created");
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("[GlobalRep] An Error occured. Is the database down?");
				}
			}		
			rs.close();
			ps.close();

		} catch (SQLException e) {
			System.out.println("SQLException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void checkDatabase(String name, String uuid) {
		try {
			if(connection == null || connection.isClosed()) {
				  getConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String query = "SELECT username FROM User WHERE UUID = (?)";
		try {
			ps = connection.prepareStatement(query);
			ps.setQueryTimeout(15);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
			if(!rs.next()) {
				query = "INSERT INTO User (UUID, username) VALUES (?, ?)";
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, uuid);
				ps.setString(2, name);
				ps.executeUpdate();
			} else {
				if(!rs.getString("username").equals(name)) {
					query = "UPDATE User SET username = ? WHERE UUID = ?";
				    try {
				    	ps = connection.prepareStatement(query);
				    	ps.setQueryTimeout(15);
						ps.setString(1, name);
						ps.setString(2, uuid);
						ps.executeUpdate();
				    } catch(SQLException se) {
				        se.printStackTrace();
						System.out.println("[GlobalRep] An Error occured. Is the database down?");
				    }
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("[GlobalRep] An Error occured. Is the database down?");
		}
		
		try {
			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public synchronized void getRep(Player player, String username) {
		try {
			if(connection == null || connection.isClosed()) {
				  getConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
			System.out.println("[GlobalRep] An Error occured. Is the database down?");
		}
		
		try {
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
							ps.setQueryTimeout(15);
							ps.setString(1, uuid);
							rs = ps.executeQuery();
							if(!rs.next()) {
								player.sendMessage(ChatColor.RED + "That player does not exist!");
								return;
							}
						} catch (Exception e) {
							e.printStackTrace();
							player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
							System.out.println("[GlobalRep] An Error occured. Is the database down?");
						}
						
						
					} catch(Exception e) {
						player.sendMessage(ChatColor.RED + "That player does not exist!");
						return;
					}
				}
			}
				
			query = "SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, uuid);
				rs = ps.executeQuery();
				if(!rs.next()) {
					player.sendMessage(ChatColor.BLUE + username + " has no rep!");
					return;
				} else {
					List<String> dates = Collections.synchronizedList( new ArrayList<String>());
					List<Integer> reps = Collections.synchronizedList( new ArrayList<Integer>());
					List<String> comments = Collections.synchronizedList( new ArrayList<String>());
					List<String> names = Collections.synchronizedList( new ArrayList<String>());
					query = "SELECT r.date, r.repAmount, r.comment, u.username FROM Rep r JOIN  User u ON r.giverId = u.userId WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?)";
					
					try {
						ps = connection.prepareStatement(query);
						ps.setQueryTimeout(15);
						ps.setString(1, uuid);
						rs = ps.executeQuery();
						while(rs.next()){
							String date = rs.getString("r.date");
							int repAmount = rs.getInt("r.repAmount");
							String comment = rs.getString("r.comment");
							String name = rs.getString("u.username");
							dates.add(date);
							reps.add(repAmount);
							comments.add(comment);
							names.add(name);							
							}
						player.sendMessage(ChatColor.BLUE + username + "'s reputation:");
						for(int i = 0; i < dates.size(); i++) {
							if(reps.get(i) > 0){
								player.sendMessage(ChatColor.GREEN + "+" + reps.get(i) + " " + ChatColor.GRAY + dates.get(i) + " " + ChatColor.YELLOW + names.get(i) + ": " + ChatColor.RESET + comments.get(i));
							} else {
								player.sendMessage(ChatColor.RED + "" + reps.get(i) + " " + ChatColor.GRAY + dates.get(i) + " " + ChatColor.YELLOW + names.get(i) + ": " + ChatColor.RESET + comments.get(i));
							}
						}
						
						int positiveRep = 0;
						int negativeRep = 0;
						for(int i = 0; i < reps.size(); i++){
							if(reps.get(i) > 0) {
								positiveRep += reps.get(i);
							} else if (reps.get(i) < 0) {
								negativeRep += reps.get(i);
							}
						}
						player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep));
						
						if(positiveRep + negativeRep > 0) {
							player.sendMessage(ChatColor.AQUA + "Total Reputation: " + ChatColor.GREEN + (positiveRep + negativeRep));
						} else if(positiveRep + negativeRep < 0) {
							player.sendMessage(ChatColor.AQUA + "Total Reputation: " + ChatColor.RED + (positiveRep + negativeRep));
						} else {
							player.sendMessage(ChatColor.AQUA + "Total Reputation: " + ChatColor.GRAY + (positiveRep + negativeRep));
						}
					} catch (SQLException e) {
						e.printStackTrace();
						player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
						System.out.println("[GlobalRep] An Error occured. Is the database down?");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
				System.out.println("[GlobalRep] An Error occured. Is the database down?");
			}
			ps.close();
			rs.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			return;
				
		}
		return;
	}
	
	public synchronized void addRep(String[] args, Player player, int rep) {
		if(player.getName().equalsIgnoreCase(args[0])) {
			player.sendMessage(ChatColor.RED + "You cannot give yourself rep.");
			return;
		}
		
		try {
			if(connection == null || connection.isClosed()) {
				  getConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
			System.out.println("[GlobalRep] An Error occured. Is the database down?");
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
				ps.setQueryTimeout(15);
				ps.setString(1, player.getUniqueId().toString());
				ps.setString(2, args[0]);
				rs = ps.executeQuery();
				
			} catch (SQLException e) {
				e.printStackTrace();
				player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
				System.out.println("[GlobalRep] An Error occured. Is the database down?");
			}
			if(rs.next()){
				int repId = rs.getInt("repId");
				String delete = "DELETE FROM Rep WHERE repId = ?";
				try {
					ps = connection.prepareStatement(delete);
					ps.setQueryTimeout(15);
					ps.setInt(1, repId);
					ps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
					player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
					System.out.println("[GlobalRep] An Error occured. Is the database down?");
				}
			}
			LocalDateTime currentDate = LocalDateTime.now();
			String date = currentDate.toString();
			String[] time = date.split("T");
			date = time[0];
			String giverUUID = player.getUniqueId().toString();
			
			query = "INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES (?, ?, (SELECT userId FROM User WHERE uuid = (?)), ?, (SELECT userId FROM User WHERE uuid = (SELECT uuid FROM User WHERE username = ?)))";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, date);
				ps.setInt(2, rep);
				ps.setString(3, giverUUID);
				ps.setString(4, comment);
				ps.setString(5, args[0]);
				ps.executeUpdate();
					
				player.sendMessage(ChatColor.BLUE + "Reputation added! You can use /rep " + args[0] + " to see it.");
				try {
					Player reciever = Bukkit.getPlayer(args[0]);
					reciever.sendMessage(ChatColor.BLUE + "Your reputation has changed! View your rep with /rep " + reciever.getName() + ".");
				} catch(NullPointerException npe) {
					//no handling necessary if player isn't online
				}
			} catch (SQLException e) {
				player.sendMessage(ChatColor.RED + "That player does not exist!");
			} catch(NullPointerException npe) {
				npe.printStackTrace();
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return;
	}
	
	public synchronized void deleteRep(Player player, String[] args) {
		try {
			if(connection == null || connection.isClosed()) {
				  getConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
			System.out.println("[GlobalRep] An Error occured. Is the database down?");
		}
		
		try{
			PreparedStatement ps = null;
			ResultSet rs = null;
			String query = "SELECT repId FROM Rep WHERE giverId = (SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?)) AND userId = (SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?))";
			try {
				ps = connection.prepareStatement(query);
				ps.setQueryTimeout(15);
				ps.setString(1, args[2]);
				ps.setString(2, args[1]);
				rs = ps.executeQuery();
				if(rs.next()) {
					int repId = rs.getInt("repId");
					String delete = "DELETE FROM Rep WHERE repId = ?";
					try {
						ps = connection.prepareStatement(delete);
						ps.setQueryTimeout(15);
						ps.setInt(1, repId);
						ps.executeUpdate();
						player.sendMessage(ChatColor.RED + "Reputaton record deleted.");
					} catch (SQLException e) {
						e.printStackTrace();
						player.sendMessage(ChatColor.RED + "An error has occured. Please tell a server administrator.");
						System.out.println("[GlobalRep] An Error occured. Is the database down?");
					}
				} else {
					player.sendMessage(ChatColor.RED + "That rep record doesn't exist!");
				}
			} catch (SQLException e) {
				player.sendMessage(ChatColor.RED + "That rep record doesn't exist!");
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
