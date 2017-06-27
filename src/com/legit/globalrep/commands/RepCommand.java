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

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.legit.globalrep.chat.Message;
import com.legit.globalrep.object.Rep;
import com.legit.globalrep.sql.DatabaseAccess;
import com.legit.globalrep.util.UUIDFetcher;

public class RepCommand implements CommandExecutor {
	private DatabaseAccess dbAccess;
	
	public RepCommand(DatabaseAccess dbAccess) {
		this.dbAccess = dbAccess;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;

		if (cmd.getName().equalsIgnoreCase("rep")) {
			if (args.length == 0) {
				Message.help(player);
			} else {
				if (args.length == 1) {
					UUID uuid = UUIDFetcher.findUUID(args[0]);
					boolean exists = dbAccess.hasLoggedIn(uuid);
					if (exists)
						dbAccess.getRep(player, args[0], uuid, 1);
					else if(!exists || uuid == null)
						Message.noPlayer(player);
				} else if (args.length >= 2) {
					if (args[1].equalsIgnoreCase("positive") || args[1].equalsIgnoreCase("pos") || args[1].equalsIgnoreCase("+")) {
						for (int i = 10; i > 0; i--) {
							if (player.hasPermission("rep.amount." + i)) {
								String comment = getComment(args);
								dbAccess.addRep(player, args[0], i, comment);
								break;
							}
						}
					} else if (args[1].equalsIgnoreCase("negative") || args[1].equalsIgnoreCase("neg") || args[1].equalsIgnoreCase("-")) {
						for (int i = 10; i > 0; i--) {
							if (player.hasPermission("rep.amount." + i)) {
								String comment = getComment(args);
								dbAccess.addRep(player, args[0], -i, comment);
								break;
							}
						}
					} else if (args[0].equalsIgnoreCase("delete")) {
						if (player.hasPermission("rep.delete")) {
							deleteRecord(player, args[1], args[2]);
						} else {
							Message.noRepSelf(player);
						}
					} else if (args[1].equalsIgnoreCase("page")) {
						UUID uuid = UUIDFetcher.findUUID(args[0]);
						boolean exists = dbAccess.hasLoggedIn(uuid);
						if (exists) {
							try {
								dbAccess.getRep(player, args[0], uuid, Integer.parseInt(args[2]));
							} catch (Exception e) {
								Message.noInt(player);
							}
						} else {
							Message.noPlayer(player);
						}
					} else {
						Message.invalidFormat(player, args[1]);
					}
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * getComment - fired when a new rep record is being created to get comment for reputation record
	 * 
	 * @param args - array of arguments containing comment
	 * @return - returns comment as a string
	 */
	private synchronized String getComment(String[] args) {
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
	 * deleteRecord - fired when a reputation record is being deleted
	 * 
	 * @param player - command sender
	 * @param reciever - player who recieved the rep that is being deleted
	 * @param giver - player who gave the rep that is being deleted
	 */
	private void deleteRecord(Player player, String reciever, String giver) {
		int repId = dbAccess.getrepIdByUsername(player, reciever, giver);
		if (repId != 0) {
			boolean deleted = dbAccess.removeRep(repId);
			if(deleted)
				Message.repRemoved(player);
			else
				Message.genericErrorPlayer(player);
		} else {
			Message.noRecord(player);
		}
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
	public static void displayRep(Player player, String username, java.util.List<Rep> reps, int page, int totalPages) {
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
			Message.pageOutOfBounds(player);
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

}
