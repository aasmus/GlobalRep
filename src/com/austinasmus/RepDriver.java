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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
        getServer().getPluginManager().registerEvents(this, this);
    	
    	if(config.contains("MYSQL.Host")){
            this.host = config.getString("MYSQL.Host");
            this.port = config.getInt("MYSQL.Port");
            this.database = config.getString("MYSQL.Database");
            this.username = config.getString("MYSQL.Username");
            this.password = config.getString("MYSQL.Password");
            
            AccessDatabase accessDb = new AccessDatabase(host, port, database, username, password);
            accessDb.generateTables();
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
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
    	Player p = event.getPlayer();
    	
    	AccessDatabase accessDb = new AccessDatabase(host, port, database, username, password);
    	accessDb.checkDatabase(p.getName(), p.getUniqueId().toString());
    	
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
    	    	AccessDatabase accessDb = new AccessDatabase(host, port, database, username, password);
    			if(args.length == 1) {
    				accessDb.getRep(player, args[0]);
    				
    			} else if(args.length >= 2) {
    				if(args[1].equalsIgnoreCase("positive")){
    					for(int i = 0; i < 10; i++) {
    						if(player.hasPermission("rep.amount." + i)) {
    							accessDb.addRep(args, player, i);
    							break;
    							
    						}
    					}
    				}else if(args[1].equalsIgnoreCase("negative")) {
    					for(int i = 0; i < 10; i++) {
    						if(player.hasPermission("rep.amount." + i)) {
    							accessDb.addRep(args, player, -i);
    							break;
    							
    						}
    					}
    				}else {
    					player.sendMessage(ChatColor.RED + args[1] + " is an unknown parameter. Parameters are: positive, negative");
    					
    				}
    			}
    		}
    		return true;
    	}
		return false;
    }
}
