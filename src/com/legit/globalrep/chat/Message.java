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
package com.legit.globalrep.chat;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.legit.globalrep.object.Rep;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Message {
	
	private static Plugin plugin;
	
	public Message(Plugin plugin) {
		Message.plugin = plugin;
	}
	
	public void send(Player player, String message) {
		if(player.isOnline())
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.PREFIX") + " "+ plugin.getConfig().getString("MESSAGES." + message)));
	}
	
	public void send(Player player, String message, String parameter) {
		if(player.isOnline()) {
			String str = plugin.getConfig().getString("MESSAGES." + message);
			String output = str.replaceAll("<parameter>", parameter);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.PREFIX") + " " + output));			
		}
	}
	
	public void sendHelp(Player player) {
		player.sendMessage(ChatColor.AQUA + "§m" + StringUtils.repeat(" ", 31) + ChatColor.GREEN + ChatColor.BOLD + " Global Rep " + ChatColor.RESET + ChatColor.AQUA + "§m" + StringUtils.repeat(" ", 31));
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.HELP_CHECK")));
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.HELP_POSITIVE")));
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.HELP_NEGATIVE")));
		if(player.hasPermission("rep.delete.self"))
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.HELP_DELETE_SELF")));
		if(player.hasPermission("rep.delete") || player.hasPermission("rep.delete.others"))
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("MESSAGES.HELP_DELETE_OTHERS")));
		printLine(player);
	}
	
	public void tableCreated(String table) {
		System.out.println("[GlobalRep] " + table + " table created");
	}
	
	public static void databaseError(Exception e) {
		System.out.println("[GlobalRep] An Error occured. Is the database down?");
		e.printStackTrace();
	}

	
	
	/**
	 * getComment - fired when a new rep record is being created to get comment for reputation record
	 * 
	 * @param args - array of arguments containing comment
	 * @return - returns comment as a string
	 */
	public synchronized static String getComment(String[] args) {
		String comment = "";
		if (args.length >= 3) {
			StringBuilder builder = new StringBuilder();
			for(int i = 2; i < args.length; i++){
				builder.append(args[i]);
				builder.append(" ");	
			}
			comment = builder.toString();
		}
		return comment;
	}
	
	
	/**
	 * displayRep - used to display reputation records 10 at a time
	 * 
	 * @param player - command sender
	 * @param username - player whose rep records are being looked up
	 * @param reps - synchronized List array containing rep objects
	 * @param page - rep page command sender is on
	 * @param totalPages - total pages of rep records
	 */
	public void displayRep(Player player, String username, java.util.List<Rep> reps, int page, int totalPages) {
		int resultAmount = page * 10;
		if(page <= totalPages && page > 0) {
			Message.repHeader(player, username);
			Rep rep;
			if(reps.size()-resultAmount <= 0) {
				if(page == 1) {
					for(int i = 0; i < reps.size(); i++) {
						rep = reps.get(i);
						sendRep(player, rep);
					}
				} else {
					int remaining = reps.size()%(resultAmount-10);
					for(int i = reps.size()-remaining; i < reps.size(); i++) {
						rep = reps.get(i);
						sendRep(player, rep);
					}
				}
			} else {
				for(int i = resultAmount-10; i < resultAmount; i++) {
					rep = reps.get(i);
					sendRep(player, rep);
				}
			}
			
			Message.navigate(player, username, page, totalPages);
			
			int positiveRep = 0;
			int negativeRep = 0;
			for(Rep record : reps){
				if(record.getAmount() > 0) {
					positiveRep += record.getAmount();
				} else if (record.getAmount() < 0) {
					negativeRep += record.getAmount();
				}
			}
			
			if(positiveRep + negativeRep > 0) {
				Message.repTotalPositive(player, positiveRep, negativeRep);
			} else if(positiveRep + negativeRep < 0) {
				Message.repTotalNegative(player, positiveRep, negativeRep);
			} else {
				Message.repTotalZero(player, positiveRep, negativeRep);
			}
			
			Message.repFooter(player);
		} else {
			send(player, "PAGE_OUT_OF_BOUNDS");
		}
	}
	
	/**
	 * sendRep - decides whether rep was positive or negative and sends output accordingly
	 * 
	 * @param player - command sender
	 * @param reps - synchronized List array containing rep objects
	 * @param i - counter variable
	 */
	private static void sendRep(Player player, Rep rep) {
		if(rep.getAmount() > 0){
			Message.repPositive(player, rep.getAmount(), rep.getDate(), rep.getUsername(), rep.getComment());
		} else {
			Message.repNegative(player, rep.getAmount(), rep.getDate(), rep.getUsername(), rep.getComment());
		}
	}
	
	
	
	
	
	
	private static void repHeader(Player player, String username) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + username + "'s Reputation:");
			printLine(player);
		}
	}
	
	private static void repPositive(Player player, int amount, String date, String username, String comment) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "+" + amount + " " + ChatColor.GRAY + date + " " + ChatColor.YELLOW + username + ": " + ChatColor.RESET + comment);
		}
	}
	
	private static void repNegative(Player player, int amount, String date, String username, String comment) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.RED + "" + amount + " " + ChatColor.GRAY + date + " " + ChatColor.YELLOW + username + ": " + ChatColor.RESET + comment);
		}
	}
	
	private static void navigate(Player player, String username, int page, int totalPages) {
		if(player.isOnline()) {
			if(page == 1) {
				if(page == totalPages) {
					TextComponent prev = new TextComponent("<§m--");
					prev.setColor(ChatColor.GRAY);
					
					TextComponent middle = new TextComponent(" Page " + page + "/" + totalPages + " ");
					
					TextComponent next = new TextComponent("§m--§r§7>");
					next.setColor(ChatColor.GRAY);
					player.spigot().sendMessage(prev, middle, next);
				} else {
					TextComponent prev = new TextComponent("<§m--");
					prev.setColor(ChatColor.GRAY);
					
					TextComponent middle = new TextComponent(" Page " + page + "/" + totalPages + " ");
					
					TextComponent next = new TextComponent("§m--§r§b>");
					next.setColor(ChatColor.AQUA);
					next.setClickEvent( new ClickEvent (ClickEvent.Action.RUN_COMMAND, "/rep " + username + " page " + (page+1)));
					next.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Next page.").create()));
					player.spigot().sendMessage(prev, middle, next);
				}
			} else if(page != totalPages) {
				TextComponent prev = new TextComponent("<§m--");
				prev.setColor(ChatColor.AQUA);
				prev.setClickEvent( new ClickEvent (ClickEvent.Action.RUN_COMMAND, "/rep " + username + " page " + (page-1)));
				prev.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Previous page.").create()));
				
				TextComponent middle = new TextComponent(" Page " + page + "/" + totalPages + " ");
				
				TextComponent next = new TextComponent("§m--§r§b>");
				next.setColor(ChatColor.AQUA);
				next.setClickEvent( new ClickEvent (ClickEvent.Action.RUN_COMMAND, "/rep " + username + " page " + (page+1)));
				next.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Next page.").create()));
				player.spigot().sendMessage(prev, middle, next);
			} else if(page == totalPages) {
				TextComponent prev = new TextComponent("<§m--");
				prev.setColor(ChatColor.AQUA);
				prev.setClickEvent( new ClickEvent (ClickEvent.Action.RUN_COMMAND, "/rep " + username + " page " + (page-1)));
				prev.setHoverEvent( new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Previous page.").create()));
				
				TextComponent middle = new TextComponent(" Page " + page + "/" + totalPages + " ");
				
				TextComponent next = new TextComponent("§m--§r§7>");
				next.setColor(ChatColor.GRAY);
				player.spigot().sendMessage(prev, middle, next);
			}
		}
	}
	
	private static void repTotalPositive(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.GREEN + (positiveRep + negativeRep));			
		}
	}
	
	private static void repTotalNegative(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.RED + (positiveRep + negativeRep));			
		}
	}
	
	private static void repTotalZero(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.GRAY + (positiveRep + negativeRep));
		}
	}
	
	private static void repFooter(Player player) {
		if(player.isOnline()) {
			printLine(player);
		}
	}
	
	private static void printLine(Player player) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.AQUA + "§m" + StringUtils.repeat(" ", 80));
		}
	}
}