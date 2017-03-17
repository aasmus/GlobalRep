package com.austinasmus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
				+ "UUID varchar(40) NOT NULL UNIQUE)";

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
						+ "giverUUID varchar(40) NOT NULL, "
						+ "comment varchar(255), "
						+ "userId INT NOT NULL, "
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
	
	public void getRep(Player player, String username) {
		UUID uid = null;
		String uuid = null;
		try {
			uid = UUIDFetcher.getUUIDOf(username);
			uuid = uid.toString();
		} catch(Exception e) {
			player.sendMessage(ChatColor.GOLD + "That player does not exist!");
			return;
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
			
			String testquery = "SELECT userId FROM User WHERE UUID = ?";
			try {
				ps = conn.prepareStatement(testquery);
				ps.setString(1, uuid);
				rs = ps.executeQuery();
				if(!rs.next()) {
					player.sendMessage(ChatColor.BLUE + username + " has no rep!");
					return;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			ArrayList<String> dates = new ArrayList<String>();
			ArrayList<Integer> reps = new ArrayList<Integer>();
			ArrayList<String> comments = new ArrayList<String>();
			ArrayList<String> names = new ArrayList<String>();
			
			String query = "SELECT date, repAmount, comment, giverUUID FROM Rep WHERE userId = (SELECT userId FROM User WHERE uuid = ?)";
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, uuid);
				rs = ps.executeQuery();
				
				while(rs.next()){
					String date = rs.getString("date");
					int repAmount = rs.getInt("repAmount");
					String comment = rs.getString("comment");
					String giverUUID = rs.getString("giverUUID");
					List<UUID> uuids = new ArrayList<UUID>();
					uuids.add(UUID.fromString(giverUUID));
					NameFetcher fetcher = new NameFetcher(uuids);
					Map<UUID, String> giverName = fetcher.call();
					
					dates.add(date);
					reps.add(repAmount);
					comments.add(comment);
					for (String name : giverName.values()) {
						names.add(name);
					}

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
				player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + negativeRep);
				player.sendMessage(ChatColor.GRAY + "Total Rep: " + (positiveRep + negativeRep));
				
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (Exception e) {
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
	
	public void addRep(String[] args, Player player, int rep) {
		if(player.getName().equalsIgnoreCase(args[0])) {
			player.sendMessage(ChatColor.RED + "You cannot give yourself rep.");
			return;
		}
		
		Player giver = player;
		String comment = "";	
		if(args.length >= 3) {
			StringBuilder builder = new StringBuilder();
			for(int i = 2; i < args.length; i++){
				builder.append(args[i]);
				builder.append(" ");	
			}
			comment = builder.toString();
		}
		ArrayList<String> name = new ArrayList<String>();
		name.add(args[0]);
		UUID uid = null;
		try {
			uid = UUIDFetcher.getUUIDOf(args[0]);
		} catch(Exception e) {
			player.sendMessage(ChatColor.RED + args[0] +" is not a valid username.");
			return;
		}
		String uuid = uid.toString();
		
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
			int userId = 0;

			String insert;
				insert = "INSERT INTO User (UUID) VALUES (?)";
				try {
					ps = conn.prepareStatement(insert);
					ps.setString(1, uuid);
					ps.executeUpdate();
				} catch (SQLException e) {
				}
			String query = "SELECT userId FROM User WHERE UUID = ?";
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, uuid);
				rs = ps.executeQuery();
				if(rs.next()) {
					userId = rs.getInt("userId");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			int repId = 0;
			query = "SELECT repId FROM Rep WHERE giverUUID = ? AND userId = ?";
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, giver.getUniqueId().toString());
				ps.setInt(2, userId);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			if(rs.next()){
				repId = rs.getInt("repId");
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
		
			insert = "INSERT INTO Rep (date, repAmount, giverUUID, comment, userId) VALUES (?, ?, ?, ?, ?)";
			try {
				ps = conn.prepareStatement(insert);
				ps.setString(1, date);
				ps.setInt(2, rep);
				ps.setString(3, giver.getUniqueId().toString());
				ps.setString(4, comment);
				ps.setInt(5, userId);
				ps.executeUpdate();
				
				Player reciever = Bukkit.getPlayer(args[0]);
				player.sendMessage(ChatColor.BLUE + "Reputation added! You can use /rep " + args[0] + " to see it.");
				try {
				reciever.sendMessage(ChatColor.BLUE + "Your reputation has changed! View it with /rep " + reciever.getName());
				} catch(NullPointerException npe) {
					rs.close();
					ps.close();
					return;
				}
				
			} catch (SQLException e) {
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
