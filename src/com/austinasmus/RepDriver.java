package com.austinasmus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RepDriver extends JavaPlugin implements Listener {
	FileConfiguration config = getConfig();
	private String host;
	private int port;
	private String database;
	private String username;
	private String password;
	
    @Override
    public void onEnable() {
    	if(config.contains("MYSQL.Host")){
            this.host = config.getString("MYSQL.Host");
            this.port = config.getInt("MYSQL.Port");
            this.database = config.getString("MYSQL.Database");
            this.username = config.getString("MYSQL.Username");
            this.password = config.getString("MYSQL.Password");
            
            AccessDatabase tables = new AccessDatabase(host, port, database, username, password);
            tables.generateTables();
    	}
    	
    	if(!config.contains("MYSQL.Host")) {
    		config.addDefault("MYSQL.Host", "127.0.0.1");
    	}
    	if(!config.contains("MYSQL.Port")) {
    		config.addDefault("MYSQL.Port", 3306);
    	}
    	if(!config.contains("MYSQL.Database")) {
    		config.addDefault("MYSQL.Database", "database name");
    	}
    	if(!config.contains("MYSQL.Username")) {
    		config.addDefault("MYSQL.Username", "root");
    	}
    	if(!config.contains("MYSQL.Password")) {
    		config.addDefault("MYSQL.Password", "password");
    	}
        config.options().copyDefaults(true);
        saveConfig();

    }
   
    @Override
    public void onDisable() {
       
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
    	
    	if(cmd.getName().equalsIgnoreCase("rep")){
    		if(args.length == 0){
        		player.sendMessage(ChatColor.RED + "===================" + ChatColor.GOLD + ChatColor.BOLD + " Global Rep " + ChatColor.RESET + ChatColor.RED + "===================");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <name> to see a player's rep!");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <name> positive <comment> to give positive rep!");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <name> negative <comment> to give negative rep!");
        		player.sendMessage(ChatColor.RED + "==================================================");
    		} else {
		        AccessDatabase data = new AccessDatabase(host, port, database, username, password);
    			if(args.length == 1) {
    				data.getRep(player, args[0]);
    				
    			} else if(args.length >= 3) {
    				if(args[1].equalsIgnoreCase("positive")){
    					if(player.hasPermission("rep.amount.10")){
        					data.addRep(args, player, 10);
						} else if(player.hasPermission("rep.amount.9")){
        					data.addRep(args, player, 9);
						} else if(player.hasPermission("rep.amount.8")){
        					data.addRep(args, player, 8);
						} else if(player.hasPermission("rep.amount.7")){
        					data.addRep(args, player, 7);
						} else if(player.hasPermission("rep.amount.6")){
        					data.addRep(args, player, 6);
						} else if(player.hasPermission("rep.amount.5")){
        					data.addRep(args, player, 5);
						} else if(player.hasPermission("rep.amount.4")){
        					data.addRep(args, player, 4);
						} else if(player.hasPermission("rep.amount.3")){
        					data.addRep(args, player, 3);
						} else if(player.hasPermission("rep.amount.2")){
        					data.addRep(args, player, 2);
						} else if(player.hasPermission("rep.amount.1")){
        					data.addRep(args, player, 1);
						}
    					
    				}else if(args[1].equalsIgnoreCase("negative")) {
    					if(player.hasPermission("rep.amount.10")){
        					data.addRep(args, player, -10);
						} else if(player.hasPermission("rep.amount.9")){
        					data.addRep(args, player, -9);
						} else if(player.hasPermission("rep.amount.8")){
        					data.addRep(args, player, -8);
						} else if(player.hasPermission("rep.amount.7")){
        					data.addRep(args, player, -7);
						} else if(player.hasPermission("rep.amount.6")){
        					data.addRep(args, player, -6);
						} else if(player.hasPermission("rep.amount.5")){
        					data.addRep(args, player, -5);
						} else if(player.hasPermission("rep.amount.4")){
        					data.addRep(args, player, -4);
						} else if(player.hasPermission("rep.amount.3")){
        					data.addRep(args, player, -3);
						} else if(player.hasPermission("rep.amount.2")){
        					data.addRep(args, player, -2);
						} else if(player.hasPermission("rep.amount.1")){
        					data.addRep(args, player, -1);
						}
    					
    				}else {
    					player.sendMessage(ChatColor.RED + args[1] + " is an unknown parameter accepted parameters are: positive, negative");
    				}
    			}
    			
    		}
    		return true;
    	}
    	
		return false;
    }
	
}
