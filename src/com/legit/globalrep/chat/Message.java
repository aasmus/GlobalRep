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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Message {
	
	public static void tableCreated(String table) {
		System.out.println("[GlobalRep] " + table + " table created");
	}
	
	public static void genericErrorSystem(Exception e) {
		e.printStackTrace();
	}
	
	public static void databaseError(Exception e) {
		System.out.println("[GlobalRep] An Error occured. Is the database down?");
		e.printStackTrace();
	}
	
	public static void genericErrorPlayer(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "An error has occured. Please tell a server administrator.");
		}
	}
	
	public static void help(Player player) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.AQUA + "§m" + StringUtils.repeat(" ", 31) + ChatColor.GREEN + ChatColor.BOLD + " Global Rep " + ChatColor.RESET + ChatColor.AQUA + "§m" + StringUtils.repeat(" ", 31));
			player.sendMessage(ChatColor.GREEN + "Use /rep <name> to see a player's rep!");
			player.sendMessage(ChatColor.GREEN + "Use /rep <name> positive <comment> to give positive rep!");
			player.sendMessage(ChatColor.GREEN + "Use /rep <name> negative <comment> to give negative rep!");
			if(player.hasPermission("rep.delete")) {
				player.sendMessage(ChatColor.GREEN + "Use /rep delete <reciever> <giver> to remove rep!");
			}
			printLine(player);
		}
	}
	
	private static String chatPrefix() {
		return ChatColor.AQUA + "[" + ChatColor.GREEN + "Rep" + ChatColor.AQUA + "] ";
	}
	
	public static void repSelf(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "You cannot give yourself rep.");
		}
	}
	
	public static void noInt(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "No page number entered");
		}
	}
	
	public static void invalidFormat(Player player, String invalid) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + invalid + " is an unknown parameter. Parameters are: positive, negative, and page");	
		}
	}
	
	public static void noRecord(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "That rep record doesn't exist!");
		}
	}
	
	public static void noPlayer(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "That player does not exist!");
		}
	}
	
	public static void noRep(Player player, String username) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.BLUE + username + " has no rep!");
		}
	}
	
	public static void pageOutOfBounds(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "Invalid page number.");
		}
	}
	
	public static void noRepSelf(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "You do not have permission do delete reputation records.");
		}
	}
	
	public static void repRemoved(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.RED + "Reputaton record deleted.");
		}
	}
	
	public static void repAddedSelf(Player player) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.BLUE + "Your reputation has changed! View your rep with /rep " + player.getName() + ".");
		}
	}
	
	public static void repAddedOther(Player player, String username) {
		if(player.isOnline()) {
			player.sendMessage(chatPrefix() + ChatColor.BLUE + "Reputation added! You can use /rep " + username + " to see it.");
		}
	}
	
	public static void repHeader(Player player, String username) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + username + "'s Reputation:");
			printLine(player);
		}
	}
	
	public static void repPositive(Player player, int amount, String date, String username, String comment) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "+" + amount + " " + ChatColor.GRAY + date + " " + ChatColor.YELLOW + username + ": " + ChatColor.RESET + comment);
		}
	}
	
	public static void repNegative(Player player, int amount, String date, String username, String comment) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.RED + "" + amount + " " + ChatColor.GRAY + date + " " + ChatColor.YELLOW + username + ": " + ChatColor.RESET + comment);
		}
	}
	
	public static void navigate(Player player, String username, int page, int totalPages) {
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
	
	public static void repTotalPositive(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.GREEN + (positiveRep + negativeRep));			
		}
	}
	
	public static void repTotalNegative(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.RED + (positiveRep + negativeRep));			
		}
	}
	
	public static void repTotalZero(Player player, int positiveRep, int negativeRep) {
		if(player.isOnline()) {
			player.sendMessage(ChatColor.GREEN + "Positive Rep: " + positiveRep + " " + ChatColor.RED + "Negative Rep: " + Math.abs(negativeRep) + ChatColor.BLUE + " Total Rep: " + ChatColor.GRAY + (positiveRep + negativeRep));
		}
	}
	
	public static void repFooter(Player player) {
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