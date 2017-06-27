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
package com.legit.globalrep;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.legit.globalrep.commands.RepCommand;
import com.legit.globalrep.event.PlayerJoin;
import com.legit.globalrep.sql.DatabaseAccess;

public class RepDriver extends JavaPlugin implements Listener {
	FileConfiguration config = getConfig();
	private DatabaseAccess dbAccess;
	
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    	
    	if(config.contains("MYSQL.Host") && config.contains("MYSQL.Port") && config.contains("MYSQL.Database") && config.contains("MYSQL.Username") && config.contains("MYSQL.Password")){
    		this.dbAccess = new DatabaseAccess(config.getString("MYSQL.Host"), config.getInt("MYSQL.Port"), config.getString("MYSQL.Database"), config.getString("MYSQL.Username"), config.getString("MYSQL.Password"));
        	dbAccess.createTable("User");
        	dbAccess.createTable("Rep");
        	Bukkit.getPluginManager().registerEvents(new PlayerJoin(this, dbAccess), this);
        	this.getCommand("rep").setExecutor(new RepCommand(dbAccess));
    	} else {
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
        
    }
   
    @Override
    public void onDisable() {
    	Bukkit.getScheduler().cancelTasks(this);
    }
    
}
