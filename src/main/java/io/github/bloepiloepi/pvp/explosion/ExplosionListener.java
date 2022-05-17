package io.github.bloepiloepi.pvp.explosion;

import io.github.bloepiloepi.pvp.utils.SoundManager;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jglrxavpok.hephaistos.nbt.NBT;

public class ExplosionListener {
	
	public static EventNode<EntityEvent> events() {
		EventNode<EntityEvent> node = EventNode.type("explosion-events", EventFilter.ENTITY);
		
		node.addListener(PlayerUseItemOnBlockEvent.class, event -> {
			if (event.getItemStack().material() != Material.END_CRYSTAL) return;
			Instance instance = event.getInstance();
			Block block = instance.getBlock(event.getPosition());
			if (!block.compare(Block.OBSIDIAN) && !block.compare(Block.BEDROCK)) return;
			
			Point above = event.getPosition().add(0, 1, 0);
			if (!instance.getBlock(above).isAir()) return;
			
			BoundingBox checkIntersect = new BoundingBox(1, 2, 1);
			for (Entity entity : instance.getNearbyEntities(above, 3)) {
				if (entity.getBoundingBox().intersectBox(above.sub(entity.getPosition()), checkIntersect)) return;
			}
			
			CrystalEntity entity = new CrystalEntity();
			entity.setInstance(instance, above.add(0.5, 0, 0.5));
			
			if (!event.getPlayer().isCreative())
				event.getPlayer().setItemInHand(event.getHand(), event.getItemStack().consume(1));
		});
		
		node.addListener(PlayerBlockInteractEvent.class, event -> {
			Instance instance = event.getInstance();
			Block block = instance.getBlock(event.getBlockPosition());
			Player player = event.getPlayer();
			if (!block.compare(Block.RESPAWN_ANCHOR)) return;
			
			// Exit if offhand has glowstone but current hand is main
			if (event.getHand() == Player.Hand.MAIN
					&& player.getItemInMainHand().material() != Material.GLOWSTONE
					&& player.getItemInOffHand().material() == Material.GLOWSTONE)
				return;
			
			ItemStack stack = player.getItemInHand(event.getHand());
			int charges = Integer.parseInt(block.getProperty("charges"));
			if (stack.material() == Material.GLOWSTONE && charges < 4) {
				instance.setBlock(event.getBlockPosition(),
						block.withProperty("charges", String.valueOf(charges + 1)));
				SoundManager.sendToAround(
						instance, event.getBlockPosition().add(0.5, 0.5, 0.5),
						SoundEvent.BLOCK_RESPAWN_ANCHOR_CHARGE, Sound.Source.BLOCK,
						1.0f, 1.0f
				);
				
				if (!player.isCreative())
					player.setItemInHand(event.getHand(), player.getItemInHand(event.getHand()).consume(1));
				
				event.setBlockingItemUse(true);
				return;
			}
			
			if (charges == 0) return;
			
			if (!instance.getDimensionType().isRespawnAnchorSafe()) {
				instance.setBlock(event.getBlockPosition(), Block.AIR);
				instance.explode(
						(float) (event.getBlockPosition().x() + 0.5),
						(float) (event.getBlockPosition().y() + 0.5),
						(float) (event.getBlockPosition().z() + 0.5),
						5.0f,
						NBT.Compound(NBT -> NBT.setByte("fire", (byte) 1).setByte("anchor", (byte) 1))
				);
			}
			
			event.setBlockingItemUse(true);
		});
		
		return node;
	}
}
