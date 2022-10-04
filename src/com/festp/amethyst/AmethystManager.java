package com.festp.amethyst;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkPopulateEvent;

import com.festp.Config;
import com.festp.utils.Utils;
import com.google.common.collect.Lists;

public class AmethystManager implements Listener {
	private static final int MAX_HOR = 10, MAX_VERT = 10, MAX_DIAG = 140;
	private static final int UNLOAD_COOLDOWN = 20*60*10;
	private List<AmethystWorld> worlds = new ArrayList<>();
	private int unloadTicks = 0;
	
	public static final int DIAMOND_RADIUS = 10, NETHERITE_RADIUS = 25;
	private List<EntityType> AFRAIDABLE_TYPES = new ArrayList<>();

	public AmethystManager() {
		// TODO use custom block types and radiuses
		// TODO get particle type
		// TODO load types from config files
		AFRAIDABLE_TYPES = Lists.newArrayList(
				EntityType.SKELETON, EntityType.STRAY,
				EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK,
				EntityType.SPIDER, EntityType.CAVE_SPIDER,
				EntityType.CREEPER, EntityType.ZOMBIFIED_PIGLIN
		);
		for (World world : Bukkit.getWorlds()) {
			worlds.add(new AmethystWorld(world));
		}
	}
	
	public void tick()
	{
		unloadTicks++;
		if (unloadTicks >= UNLOAD_COOLDOWN) {
			unloadTicks -= UNLOAD_COOLDOWN;
			tryUnload();
		}
	}
	
	public void tryUnload()
	{
		for (AmethystWorld world : worlds) {
			world.tryUnload();
		}
	}
	
	public AmethystChunk get(Location l)
	{
		Chunk chunk = l.getChunk();
		return getWorld(l).getIfLoaded(chunk.getX(), chunk.getZ());
	}
	
	public String getInfo() {
		String res = "";
		for (int i = 0; i < worlds.size(); i++) {
			if (i > 0)
				res += "\n";
			res += worlds.get(i).getInfo(true);
		}
		return res;
	}
	
	public String getInfo(World w) {
		return getWorld(w).getInfo(false);
	}
	
	// cancel spawn
	@EventHandler(priority=EventPriority.LOWEST)
	public void onEntitySpawn(EntitySpawnEvent event)
	{
		if (event.isCancelled())
			return;

		Entity entity = event.getEntity();
		if (!isCancellable(entity))
			return;

		Location l = event.getLocation();
		AmethystWorld world = getWorld(l);
		if (world.cancelSpawn(l))
		{
			event.setCancelled(true);
			if (entity.getVehicle() != null)
				entity.getVehicle().remove();

			if (Config.showParticles)
			{
				double horizontalSize = 0.1;
				double verticalSize = 0.3;
				double verticalOffset = verticalSize + 0.5;
				l = l.add(0, verticalOffset, 0);
				l.getWorld().spawnParticle(Particle.ASH, l, 20, horizontalSize, verticalSize, horizontalSize);
				l.getWorld().spawnParticle(Particle.SMOKE_NORMAL, l, 25, horizontalSize, verticalSize, horizontalSize, 0.03);
				
				// spit + poof
				// squid ink
			}
		}
	}
	
	public boolean isAfraidable(EntityType type)
	{
		return AFRAIDABLE_TYPES.contains(type);
	}
	public boolean isCancellable(Entity entity)
	{
		EntityType type = entity.getType();
		//if (!isAfraidable(type)) return false;

		// minecarts, armor stands, etc
		if (type == EntityType.PLAYER || !type.isAlive())
			return false;

		// universal tag for inter-plugin interaction
		if (entity.getCustomName() != null)
			return false;
		if (entity instanceof Tameable && ((Tameable)entity).isTamed())
			return false;
		
		// spawn by bucket
		if (Utils.isFish(type) || type == EntityType.AXOLOTL)
			return isPlayerNearby(entity.getLocation(), 12); // extra distance for lagging players
		
		return true;
	}
	public boolean isPlayerNearby(Location l, double radius)
	{
		double radius2 = radius * radius;
		for (Player p : l.getWorld().getPlayers())
		{
			if (p.getLocation().distanceSquared(l) < radius2)
				return true;
		}
		return false;
	}

	public void onChunkGenerate(ChunkPopulateEvent event)
	{
		Chunk chunk = event.getChunk();
		AmethystWorld world = getWorld(event.getWorld());
		world.getOrAdd(chunk.getX(), chunk.getZ(), true);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.isCancelled())
			return;
		
		if (isCancelling(event.getItemInHand().getType())) {
			Block block = event.getBlock();
			getWorld(block.getWorld()).delayUpdate(block, 0, 1);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		onBlockPiston(event, true);
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPistonExtract(BlockPistonExtendEvent event) {
		onBlockPiston(event, false);
	}
	private void onBlockPiston(BlockPistonEvent event, boolean retraction)
	{
		if (event.isCancelled())
			return;
		BlockFace dir = event.getDirection();
		if (retraction)
			dir = dir.getOppositeFace();
		Block bNear = event.getBlock().getRelative(dir);
		Block bFar = bNear.getRelative(dir);
		Block bFrom = bNear, bTo = bFar;
		if (retraction) {
			bFrom = bFar;
			bTo = bNear;
		}
		if (isCancelling(bFrom.getType())) {
			getWorld(bTo.getWorld()).delayUpdate(bTo, 12, 3);
		}
	}
	
	private AmethystWorld getWorld(Location l)
	{
		return getWorld(l.getWorld());
	}
	
	private AmethystWorld getWorld(World w)
	{
		for (AmethystWorld world : worlds)
			if (world.origWorld == w)
				return world;
		AmethystWorld world = new AmethystWorld(w);
		worlds.add(world);
		return world;
	}
	
	/** Check every block in the cube with size <b>MAX_HOR</b><br> but |dx| + |dy| + |dz| &leq; <b>MAX_DIAG</b> */
	public boolean cancelSpawn_Ineffective(Location l)
	{
		Block center = l.getBlock();

		for (int dy = -MAX_VERT; dy <= MAX_VERT; dy++) {
			int x_limit = Math.min(MAX_DIAG - Math.abs(dy), MAX_HOR); 
			for (int dx = -x_limit; dx <= x_limit; dx++) {
				int z_limit = Math.min(MAX_DIAG - Math.abs(dx) - Math.abs(dy), MAX_HOR); 
				for (int dz = -z_limit; dz <= z_limit; dz++) {
					Block b = center.getRelative(dx, dy, dz);
					if (isCancelling(b)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public static boolean isCancelling(Material m) {
		return m == Material.DIAMOND_BLOCK || m == Material.NETHERITE_BLOCK;
	}
	
	static boolean isCancelling(Block b) {
		return b.getChunk().isLoaded() && isCancelling(b.getType()) && b.isBlockPowered();
	}

	// TODO memory actualizing (random scanning?)
	
}
