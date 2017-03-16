package rep.global;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RepDriver extends JavaPlugin {

    @Override
    public void onEnable() {
       
    }
   
    @Override
    public void onDisable() {
       
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	
    	if(cmd.getName().equalsIgnoreCase("rep")){
    		if(args.length == 0){
        		sender.sendMessage(ChatColor.RED + "=====================================" + ChatColor.GOLD + " Global Rep " + ChatColor.RED + "=========================================");
        		sender.sendMessage(ChatColor.GOLD + "Use /rep <playername> to see a player's rep!");
        		sender.sendMessage(ChatColor.GOLD + "Use /rep <playername> positive <comment> to give a player positive rep!");
        		sender.sendMessage(ChatColor.GOLD + "Use /rep <playername> negative <comment> to give a player negative rep!");
        		sender.sendMessage(ChatColor.RED + "==========================================================================================");
    		} else {
    			String playerName = args[0];
    			
    			if(args.length == 1) {
    				//TODO: call database for players rep history
    				
    			} else if(args.length >= 3) {
    				if(args[2].equalsIgnoreCase("positive")){
    					//TODO: call database to add record of positive rep
    					StringBuilder builder = new StringBuilder();
    					for(int i = 3; i < args.length; i++){
    						builder.append(args[i]);
    						builder.append(" ");	
    					}
    					String comment = builder.toString();
    					
    				}else if(args[2].equalsIgnoreCase("negative")) {
    					//TODO: call database to add record of negative rep
    					
    				}else {
    					sender.sendMessage(args[2] + " is an unknown parameter!");
    				}
    			}
    			
    		}
    		return true;
    	}
    	
		return false;
    }
	
}
