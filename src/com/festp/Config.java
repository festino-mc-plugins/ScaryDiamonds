package com.festp;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
	
	private static JavaPlugin plugin;
	private static MemoryConfiguration config;
	public static String pluginName;

	public static Map<String,Boolean> FunctionsON = new HashMap<>();
	public static boolean showParticles = true;
	
	public Config(JavaPlugin jp) {
		plugin = jp;
		pluginName = plugin.getName();
		config = jp.getConfig();
	}
	
	public static void loadConfig()
	{
		config.addDefault("show-particles", true);
		config.options().copyDefaults(true);
		plugin.saveConfig();

		Config.showParticles = plugin.getConfig().getBoolean("show-particles");

		Logger.info("Config reloaded.");
	}
	
	public static void saveConfig()
	{
		config.set("show-particles", showParticles);

		plugin.saveConfig();
		
		Logger.info("Config successfully saved.");
	}
	
	public static JavaPlugin plugin() {
		return plugin;
	}
}
