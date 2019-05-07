package com.legit.globalrep.util;

import org.bukkit.entity.Player;

import com.legit.globalrep.RepDriver;
import com.legit.globalrep.sql.DatabaseAccess;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIHook extends PlaceholderExpansion {
	
	private RepDriver plugin;
	private DatabaseAccess dbAccess;

	public PlaceholderAPIHook(RepDriver plugin, DatabaseAccess dbAccess) {
		this.plugin = plugin;
		this.dbAccess = dbAccess;
	}

	public String onPlaceholderRequest(Player player, String identifier) {
		if(player == null) { return ""; }
		
		//Placeholder: %GlobalRep_total_rep%
		if(identifier.equals("total_rep")) {
			int totalRep = dbAccess.getTotalRep(player.getUniqueId());
			if(totalRep > 0) {
				return "&a+" + Integer.toString(totalRep);
			} else if(totalRep < 0) {
				return "&c" + Integer.toString(totalRep);
			} else {
				return "&7" + Integer.toString(totalRep);
			}
		}
		
		//Placeholder: %GlobalRep_positive_rep%
		if(identifier.equals("positive_rep")) {
			return "&a+" + Integer.toString(dbAccess.getPositiveRep(player.getUniqueId()));
		}
		
		//Placeholder: %GlobalRep_negative_rep%
		if(identifier.equals("negative_rep")) {
			return "&c" + Integer.toString(dbAccess.getNegativeRep(player.getUniqueId()));
		}
		
		return null;
	}
	
	@Override
	public boolean persist() {
		return true;
	}
	
	@Override
	public boolean canRegister() {
		return true;
	}
	
	@Override
	public String getIdentifier() {
		return "GlobalRep";
	}

	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().toString();
	}


	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}
	
}
