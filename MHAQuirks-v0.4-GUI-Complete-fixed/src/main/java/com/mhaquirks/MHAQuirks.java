package com.mhaquirks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MHAQuirks extends JavaPlugin implements Listener {

    private final Map<UUID, String> quirks = new HashMap<>();
    private final Map<UUID, String> sides = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    private File dataFile;
    private YamlConfiguration data;

    private static final String KINETIC = "KINETIC_MANIPULATION";
    private static final String ONE_FOR_ALL = "ONE_FOR_ALL";
    private static final String HALF_COLD_HALF_HOT = "HALF_COLD_HALF_HOT";

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "players.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        loadPlayers();

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("quirk") != null) {
            getCommand("quirk").setExecutor(this);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!hasQuirk(player, HALF_COLD_HALF_HOT) || !player.isSneaking()) {
                        continue;
                    }

                    Particle particle = "ICE".equals(sides.get(player.getUniqueId()))
                            ? Particle.SNOW_SHOVEL
                            : Particle.FLAME;

                    player.getWorld().spawnParticle(
                            particle,
                            player.getLocation().add(0, 1, 0),
                            12,
                            0.45, 0.8, 0.45,
                            0.02
                    );
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    @Override
    public void onDisable() {
        savePlayers();
    }

    private void loadPlayers() {
        if (data.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidString : data.getConfigurationSection("players").getKeys(false)) {
            try {
                String quirk = data.getString("players." + uuidString + ".quirk");

                if (quirk != null) {
                    quirks.put(UUID.fromString(uuidString), quirk);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void savePlayers() {
        for (Map.Entry<UUID, String> entry : quirks.entrySet()) {
            data.set(
                    "players." + entry.getKey() + ".quirk",
                    entry.getValue()
            );
        }

        try {
            data.save(dataFile);
        } catch (Exception exception) {
            getLogger().warning("Could not save players.yml: " + exception.getMessage());
        }
    }

    private boolean hasQuirk(Player player, String quirk) {
        return quirk.equals(quirks.get(player.getUniqueId()));
    }

    private boolean ready(Player player, String ability, long milliseconds) {
        String key = player.getUniqueId() + ":" + ability;
        long now = System.currentTimeMillis();

        Long previous = cooldowns.get(key);

        if (previous != null && now - previous < milliseconds) {
            long remaining = (milliseconds - (now - previous) + 999L) / 1000L;
            player.sendMessage(ChatColor.RED + "Cooldown: " + remaining + "s");
            return false;
        }

        cooldowns.put(key, now);
        return true;
    }

    private ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + name);
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private void openQuirkGui(Player player) {
        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_PURPLE + "Choose Your Quirk"
        );

        inventory.setItem(
                10,
                item(Material.ENDER_PEARL, "Kinetic Manipulation")
        );

        inventory.setItem(
                13,
                item(Material.NETHER_STAR, "One For All")
        );

        inventory.setItem(
                16,
                item(Material.BLAZE_POWDER, "Half-Cold Half-Hot")
        );

        player.openInventory(inventory);
    }

    private void setQuirk(Player player, String quirk) {
        quirks.put(player.getUniqueId(), quirk);
        sides.put(player.getUniqueId(), "FIRE");

        savePlayers();

        player.sendMessage(
                ChatColor.GREEN + "Quirk selected: " + quirk
        );

        loadout(player);
    }

    private void loadout(Player player) {
        player.getInventory().clear();

        String quirk = quirks.get(player.getUniqueId());

        if (KINETIC.equals(quirk)) {
            player.getInventory().setItem(
                    0, item(Material.NETHER_STAR, "Kinetic Burst")
            );
            player.getInventory().setItem(
                    1, item(Material.IRON_INGOT, "Acceleration")
            );
            player.getInventory().setItem(
                    2, item(Material.IRON_BLOCK, "Deceleration")
            );
            player.getInventory().setItem(
                    3, item(Material.ENDER_PEARL, "Infinite Approach")
            );

        } else if (ONE_FOR_ALL.equals(quirk)) {
            player.getInventory().setItem(
                    0, item(Material.NETHER_STAR, "Smash")
            );
            player.getInventory().setItem(
                    1, item(Material.IRON_BLOCK, "Detroit Smash")
            );
            player.getInventory().setItem(
                    2, item(Material.FEATHER, "Full Cowl")
            );

        } else if (HALF_COLD_HALF_HOT.equals(quirk)) {
            // SNOW_BALL is the legacy 1.12.2 Bukkit Material name.
            player.getInventory().setItem(
                    0, item(Material.BLAZE_POWDER, "Half-Cold Half-Hot")
            );
            player.getInventory().setItem(
                    1, item(Material.ICE, "Ice Spike")
            );
            player.getInventory().setItem(
                    2, item(Material.PACKED_ICE, "Ice Wall")
            );
            player.getInventory().setItem(
                    3, item(Material.FIREBALL, "Flame Blast")
            );
            player.getInventory().setItem(
                    4, item(Material.SNOW_BALL, "Flashfire Frost")
            );
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(
                ChatColor.DARK_PURPLE + "Choose Your Quirk")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String name = item.getItemMeta().getDisplayName();

        if (name.contains("Kinetic")) {
            setQuirk(player, KINETIC);
        } else if (name.contains("One For All")) {
            setQuirk(player, ONE_FOR_ALL);
        } else if (name.contains("Half-Cold")) {
            setQuirk(player, HALF_COLD_HALF_HOT);
        }

        player.closeInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (!isRelevantAction(action)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String name = item.getItemMeta().getDisplayName();

        if (hasQuirk(player, HALF_COLD_HALF_HOT)
                && name.contains("Half-Cold Half-Hot")) {

            if (player.isSneaking() && isLeftClick(action)) {
                sides.put(player.getUniqueId(), "FIRE");
                player.sendMessage(ChatColor.RED + "Fire side selected");
                event.setCancelled(true);
                return;
            }

            if (player.isSneaking() && isRightClick(action)) {
                sides.put(player.getUniqueId(), "ICE");
                player.sendMessage(ChatColor.AQUA + "Ice side selected");
                event.setCancelled(true);
                return;
            }

            if (!player.isSneaking() && isRightClick(action)) {
                if ("ICE".equals(sides.get(player.getUniqueId()))) {
                    ice(player);
                } else {
                    fire(player);
                }

                event.setCancelled(true);
                return;
            }
        }

        if (hasQuirk(player, KINETIC)
                && name.contains("Kinetic Burst")
                && isRightClick(action)) {
            burst(player);

        } else if (hasQuirk(player, KINETIC)
                && name.contains("Acceleration")
                && isRightClick(action)) {
            accelerate(player);

        } else if (hasQuirk(player, KINETIC)
                && name.contains("Deceleration")
                && isRightClick(action)) {
            decelerate(player);

        } else if (hasQuirk(player, KINETIC)
                && name.contains("Infinite Approach")
                && isRightClick(action)) {
            infiniteApproach(player);

        } else if (hasQuirk(player, ONE_FOR_ALL)
                && name.contains("Detroit Smash")
                && isRightClick(action)) {
            detroitSmash(player);

        } else if (hasQuirk(player, ONE_FOR_ALL)
                && name.equals(ChatColor.AQUA + "Smash")
                && isRightClick(action)) {
            smash(player);

        } else if (hasQuirk(player, ONE_FOR_ALL)
                && name.contains("Full Cowl")
                && isRightClick(action)) {
            fullCowl(player);

        } else if (hasQuirk(player, HALF_COLD_HALF_HOT)
                && name.contains("Ice Spike")
                && isRightClick(action)) {
            iceSpike(player);

        } else if (hasQuirk(player, HALF_COLD_HALF_HOT)
                && name.contains("Ice Wall")
                && isRightClick(action)) {
            iceWall(player);

        } else if (hasQuirk(player, HALF_COLD_HALF_HOT)
                && name.contains("Flame Blast")
                && isRightClick(action)) {
            fire(player);

        } else if (hasQuirk(player, HALF_COLD_HALF_HOT)
                && name.contains("Flashfire Frost")
                && isRightClick(action)) {
            frost(player);
        }
    }

    private boolean isRelevantAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK
                || action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK;
    }

    private void fire(Player player) {
        if (!ready(player, "fire", 1200L)) {
            return;
        }

        SmallFireball fireball =
                player.launchProjectile(SmallFireball.class);

        fireball.setMetadata(
                "MHA_FIRE",
                new org.bukkit.metadata.FixedMetadataValue(this, true)
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_BLAZE_SHOOT,
                1F,
                1F
        );
    }

    private void ice(Player player) {
        if (!ready(player, "ice", 1200L)) {
            return;
        }

        Snowball snowball =
                player.launchProjectile(Snowball.class);

        snowball.setMetadata(
                "MHA_ICE",
                new org.bukkit.metadata.FixedMetadataValue(this, true)
        );
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)
                || !(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Projectile projectile = (Projectile) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        if (projectile.hasMetadata("MHA_FIRE")) {
            target.setFireTicks(80);
            event.setDamage(5);
        }

        if (projectile.hasMetadata("MHA_ICE")) {
            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOW,
                            100,
                            4
                    )
            );

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.JUMP,
                            100,
                            128
                    )
            );

            event.setDamage(1);
        }
    }

    private void burst(Player player) {
        if (!ready(player, "kb", 2500L)) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                Vector velocity = entity.getLocation()
                        .toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(1.7);

                velocity.setY(0.6);

                entity.setVelocity(velocity);
                ((LivingEntity) entity).damage(4, player);
            }
        }

        player.getWorld().spawnParticle(
                Particle.CRIT_MAGIC,
                player.getLocation(),
                30,
                1, 1, 1,
                0.1
        );
    }

    private void accelerate(Player player) {
        if (!ready(player, "acc", 1500L)) {
            return;
        }

        player.setVelocity(
                player.getLocation()
                        .getDirection()
                        .multiply(1.8)
                        .setY(0.2)
        );
    }

    private void decelerate(Player player) {
        if (!ready(player, "dec", 1500L)) {
            return;
        }

        player.setVelocity(new Vector(0, 0, 0));
    }

    private void infiniteApproach(Player player) {
        if (!ready(player, "inf", 5000L)) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(8, 4, 8)) {
            if (entity instanceof Projectile) {
                entity.setVelocity(new Vector(0, 0, 0));
            }
        }

        player.sendMessage(
                ChatColor.LIGHT_PURPLE + "Infinite Approach active."
        );
    }

    private void smash(Player player) {
        if (!ready(player, "smash", 1800L)) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(6, player);
            }
        }
    }

    private void detroitSmash(Player player) {
        if (!ready(player, "det", 6000L)) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(7, 3, 7)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(12, player);

                Vector velocity = entity.getLocation()
                        .toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(2.3);

                velocity.setY(1);
                entity.setVelocity(velocity);
            }
        }
    }

    private void fullCowl(Player player) {
        if (!ready(player, "cowl", 12000L)) {
            return;
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        240,
                        1
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.JUMP,
                        240,
                        1
                )
        );
    }

    private void iceSpike(Player player) {
        if (!ready(player, "spike", 2200L)) {
            return;
        }

        player.getWorld().spawnParticle(
                Particle.SNOW_SHOVEL,
                player.getTargetBlock((Set<Material>) null, 12)
                        .getLocation()
                        .add(0, 1, 0),
                40,
                0.5, 0.5, 0.5,
                0.02
        );
    }

    private void iceWall(Player player) {
        if (!ready(player, "wall", 5000L)) {
            return;
        }

        player.getWorld().spawnParticle(
                Particle.SNOW_SHOVEL,
                player.getTargetBlock((Set<Material>) null, 8)
                        .getLocation()
                        .add(0, 1, 0),
                80,
                1, 1, 1,
                0.02
        );
    }

    private void frost(Player player) {
        if (!ready(player, "frost", 5000L)) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(6, 3, 6)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;

                target.damage(4, player);

                target.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SLOW,
                                120,
                                3
                        )
                );
            }
        }

        player.getWorld().spawnParticle(
                Particle.SNOW_SHOVEL,
                player.getLocation(),
                40,
                1, 1, 1,
                0.1
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0
                || args[0].equalsIgnoreCase("select")) {
            openQuirkGui(player);

        } else if (args[0].equalsIgnoreCase("loadout")) {
            loadout(player);

        } else if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage(
                    ChatColor.GOLD
                    + "Quirks: Kinetic Manipulation | One For All | Half-Cold Half-Hot"
            );

        } else if (args[0].equalsIgnoreCase("set")
                && args.length >= 3
                && player.hasPermission("mhaquirks.admin")) {

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            String quirk = args[2].toUpperCase();

            if (quirk.equals("KINETIC")) {
                quirk = KINETIC;
            } else if (quirk.equals("ONE_FOR_ALL")) {
                quirk = ONE_FOR_ALL;
            } else if (quirk.equals("HALF_COLD_HALF_HOT")) {
                quirk = HALF_COLD_HALF_HOT;
            } else {
                player.sendMessage(ChatColor.RED + "Unknown quirk.");
                return true;
            }

            setQuirk(target, quirk);
        }

        return true;
    }
}
