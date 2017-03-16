package rep.global;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
			
			String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '?' AND table_name = 'User'";
			
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, DB_NAME);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			String table;
			if(rs == null){
				table = "CREATE TABLE User "
				+ "(userId INT auto_increment NOT NULL PRIMARY KEY, "
				+ "UUID varchar(40) NOT NULL, "
				+ "username varchar(16) NOT NULL)";

				try {
					ps = conn.prepareStatement(table);
					ps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '?' AND table_name = 'Rep'";
			
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, user);
				rs = ps.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			if(rs == null) {
				table = "CREATE TABLE Rep "
						+ "(repId INT auto_increment NOT NULL PRIMARY KEY, "
						+ "date varchar(10) NOT NULL, "
						+ "repAmount INT NOT NULL, "
						+ "comment varchar(255), "
						+ "userId NOT NULL, "
						+ "FOREIGN KEY (userId) REFERENCES User (userId))";

						try {
							ps = conn.prepareStatement(table);
							ps.executeUpdate();
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
	
	public String getRep() {
		return "";
	}
	
	public boolean addRep(String[] args, Player player, int rep) {
		
		String comment = "";	
		if(args.length >= 4) {
			StringBuilder builder = new StringBuilder();
			for(int i = 3; i < args.length; i++){
				builder.append(args[i]);
				builder.append(" ");	
			}
			comment = builder.toString();
		}
		String uuid = null;
		try {
		uuid = player.getUniqueId().toString();
		} catch(Exception e) {
			return false;
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
		
		String query = "SELECT userId FROM User WHERE UUID = '?'";
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String insert;
		if(rs == null){
			insert = "INSERT INTO User (uuid, username) VALUES ?, ?";
			try {
				ps = conn.prepareStatement(query);
				ps.setString(1, uuid);
				ps.setString(2, player.getName());
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		
		query = "SELECT userId FROM User WHERE UUID = '?'";
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, uuid);
			rs = ps.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		int userId = Integer.parseInt(rs);
		
		insert = "INSERT INTO Rep (date, repAmount, comment, userId) VALUES ?, ?, ?, ?";
		ps = conn.prepareStatement(insert);
		ps.setString(1, date);
		ps.setInt(2, rep);
		ps.setString(3, comment);
		ps.setInt(4, userId);
		
		} catch (SQLException e) {
			System.out.println("SQLException: ");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

}
