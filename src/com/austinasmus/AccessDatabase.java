package com.austinasmus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

	public AccessDatabase(String databaseIp, int databasePort, String databaseName, String username, String password) {
		this.DB_IP = databaseIp;
		this.DB_PORT = databasePort;
		this.DB_NAME = databaseName;
		this.user = username;
		this.pass = password;
	}

	public void generateTables() {
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
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'User'";
			
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			String table;
			if(!rs.next()){
				table = "CREATE TABLE User "
				+ "(userId INT auto_increment NOT NULL PRIMARY KEY, "
				+ "UUID varchar(40) NOT NULL UNIQUE, "
				+ "username varchar(16) NOT NULL)";

				try {
					ps = conn.prepareStatement(table);
					ps.executeUpdate();
					System.out.println("[GlobalRep] User table created");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'Rep'";
			
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
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
							ps = conn.prepareStatement(table);
							ps.executeUpdate();
							System.out.println("[GlobalRep] Rep table created");
						} catch (SQLException e) {
							e.printStackTrace();
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
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			String query = "SELECT username FROM User WHERE UUID = ?";
			String insert;
			
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, uuid);
				rs = ps.executeQuery();
				if(!rs.next()) {
					insert = "INSERT INTO User (UUID, username) VALUES (?, ?)";
					ps = conn.prepareStatement(insert);
					ps.setString(1, uuid);
					ps.setString(2, name);
					ps.executeUpdate();
				} else if(rs.next()) {
					if(!rs.getString("username").equals(name)) {
						insert = "INSERT INTO User (username) VALUES ? WHERE UUID = (?)";
						
					    try(PreparedStatement ps1 = conn.prepareStatement(insert);) {
							ps.setString(1, name);
							ps.setString(2, uuid);

					    } catch(SQLException se) {
					        se.printStackTrace();
					    }
					}
				}
				
				ps.close();
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		} catch (SQLException e) {
			System.out.println("SQLException: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public synchronized void getRep(Player player, String username) {
			String uuid = null;
			if(Bukkit.getServer().getPlayer(username) != null) {
				uuid = Bukkit.getServer().getPlayer(username).getUniqueId().toString();
			} else {
				OfflinePlayer op = Bukkit.getOfflinePlayer(username);
				if (op.hasPlayedBefore()) {
				    uuid = op.getUniqueId().toString();
				} else {
					try {
						uuid = UUIDFetcher.getUUIDOf(username).toString();
					} catch(Exception e) {
						player.sendMessage(ChatColor.RED + "That player does not exist!");
						return;
					}
				}
			}
			
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
				PreparedStatement ps = null;
				ResultSet rs = null;
				
				
				String query = "SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))";
				try {
					ps = conn.prepareStatement(query);
					ps.setString(1, uuid);
					rs = ps.executeQuery();
					if(!rs.next()) {
						player.sendMessage(ChatColor.BLUE + username + " has no rep!");
						return;
					} else {
						ArrayList<String> dates = new ArrayList<String>();
						ArrayList<Integer> reps = new ArrayList<Integer>();
						ArrayList<String> comments = new ArrayList<String>();
						ArrayList<String> names = new ArrayList<String>();
						query = "SELECT r.date, r.repAmount, r.comment, u.username FROM Rep r JOIN  User u ON r.giverId = u.userId WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?)";
						try {
							ps = conn.prepareStatement(query);
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
						} catch (Exception e) {
							e.printStackTrace();
						}
						
					}
				} catch (SQLException e) {
					e.printStackTrace();
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
			PreparedStatement ps = null;
			ResultSet rs = null;
			
		try {
			String query = "SELECT uuid FROM User WHERE username = ?";
			ps = conn.prepareStatement(query);
			ps.setString(1, args[0]);
			rs = ps.executeQuery();
			if(!rs.next()) {
				player.sendMessage(ChatColor.RED + args[0] +" has never joined.");
				return;
			} else {
				String uuid = rs.getString("UUID");
				
				query = "SELECT userId FROM User WHERE UUID = ?";
				try {
					ps = conn.prepareStatement(query);
					ps.setString(1, uuid);
					rs = ps.executeQuery();
					if(rs.next()) {
						int userId = rs.getInt("userId");
						
						query = "SELECT userId FROM User WHERE UUID = ?";
						try {
							ps = conn.prepareStatement(query);
							ps.setString(1, player.getUniqueId().toString());
							rs = ps.executeQuery();
							int giverId = 0;
							if(rs.next()){
								giverId = rs.getInt("userId");
								}
							
							query = "SELECT repId FROM Rep WHERE giverId = ? AND userId = ?";
							try {
								ps = conn.prepareStatement(query);
								ps.setInt(1, giverId);
								ps.setInt(2, userId);
								rs = ps.executeQuery();
							} catch (SQLException e) {
								e.printStackTrace();
							}
							
							if(rs.next()){
								int repId = rs.getInt("repId");
								String delete = "DELETE FROM Rep WHERE repId = ?";
								try {
									ps = conn.prepareStatement(delete);
									ps.setInt(1, repId);
									ps.executeUpdate();
								} catch (SQLException e) {
									e.printStackTrace();
								}
							}
							
							LocalDateTime currentDate = LocalDateTime.now();
							String date = currentDate.toString();
							String[] time = date.split("T");
							date = time[0];
						
							String insert = "INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES (?, ?, ?, ?, ?)";
							try {
								ps = conn.prepareStatement(insert);
								ps.setString(1, date);
								ps.setInt(2, rep);
								ps.setInt(3, giverId);
								ps.setString(4, comment);
								ps.setInt(5, userId);
								ps.executeUpdate();
								
								Player reciever = Bukkit.getPlayer(args[0]);
								player.sendMessage(ChatColor.BLUE + "Reputation added! You can use /rep " + args[0] + " to see it.");
								try {
								reciever.sendMessage(ChatColor.BLUE + "Your reputation has changed! View your rep with /rep " + reciever.getName() + ".");
								} catch(NullPointerException npe) {
									rs.close();
									ps.close();
									return;
								}
								
							} catch (SQLException e) {
								e.printStackTrace();
							}
							
						} catch (SQLException e) {
							e.printStackTrace();
						}
						
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
			rs.close();
			ps.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		
		return;
	}

}
