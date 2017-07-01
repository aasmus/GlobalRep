package com.legit.globalrep.event;

import static org.bukkit.Bukkit.getServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.legit.globalrep.sql.DatabaseAccess;

public class PlayerJoin implements Listener {
	
	private Plugin plugin;
	private DatabaseAccess dbAccess;
	
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
   		BukkitScheduler scheduler = getServer().getScheduler();
   		scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {
   			public void run() {
   				dbAccess.checkDatabase(p.getName(), p.getUniqueId().toString());
   			}
    	}, 1L);
    }

}
