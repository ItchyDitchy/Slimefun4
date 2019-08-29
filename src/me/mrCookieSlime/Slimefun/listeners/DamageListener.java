package me.mrCookieSlime.Slimefun.listeners;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import me.mrCookieSlime.EmeraldEnchants.EmeraldEnchants;
import me.mrCookieSlime.EmeraldEnchants.ItemEnchantment;
import me.mrCookieSlime.Slimefun.SlimefunStartup;
import me.mrCookieSlime.Slimefun.Lists.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SoulboundItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.Talisman;
import me.mrCookieSlime.Slimefun.Setup.SlimefunManager;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.Soul;
import me.mrCookieSlime.Slimefun.utils.Utilities;

public class DamageListener implements Listener {

    private SimpleDateFormat format = new SimpleDateFormat("(MMM d, yyyy @ hh:mm)");
	private Utilities utilities;
	private Random random = new Random();

    public DamageListener(SlimefunStartup plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        utilities = plugin.getUtilities();
    }

    @EventHandler
    public void onDamage(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (p.getInventory().containsAtLeast(SlimefunItems.GPS_EMERGENCY_TRANSMITTER, 1)) {
                Slimefun.getGPSNetwork().addWaypoint(p, "&4Deathpoint &7" + format.format(new Date()), p.getLocation().getBlock().getLocation());
            }
            
            Iterator<ItemStack> drops = e.getDrops().iterator();
            while (drops.hasNext()) {
                ItemStack item = drops.next();
                if (item != null) {
                    if (SlimefunManager.isItemSimiliar(item, SlimefunItems.BOUND_BACKPACK, false)) {
                        Soul.storeItem(e.getEntity().getUniqueId(), item);
                        drops.remove();
                    } 
                    else if (SlimefunItem.getByItem(removeEnchantments(item)) instanceof SoulboundItem) {
                        Soul.storeItem(e.getEntity().getUniqueId(), item);
                        drops.remove();
                    }
                }
            }

        }
        if (e.getEntity().getKiller() instanceof Player) {
            Player p = (Player) e.getEntity().getKiller();
            ItemStack item = p.getInventory().getItemInMainHand();

            if (SlimefunManager.drops.containsKey(e.getEntity().getType())) {
                for (ItemStack drop : SlimefunManager.drops.get(e.getEntity().getType())) {
                    if (Slimefun.hasUnlocked(p, item, true)) {
                        e.getDrops().add(drop);
                    }
                }
            }

            if (item != null && Slimefun.hasUnlocked(p, item, true) && SlimefunManager.isItemSimiliar(item, SlimefunItem.getItem("SWORD_OF_BEHEADING"), true)) {
                if (e.getEntity() instanceof Zombie) {
                    if (random.nextInt(100) < (Integer) Slimefun.getItemValue("SWORD_OF_BEHEADING", "chance.ZOMBIE")) {
                        e.getDrops().add(new ItemStack(Material.ZOMBIE_HEAD));
                    }
                }
                else if (e.getEntity() instanceof WitherSkeleton) {
                    if (random.nextInt(100) < (Integer) Slimefun.getItemValue("SWORD_OF_BEHEADING", "chance.WITHER_SKELETON"))
                        e.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL));
                }
                else if (e.getEntity() instanceof Skeleton) {
                    if (random.nextInt(100) < (Integer) Slimefun.getItemValue("SWORD_OF_BEHEADING", "chance.SKELETON"))
                        e.getDrops().add(new ItemStack(Material.SKELETON_SKULL));
                }
                else if (e.getEntity() instanceof Creeper) {
                    if (random.nextInt(100) < (Integer) Slimefun.getItemValue("SWORD_OF_BEHEADING", "chance.CREEPER")) {
                        e.getDrops().add(new ItemStack(Material.CREEPER_HEAD));
                    }
                }
                else if (e.getEntity() instanceof Player && random.nextInt(100) < (Integer) Slimefun.getItemValue("SWORD_OF_BEHEADING", "chance.PLAYER")) {
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    ItemMeta meta = skull.getItemMeta();
                    ((SkullMeta) meta).setOwningPlayer((Player) e.getEntity());
                    skull.setItemMeta(meta);

                    e.getDrops().add(skull);
                }
            }

            if (!e.getEntity().getCanPickupItems() && Talisman.checkFor(e, SlimefunItem.getByID("HUNTER_TALISMAN")) && !(e.getEntity() instanceof Player)) {
            	
                List<ItemStack> newDrops = new ArrayList<>();
                for (ItemStack drop : e.getDrops()) {
                	newDrops.add(drop);
                }
                for (ItemStack drop : newDrops) {
                    e.getDrops().add(drop);
                }
                
            	if(e.getEntity() instanceof ChestedHorse) {
            		for(ItemStack invItem : ((ChestedHorse) e.getEntity()).getInventory().getStorageContents()) {
            			e.getDrops().remove(invItem);
            		}
            		e.getDrops().remove(new ItemStack(Material.CHEST)); //The chest is not included in getStorageContents()
            	}
            }
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == DamageCause.FALL && utilities.damage.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            utilities.damage.remove(e.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Soul.retrieveItems(e.getPlayer());
    }

    private ItemStack removeEnchantments(ItemStack itemStack) {
        ItemStack strippedItem = itemStack.clone();

        for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
            strippedItem.removeEnchantment(enchantment);
        }

        if (Slimefun.isEmeraldEnchantsInstalled()) {
            for(ItemEnchantment enchantment : EmeraldEnchants.getInstance().getRegistry().getEnchantments(itemStack)){
                EmeraldEnchants.getInstance().getRegistry().applyEnchantment(strippedItem, enchantment.getEnchantment(), 0);
            }
        }
        return strippedItem;
    }
}
