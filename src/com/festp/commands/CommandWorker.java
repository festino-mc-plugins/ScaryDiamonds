package com.festp.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.festp.Config;
import com.festp.Main;
import com.festp.amethyst.AmethystChunk;
import com.festp.utils.Utils;

public class CommandWorker implements CommandExecutor, TabCompleter {
	private Main plugin;
	
	public final static String MAIN_COMMAND = "scarydiamonds";
	public final static String MAIN_USAGE = "Usage:\n"
			+ "/" + MAIN_COMMAND + " config\n"
			+ "/" + MAIN_COMMAND + " metrics\n";
	
	public CommandWorker(Main plugin) {
		this.plugin = plugin;
	}
	
	private void reloadConfig() {
		Config.plugin().reloadConfig();
	}
	
	private String formatLong(long n, int minDigits)
	{
		String res = "" + n;
		int dif = minDigits - res.length();
		for (int i = 0; i < dif; i++)
			res = '0' + res;
		return res;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase(MAIN_COMMAND))
		{
			if (args.length == 0)
			{
				sender.sendMessage(ChatColor.GREEN + plugin.amethystManager.getInfo());
				sender.sendMessage(ChatColor.GRAY + MAIN_USAGE);
				return true;
			}
			if (args[0].equalsIgnoreCase("world"))
			{
				if (sender instanceof Player) {
					sender.sendMessage(ChatColor.GREEN + plugin.amethystManager.getInfo(((Player)sender).getWorld()));
					return true;
				}
			}
			if (args[0].equalsIgnoreCase("chunk"))
			{
				if (sender instanceof Player) {
					AmethystChunk chunk = plugin.amethystManager.get(((Player)sender).getLocation());
					if (chunk == null) {
						sender.sendMessage(ChatColor.GREEN + "Chunk isn't loaded");
					} else {
						sender.sendMessage(ChatColor.GREEN + "Loaded " + chunk.antispawnBlocks.size() + " blocks:");
						for (Block b : chunk.antispawnBlocks)
							sender.sendMessage(ChatColor.GREEN + "   " + b.getType() + " " + Utils.toString(b.getLocation()));
					}
					return true;
				}
			}
			if (args[0].equalsIgnoreCase("config"))
			{
				if (!sender.isOp())
				{
					sender.sendMessage(ChatColor.RED + "The sender must be an operator.");
					return false;
				}
				if (args.length == 2 && args[1].equalsIgnoreCase("reload"))
				{
					reloadConfig();
					Config.loadConfig();
					sender.sendMessage(ChatColor.GREEN + "Config was reloaded.");
				}
				return true;
			}
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Couldn't find command \"" + cmd + "\".");
			return false;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> options = new ArrayList<>();
		if (args.length <= 1) {
			options.add("config");
			options.add("world");
			options.add("chunk");
		}
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("config")) {
				options.add("reload");
			}
		}
		return options;
	}
}
