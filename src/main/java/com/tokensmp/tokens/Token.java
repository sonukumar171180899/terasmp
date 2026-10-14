package com.tokensmp.tokens;

import com.tokensmp.TokenSMP;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class Token {

    public static final NamespacedKey TOKEN_ID_KEY = new NamespacedKey(TokenSMP.getInstance(), "token_id");
    public static final NamespacedKey TOKEN_DATA_KEY = new NamespacedKey(TokenSMP.getInstance(), "token_data");

    private final String id;
    private final String displayName;
    private final Material material;
    private final Rarity rarity;
    private final List<String> lore;
    private final int health;
    private final boolean enabled;
    private final AbilityConfig primary;
    private final AbilityConfig secondary;
    private final boolean consumable;
    private final int maxLives;

    public Token(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = section.getString("display-name", id);
        this.material = Material.valueOf(section.getString("material", "PAPER"));
        this.rarity = Rarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
        this.lore = section.getStringList("lore");
        this.health = section.getInt("health", 0);
        this.enabled = section.getBoolean("enabled", true);
        
        ConfigurationSection primarySection = section.getConfigurationSection("primary");
        this.primary = primarySection != null ? new AbilityConfig(primarySection) : new AbilityConfig();
        
        ConfigurationSection secondarySection = section.getConfigurationSection("secondary");
        this.secondary = secondarySection != null ? new AbilityConfig(secondarySection) : new AbilityConfig();
        
        this.consumable = section.getBoolean("consumable", false);
        this.maxLives = section.getInt("max-lives", 20);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getHealth() {
        return health;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AbilityConfig getPrimary() {
        return primary;
    }

    public AbilityConfig getSecondary() {
        return secondary;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String processedName = displayName.replace("%max_lives%", String.valueOf(maxLives));
            meta.displayName(net.kyori.adventure.text.Component.text(processedName));
            
            List<String> processedLore = lore.stream()
                    .map(line -> line.replace("%max_lives%", String.valueOf(maxLives)))
                    .toList();
            meta.lore(processedLore.stream()
                    .map(net.kyori.adventure.text.Component::text)
                    .toList());
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(TOKEN_ID_KEY, PersistentDataType.STRING, id);
            container.set(TOKEN_DATA_KEY, PersistentDataType.STRING, "{}");
            
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(TOKEN_ID_KEY, PersistentDataType.STRING);
    }

    public static String getTokenId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.get(TOKEN_ID_KEY, PersistentDataType.STRING);
    }

    public static class AbilityConfig {
        private String ability;
        private String cooldown;
        private int manaCost;

        public AbilityConfig() {
            this.ability = "";
            this.cooldown = "0s";
            this.manaCost = 0;
        }

        public AbilityConfig(ConfigurationSection section) {
            this.ability = section.getString("ability", "");
            this.cooldown = section.getString("cooldown", "0s");
            this.manaCost = section.getInt("mana-cost", 0);
        }

        public String getAbility() {
            return ability;
        }

        public String getCooldown() {
            return cooldown;
        }

        public int getManaCost() {
            return manaCost;
        }

        public long getCooldownTicks() {
            return parseTime(cooldown);
        }

        private long parseTime(String time) {
            if (time == null || time.isEmpty()) return 0;
            time = time.toLowerCase().trim();
            if (time.endsWith("s")) {
                return Long.parseLong(time.substring(0, time.length() - 1)) * 20L;
            } else if (time.endsWith("m")) {
                return Long.parseLong(time.substring(0, time.length() - 1)) * 20L * 60L;
            } else if (time.endsWith("h")) {
                return Long.parseLong(time.substring(0, time.length() - 1)) * 20L * 60L * 60L;
            } else if (time.endsWith("t")) {
                return Long.parseLong(time.substring(0, time.length() - 1));
            }
            return Long.parseLong(time) * 20L;
        }
    }

    public enum Rarity {
        COMMON("<white>"),
        UNCOMMON("<green>"),
        RARE("<blue>"),
        EPIC("<light_purple>"),
        LEGENDARY("<gold>"),
        MYTHIC("<gradient:#ff00ff:#ff69b4>");

        private final String color;

        Rarity(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }

        public String getDisplayName() {
            return color + name() + "</white>";
        }
    }
}