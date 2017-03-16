package rep.global;

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
    	if(!config.contains("MYSQL.Host")) {
    		config.addDefault("MYSQL.Host", "127.0.0.1");
    	}
    	if(!config.contains("MYSQL.Port")) {
    		config.addDefault("MYSQL.Port", "3306");
    	}
    	if(!config.contains("MYSQL.Database")) {
    		config.addDefault("MYSQL.Database", "Database Name");
    	}
    	if(!config.contains("MYSQL.Username")) {
    		config.addDefault("MYSQL.Username", "root");
    	}
    	if(!config.contains("MYSQL.Password")) {
    		config.addDefault("MYSQL.Password", "Password");
    	}
        config.options().copyDefaults(true);
        saveConfig();
        
        this.host = config.getString("MYSQL.Host");
        this.port = config.getInt("MYSQL.Port");
        this.database = config.getString("MYSQL.Database");
        this.username = config.getString("MYSQL.Username");
        this.password = config.getString("MYSQL.Password");
        
        AccessDatabase tables = new AccessDatabase(host, port, database, username, password);
        tables.generateTables();

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
        		player.sendMessage(ChatColor.RED + "=====================================" + ChatColor.GOLD + " Global Rep " + ChatColor.RED + "=========================================");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <playername> to see a player's rep!");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <playername> positive <comment> to give a player positive rep!");
        		player.sendMessage(ChatColor.GOLD + "Use /rep <playername> negative <comment> to give a player negative rep!");
        		player.sendMessage(ChatColor.RED + "==========================================================================================");
    		} else {
		        AccessDatabase data = new AccessDatabase(host, port, database, username, password);
    			if(args.length == 1) {
    				//TODO: call database for players rep history
    				
    			} else if(args.length >= 3) {
    				if(args[2].equalsIgnoreCase("positive")){
    					//TODO: call database to add record of positive rep
    					for(int i = 1; i <= 10; i++){
    						if(player.hasPermission("rep.amount." + i)){
            					data.addRep(args, player, i);
    						}
    					}
    					
    				}else if(args[2].equalsIgnoreCase("negative")) {
    					//TODO: call database to add record of negative rep
    					for(int i = 1; i <= 10; i++){
    						if(player.hasPermission("rep.amount." + i)){
            					data.addRep(args, player, -i);
    						}
    					}
    					
    				}else {
    					player.sendMessage(args[2] + " is an unknown parameter!");
    				}
    			}
    			
    		}
    		return true;
    	}
    	
		return false;
    }
	
}
