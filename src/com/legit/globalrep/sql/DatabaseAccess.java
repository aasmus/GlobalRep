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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.legit.globalrep.chat.Message;
import com.legit.globalrep.commands.RepCommand.CallbackBoolean;
import com.legit.globalrep.commands.RepCommand.CallbackInt;
import com.legit.globalrep.commands.RepCommand.CallbackRep;
import com.legit.globalrep.object.Rep;

public class DatabaseAccess {
	private final String DB_NAME;
	private DatabaseConnection dbConn;
	private Connection connection;
	private Plugin plugin;
	final private BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

	public DatabaseAccess(String databaseIp, int databasePort, String databaseName, String username, String password, Plugin plugin) {
		this.DB_NAME = databaseName;
		dbConn = new DatabaseConnection(databaseIp, databasePort, databaseName, username, password);
		connection = dbConn.getConnection();
		this.plugin = plugin;
	}
	
	/**
	 * createTable: Called to create MySQL tables, if they don't already exist
	 * 
	 * @param name - name of the MySQL table to be created
	 */
	public void createTable(String name) {
		this.connection = dbConn.checkConnection(connection);
		if(connection == null) {
			return;
		}
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
        		String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        		try {
        			PreparedStatement ps = connection.prepareStatement(query);
        			ps.setQueryTimeout(5);
        			ps.setString(1, DB_NAME);
        			ps.setString(2, name);
        			ResultSet rs = ps.executeQuery();
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
        			ps.close();
        			rs.close();
        		} catch (SQLException e) {
        			Message.genericErrorSystem(e);
        		}
            }
        });
	}
	
	/**
	 * checkDatabase: used to check database for an existing UUID/username combo
	 * 
	 * @param name - username of player having username checked
	 * @param uuid - UUID of player having username checked
	 */
	public void checkDatabase(String name, String uuid) {
		this.connection = dbConn.checkConnection(connection);
		if(connection == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
		    @Override
		    public void run() {
				try {
					String query = "SELECT userId, UUID FROM User where username = (?)";
					PreparedStatement ps = connection.prepareStatement(query);
					ps.setString(1, name);
					ps.setQueryTimeout(5);
					ResultSet rs = ps.executeQuery();
					while(rs.next()) {
						if(!rs.getString("UUID").equals(uuid)) {
							query = "DELETE FROM Rep WHERE userId = ?";
							ps = connection.prepareStatement(query);
						    ps.setQueryTimeout(5);
							ps.setInt(1, rs.getInt("userId"));
							ps.executeUpdate();
							query = "DELETE FROM User WHERE UUID = ?";
							ps = connection.prepareStatement(query);
						    ps.setQueryTimeout(5);
							ps.setString(1, rs.getString("UUID"));
							ps.executeUpdate();
						}
					}
					query = "SELECT username FROM User WHERE UUID = (?)";
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
					ps.close();
					rs.close();
				} catch (SQLException e) {
					Message.databaseError(e);
				}
		    }
		});
	}
	
	/**
	 * GetRep: used to retrieve a player's reputation records from the database
	 * 
	 * @param player - command sender
	 * @param username - name of the player who's rep is being looked up
	 * @param uuid - UUID of the player who's rep is being looked up
	 * @param page - rep page # that command sender is currently on
	 */
	public void getRep(Player player, String username, UUID uuid, int page, CallbackRep cr) {
		this.connection = dbConn.checkConnection(connection);
		if (connection == null) {
			return;
		}
		getUserId(uuid, new CallbackInt() {

			@Override
			public void onQueryDone(int userId) {
					if (userId == 0) {
						Message.noRep(player, username);
					} else {
						Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
							@Override
							public void run() {
								try {
									PreparedStatement ps = connection
											.prepareStatement("SELECT r.date, r.repAmount, r.comment, u.username "
													+ "FROM Rep r JOIN  User u ON r.giverId = u.userId "
													+ "WHERE r.userId = (SELECT userId FROM User WHERE uuid = ?) "
													+ "ORDER BY repId DESC");
									ps.setQueryTimeout(5);
									ps.setString(1, uuid.toString());
									ResultSet rs = ps.executeQuery();
									List<Rep> reps = Collections.synchronizedList(new ArrayList<Rep>());
									int numRep = 0;
									while (rs.next()) {
										Rep rep = new Rep(rs.getInt("r.repAmount"), rs.getString("r.date"),
												rs.getString("u.username"), rs.getString("r.comment"));
										reps.add(rep);
										numRep++;
									}
									int totalPages = (numRep + 10 - 1) / 10;
									ps.close();
									rs.close();
									scheduler.runTask(plugin, new Runnable() {
										@Override
										public void run() {
											cr.onQueryDone(reps, totalPages);
										}
									});
								} catch (Exception e) {
									Message.genericErrorSystem(e);
								}
							}
						});
					}
			}
		});
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
		this.connection = dbConn.checkConnection(connection);
		
		getRepIdbyUUID(player.getUniqueId(), username, new CallbackInt() {

			@Override
			public void onQueryDone(int repId) {
				if (repId != 0) {
					removeRep(repId);
				}

				scheduler.runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						LocalDateTime currentDate = LocalDateTime.now();
						String date = currentDate.toString();
						String[] time = date.split("T");
						date = time[0];
						try {
							PreparedStatement ps = connection.prepareStatement(
									"INSERT INTO Rep (date, repAmount, giverId, comment, userId) VALUES "
											+ "(?, ?, (SELECT userId FROM User WHERE uuid = (?)), ?, "
											+ "(SELECT userId FROM User WHERE uuid = "
											+ "(SELECT uuid FROM User WHERE username = ?)))");
							ps.setQueryTimeout(5);
							ps.setString(1, date);
							ps.setInt(2, rep);
							ps.setString(3, player.getUniqueId().toString());
							ps.setString(4, comment);
							ps.setString(5, username);
							ps.executeUpdate();
							ps.close();
						} catch (SQLException e) {
							Message.noPlayer(player);
						} catch (NullPointerException e) {
							Message.genericErrorSystem(e);
						}
					}
				});

				Message.repAddedOther(player, username);
				try {
					Player reciever = Bukkit.getPlayer(username);
					Message.repAddedSelf(reciever);
				} catch (NullPointerException e) {
					// no handling necessary if player isn't online
				}
			}
		});
	}

	/**
	 * checkRepId: used to check of a reputation record exists or not
	 * 
	 * @param repId - repId from the "Rep" table
	 */
	public void checkRepId(int repId, CallbackBoolean cb) {
		this.connection = dbConn.checkConnection(connection);

		scheduler.runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					PreparedStatement ps = connection.prepareStatement("SELECT repId FROM Rep WHERE repId = ?");
					ps.setQueryTimeout(5);
					ps.setInt(1, repId);
					ResultSet rs = ps.executeQuery();
					boolean bool;
					if (rs.next()) {
						bool = true;
					} else {
						bool = false;
					}
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							cb.onQueryDone(bool);
						}
					});
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	/**
	 * removeRep: used to remove a reputation record from the database
	 * 
	 * @param repId - repId from the "Rep" table
	 */
	public void removeRep(int repId) {
		this.connection = dbConn.checkConnection(connection);

		scheduler.runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					PreparedStatement ps = connection.prepareStatement("DELETE FROM Rep WHERE repId = ?");
					ps.setInt(1, repId);
					ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * hasLoggedIn: used to see if a player has logged in previously by checking the database
	 * 
	 * @param uuid - UUID of player who is being checked for previous logins
	 * @return - true = has logged in, false = hasn't logged in
	 */
	public void hasLoggedIn(UUID uuid, CallbackBoolean cb) {
		this.connection = dbConn.checkConnection(connection);

		scheduler.runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					PreparedStatement ps = connection.prepareStatement("SELECT userId FROM User WHERE UUID = (?)");
					ps.setQueryTimeout(5);
					ps.setString(1, uuid.toString());
					ResultSet rs = ps.executeQuery();
					boolean bool;
					if (rs.next()) {
						bool = true;
					} else {
						bool = false;
					}
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							cb.onQueryDone(bool);
						}
					});
					ps.close();
					rs.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * getUserId - used to get the userId of a player from the "User" table
	 * 
	 * @param uuid - UUID of the player who's userId is being looked up
	 * @return - returns a userId or 0 if not found
	 */
	private void getUserId(UUID uuid, CallbackInt ci) {
		this.connection = dbConn.checkConnection(connection);

		scheduler.runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					int userId;
					PreparedStatement ps = connection.prepareStatement(
							"SELECT userId FROM Rep WHERE userId = (SELECT userId FROM User WHERE UUID = (?))");
					ps.setQueryTimeout(5);
					ps.setString(1, uuid.toString());
					ResultSet rs = ps.executeQuery();
					rs.next();
					userId = rs.getInt("userId");
					ps.close();
					rs.close();
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							ci.onQueryDone(userId);
						}
					});
				} catch (Exception e) {
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							ci.onQueryDone(0);
						}
					});
				}
			}
		});
	}
	
	/**
	 * getRepIdByUUID - used to get a repId based on giver's UUID
	 * 
	 * @param uuid - UUID of player who gave the rep
	 * @param username - username of player who received the rep
	 * @return - returns repId if exists or 0 if doesn't exist
	 */
	private void getRepIdbyUUID(UUID uuid, String username, CallbackInt ci) {
		this.connection = dbConn.checkConnection(connection);

        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
        		try {
        			PreparedStatement ps = connection.prepareStatement("SELECT repId FROM Rep WHERE giverId = "
        					+ "(SELECT userId FROM User WHERE UUID = ?)"
        					+ " AND userId = "
        					+ "(SELECT userId FROM User WHERE UUID = "
        					+ "(SELECT uuid FROM User WHERE username = ?))");
        			ps.setQueryTimeout(5);
        			ps.setString(1, uuid.toString());
        			ps.setString(2, username);
        			ResultSet rs = ps.executeQuery();
        			int repId;
        			if(rs.next()){
        				repId = rs.getInt("repId");
        			} else {
        				repId = 0;
        			}
            		scheduler.runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            ci.onQueryDone(repId);
                        }
                    });
        			ps.close();
        			rs.close();
        		} catch (SQLException e) {
        			Message.databaseError(e);
        		}
            }
        });
	}
	
	/**
	 * getrepIdByUsername - used to get a repId by giver's username
	 * 
	 * @param player - command sender
	 * @param reciever - player who recieved rep
	 * @param giver - player who gave rep
	 * @return - returns repId if exists or 0 if doesn't exist
	 */
	public void getrepIdByUsername(Player player, String reciever, String giver, CallbackInt ci) {
		this.connection = dbConn.checkConnection(connection);

        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
        		try {
        			PreparedStatement ps = connection.prepareStatement("SELECT repId FROM Rep WHERE giverId = "
        					+ "(SELECT userId FROM User WHERE UUID = "
        					+ "(SELECT uuid FROM User WHERE username = ?)) AND userId = "
        					+ "(SELECT userId FROM User WHERE UUID = (SELECT uuid FROM User WHERE username = ?))");
        			ps.setQueryTimeout(5);
        			ps.setString(1, giver);
        			ps.setString(2, reciever);
        			ResultSet rs = ps.executeQuery();
        			int repId;
        			if(rs.next()) {
        				repId = rs.getInt("repId");
        			} else {
        				repId = 0;
        			}
            		scheduler.runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            ci.onQueryDone(repId);
                        }
                    });
        			ps.close();
            		rs.close();
        		} catch (SQLException e) {
        			Message.noRecord(player);
        		}
            	
            }
        });
	}
}
