package com.festp;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.festp.amethyst.AmethystManager;
import com.festp.commands.CommandWorker;
import com.festp.utils.TimeUtils;

public class Main extends JavaPlugin implements Listener
{
	private static String PATH = "plugins" + System.getProperty("file.separator") + "ScaryDiamonds" + System.getProperty("file.separator");
	private static String pluginname;
	
	Config conf;
	
	public AmethystManager amethystManager;
	
	public static String getPath()
	{
		return PATH;
	}
	
	public void onEnable()
	{
		Logger.setLogger(getLogger());
		pluginname = getName();
		PATH = "plugins" + System.getProperty("file.separator") + pluginname + System.getProperty("file.separator");
    	PluginManager pm = getServer().getPluginManager();
    	
		getServer().getPluginManager().registerEvents(this, this);
		
		conf = new Config(this);
		Config.loadConfig();
    	
    	CommandWorker command_worker = new CommandWorker(this);
    	getCommand(CommandWorker.MAIN_COMMAND).setExecutor(command_worker);
    	getCommand(CommandWorker.MAIN_COMMAND).setTabCompleter(command_worker);

    	amethystManager = new AmethystManager();
    	pm.registerEvents(amethystManager, this);
    	
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
			new Runnable() {
				public void run() {
					TimeUtils.addTick();
					
					TaskList.tick();
					
					amethystManager.tick();
				}
			}, 0L, 1L);
	}
}
