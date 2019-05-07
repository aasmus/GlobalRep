package com.legit.globalrep.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import com.legit.globalrep.sql.DatabaseAccess;

public class PlayerJoin implements Listener {
	
	private DatabaseAccess dbAccess;
	private Plugin plugin;
	
	public PlayerJoin(Plugin plugin, DatabaseAccess dbAccess) {
		this.plugin = plugin;
		this.dbAccess = dbAccess;
	}
    
	/**
	 * OnPlayerJoin: checks if player exists in database and for name changes
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() { //start a new async thread
			@Override
			public void run() {
				dbAccess.checkDatabase(p.getName(), p.getUniqueId().toString()); //check if the player is in the database
			}
		});
	}

}
