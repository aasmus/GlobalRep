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
package com.legit.globalrep.server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.legit.globalrep.server.sql.DatabaseAccess;
import com.legit.globalrep.util.Message;


public class RepDriver extends JavaPlugin implements Listener {
	FileConfiguration config = getConfig();
	private DatabaseAccess accessDb;
	
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    	
    	if(config.contains("MYSQL.Host") && config.contains("MYSQL.Port") && config.contains("MYSQL.Database") && config.contains("MYSQL.Username") && config.contains("MYSQL.Password")){
    		this.accessDb = new DatabaseAccess(config.getString("MYSQL.Host"), config.getInt("MYSQL.Port"), config.getString("MYSQL.Database"), config.getString("MYSQL.Username"), config.getString("MYSQL.Password"));
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
   		BukkitScheduler scheduler = getServer().getScheduler();
   		scheduler.runTaskLaterAsynchronously(this, new Runnable() {
   			public void run() {
   				accessDb.checkDatabase(p.getName(), p.getUniqueId().toString());
   			}
    	}, 1L);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
    	
    	if(cmd.getName().equalsIgnoreCase("rep")){
    		if(args.length == 0){
        		Message.help(player);
    		} else {
    			if(args.length == 1) {
    				accessDb.getRep(player, args[0], 1);
    			} else if(args.length >= 2) {
    				if(args[1].equalsIgnoreCase("positive") || args[1].equalsIgnoreCase("pos") || args[1].equalsIgnoreCase("+")){
    					for(int i = 10; i > 0; i--) {
    						if(player.hasPermission("rep.amount." + i)) {
    							accessDb.addRep(args, player, i);
    							break;
    						}
    					}
    				}else if(args[1].equalsIgnoreCase("negative") || args[1].equalsIgnoreCase("neg") || args[1].equalsIgnoreCase("-")) {
    					for(int i = 10; i > 0; i--) {
    						if(player.hasPermission("rep.amount." + i)) {
    							accessDb.addRep(args, player, -i);
    							break;
    						}
    					}
    				}else if(args[0].equalsIgnoreCase("delete")) {
    					if(player.hasPermission("rep.delete")) {
    						accessDb.deleteRep(player, args);
    					} else {
    						Message.noRepSelf(player);
    					}
    				}else if(args[1].equalsIgnoreCase("page")){
    					try {
        					accessDb.getRep(player, args[0], Integer.parseInt(args[2]));
    					} catch (Exception e) {
    						Message.noInt(player);
    					}
    				}else {
    					Message.invalidFormat(player, args[1]);
    				}
    			}
    		}
    		return true;
    	}
		return false;
    }
}
