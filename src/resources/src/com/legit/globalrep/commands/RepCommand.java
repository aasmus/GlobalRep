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
package com.legit.globalrep.commands;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.legit.globalrep.chat.Message;
import com.legit.globalrep.sql.DatabaseAccess;
import com.legit.globalrep.util.UUIDFetcher;

public class RepCommand implements CommandExecutor {
	private DatabaseAccess dbAccess;
	private Plugin plugin;
	private Message msg;
	private ArrayList<UUID> async = new ArrayList<UUID>();
	
	public RepCommand(DatabaseAccess dbAccess, Plugin plugin, Message msg) {
		this.dbAccess = dbAccess;
		this.plugin = plugin;
		this.msg = msg;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("rep")) {
			if(!async.contains(player.getUniqueId())) { //checks to see if player already has an async thread open
				async.add(player.getUniqueId()); //adds player to async thread array
				Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() { //starts a new async thread
					@Override
					public void run() {
						if (args.length == 0) {
							msg.sendHelp(player);
						} else if(args[0].equalsIgnoreCase("help")) { //checks for '/rep help' command
							msg.sendHelp(player); 
						} else if (args[0].equalsIgnoreCase("delete")) { //checks for '/rep delete ' command
							if (args.length == 2) {
								if (player.hasPermission("rep.delete.self")) { //checks for permissions
									dbAccess.removeRep(player, args[1], player.getName()); //delete's reputation record
								}
								else {
									msg.send(player, "SELF_REP");
								}
							} else {
								if (player.hasPermission("rep.delete") || player.hasPermission("rep.delete.others")) {
									dbAccess.removeRep(player, args[1], args[2]);
								} else {
									msg.send(player, "SELF_REP");
								}
							}
						} else {
							UUID uuid = UUIDFetcher.findUUID(args[0]);  //finds UUID by username
							if (uuid == null) { //checks if name exists
								msg.send(player, "NO_PLAYER");
								return;
							}
							if (args.length == 1) { //shows player's reputation records
								dbAccess.getRep(player, args[0], uuid, 1);
							} else if (args.length >= 2) { //adds positive reputation record
								if (args[1].equalsIgnoreCase("positive") || args[1].equalsIgnoreCase("pos") || args[1].equalsIgnoreCase("+")) {
									for (int i = 10; i > 0; i--) {
										if (player.hasPermission("rep.amount." + i)) {
											String comment = Message.getComment(args);
											dbAccess.addRep(player, args[0], i, comment);
											break;
										}
									}
								} else if (args[1].equalsIgnoreCase("negative") || args[1].equalsIgnoreCase("neg") || args[1].equalsIgnoreCase("-")) { //adds negative reputation record
									for (int i = 10; i > 0; i--) {
										if (player.hasPermission("rep.amount." + i)) {
											dbAccess.addRep(player, args[0], -i, Message.getComment(args));
											break;
										}
									}
								} else if (args[1].equalsIgnoreCase("page")) {
									try {
										dbAccess.getRep(player, args[0], uuid, Integer.parseInt(args[2]));
									} catch (Exception e) {
										msg.send(player, "NO_INT");
									}
								} else { //command format is invalid
									msg.send(player, "INVALID_FORMAT",  args[1]);
								}
							}
						}
					}
				});
				async.remove(player.getUniqueId()); //removes player from async thread array
			}
			return true;
		}
		return false;
	}
	
}
