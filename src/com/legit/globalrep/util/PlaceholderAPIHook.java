package com.legit.globalrep.util;

import org.bukkit.entity.Player;

import com.legit.globalrep.RepDriver;
import com.legit.globalrep.sql.DatabaseAccess;

import me.clip.placeholderapi.external.EZPlaceholderHook;

public class PlaceholderAPIHook extends EZPlaceholderHook {
	private DatabaseAccess dbAccess;

	public PlaceholderAPIHook(RepDriver plugin, DatabaseAccess dbAccess) {
		super(plugin, "globalrep");
		this.dbAccess = dbAccess;
	}

	public String onPlaceholderRequest(Player player, String identifier) {
		if(player == null) { return ""; }
		
		//Placeholder: %GlobalRep_total_rep%
		if(identifier.equals("total_rep")) {
			return Integer.toString(dbAccess.getTotalRep(player.getUniqueId()));
		}
		
		//Placeholder: %GlobalRep_positive_rep%
		if(identifier.equals("positive_rep")) {
			return Integer.toString(dbAccess.getPositiveRep(player.getUniqueId()));
		}
		
		//Placeholder: %GlobalRep_negative_rep%
		if(identifier.equals("negative_rep")) {
			return Integer.toString(dbAccess.getNegativeRep(player.getUniqueId()));
		}
		
		return null;
	}
	
}
