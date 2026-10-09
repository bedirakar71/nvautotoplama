package com.zornoely.nvautotoplama;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public final class NVAutoToplama extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        // Her 1 saniyede bir kontrol edip gece görüşünü yeniler (Totem patlasa bile silinmez)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyNightVision(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info("NVAutoToplama aktif edildi!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NVAutoToplama deaktif edildi.");
    }

    private void applyNightVision(Player player) {
        if (player == null || !player.isOnline()) return;
        PotionEffect current = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (current == null || current.getDuration() < 200 || current.getAmplifier() != 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyNightVision(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> applyNightVision(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Bukkit.getScheduler().runTaskLater(this, () -> applyNightVision(player), 2L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        if (drops.isEmpty()) return;

        event.setDropItems(false);

        for (ItemStack drop : drops) {
            var leftover = player.getInventory().addItem(drop);
            if (!leftover.isEmpty()) {
                for (ItemStack unadded : leftover.values()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), unadded);
                }
            }
        }

        block.setType(Material.AIR);
        
        if (event.getExpToDrop() > 0) {
            player.giveExp(event.getExpToDrop());
            event.setExpToDrop(0);
        }
    }
}
