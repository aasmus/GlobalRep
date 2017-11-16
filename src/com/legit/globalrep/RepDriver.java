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

import com.legit.globalrep.chat.Message;
import com.legit.globalrep.commands.RepCommand;
import com.legit.globalrep.event.PlayerJoin;
import com.legit.globalrep.sql.DatabaseAccess;

public class RepDriver extends JavaPlugin implements Listener {
	FileConfiguration config = getConfig();
	private DatabaseAccess dbAccess;
	
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    	if(config.contains("MYSQL")){
    		Message msg = new Message(this);
    		this.dbAccess = new DatabaseAccess(config.getString("MYSQL.Host"), config.getInt("MYSQL.Port"), config.getString("MYSQL.Database"), config.getString("MYSQL.Username"), config.getString("MYSQL.Password"), msg);
    		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
    			@Override
    			public void run() {
    	    		dbAccess.createTables();
    			}
    		});
        	Bukkit.getPluginManager().registerEvents(new PlayerJoin(this, dbAccess), this);
        	this.getCommand("rep").setExecutor(new RepCommand(dbAccess, this, msg));
    	}
    	setupConfig();
    }
   
    @Override
    public void onDisable() {
    	Bukkit.getScheduler().cancelTasks(this);
    }
    
    private void setupConfig() {
		if(!config.contains("MYSQL.Host"))
			config.addDefault("MYSQL.Host", "127.0.0.1");
		if(!config.contains("MYSQL.Port"))
			config.addDefault("MYSQL.Port", 3306);
		if(!config.contains("MYSQL.Database"))
			config.addDefault("MYSQL.Database", "database name");
		if(!config.contains("MYSQL.Username"))
			config.addDefault("MYSQL.Username", "root");
		if(!config.contains("MYSQL.Password"))
			config.addDefault("MYSQL.Password", "password");
		if(!config.contains("MESSAGES.PREFIX"))
			config.addDefault("MESSAGES.PREFIX", "\"&b[&aRep&b]\"");
		if(!config.contains("MESSAGES.HELP_CHECK"))
			config.addDefault("MESSAGES.HELP_CHECK", "\"&aUse /rep <name> to see a player's rep!\"");
		if(!config.contains("MESSAGES.HELP_POSITIVE"))
			config.addDefault("MESSAGES.HELP_POSITIVE", "\"&aUse /rep <name> positive <comment> to give positive rep!\"");
		if(!config.contains("MESSAGES.HELP_NEGATIVE"))
			config.addDefault("MESSAGES.HELP_NEGATIVE", "\"&aUse /rep <name> negative <comment> to give negative rep!\"");
		if(!config.contains("MESSAGES.HELP_DELETE_SELF"))
			config.addDefault("MESSAGES.HELP_DELETE_SELF", "\"&aUse /rep delete <name> to delete a rep you gave someone!\"");
		if(!config.contains("MESSAGES.HELP_DELETE_OTHERS"))
			config.addDefault("MESSAGES.HELP_DELETE_OTHERS", "\"&aUse /rep delete <reciever> <giver> to remove rep!\"");
		if(!config.contains("MESSAGES.SELF_REP"))
			config.addDefault("MESSAGES.SELF_REP", "\"&cYou cannot give yourself rep.\"");
		if(!config.contains("MESSAGES.NO_INT"))
			config.addDefault("MESSAGES.NO_INT", "\"&cNo page number entered.\"");
		if(!config.contains("MESSAGES.INVALID_FORMAT"))
			config.addDefault("MESSAGES.INVALID_FORMAT", "\"&c<parameter> is an unknown parameter. Parameters are: positive, negative, and page\"");
		if(!config.contains("MESSAGES.NO_RECORD"))
			config.addDefault("MESSAGES.NO_RECORD", "\"&cThat rep record doesn't exist!\"");
		if(!config.contains("MESSAGES.NO_PLAYER"))
			config.addDefault("MESSAGES.NO_PLAYER", "\"&cThat player does not exist!\"");
		if(!config.contains("MESSAGES.NO_REP"))
			config.addDefault("MESSAGES.NO_REP", "\"&9<parameter> has no rep!\"");
		if(!config.contains("MESSAGES.PAGE_OUT_OF_BOUNDS"))
			config.addDefault("MESSAGES.PAGE_OUT_OF_BOUNDS", "\"&cInvalid page number.\"");
		if(!config.contains("MESSAGES.NO_PERMISSION"))
			config.addDefault("MESSAGES.NO_PERMISSION", "\"&cYou do not have permission do delete reputation records.\"");
		if(!config.contains("MESSAGES.REP_REMOVED"))
			config.addDefault("MESSAGES.REP_REMOVED", "\"&cReputaton record deleted.\"");
		if(!config.contains("MESSAGES.REP_ADDED"))
			config.addDefault("MESSAGES.REP_ADDED", "\"&9Your reputation has changed! View your rep with /rep <parameter>.\"");
		if(!config.contains("MESSAGES.REP_GIVEN"))
			config.addDefault("MESSAGES.REP_GIVEN", "\"&9Reputation added! You can use /rep <parameter> to see it.\"");
		if(!config.contains("MESSAGES.ERROR"))
			config.addDefault("MESSAGES.ERROR", "\"&cAn error has occured. Please tell a server administrator.\"");
		config.options().copyDefaults(true);
		saveConfig();
    }
    
}
