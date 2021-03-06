package studio.trc.bukkit.litesignin.reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import studio.trc.bukkit.litesignin.api.Storage;
import studio.trc.bukkit.litesignin.config.ConfigurationType;
import studio.trc.bukkit.litesignin.config.ConfigurationUtil;
import studio.trc.bukkit.litesignin.config.MessageUtil;
import studio.trc.bukkit.litesignin.queue.SignInQueue;
import studio.trc.bukkit.litesignin.reward.util.SignInSound;
import studio.trc.bukkit.litesignin.util.PluginControl;
import studio.trc.bukkit.litesignin.util.SignInPluginProperties;

public abstract class SignInRewardUtil
    implements SignInReward
{
    @Override
    public void giveReward(Storage playerData) {
        String queue = String.valueOf(SignInQueue.getInstance().getRank(playerData.getUserUUID()));
        if (playerData.getPlayer() != null) {
            Player player = playerData.getPlayer();
            for (String taskName : ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getStringList("Reward-Task-Sequence")) {
                try {
                    switch (SignInRewardTask.valueOf(taskName.toUpperCase())) {
                        case ITEMS_REWARD: {
                            player.getInventory().addItem(getRewardItems(player).toArray(new ItemStack[0]));
                            break;
                        }
                        case COMMANDS_EXECUTION: {
                            getCommands().stream().forEach(commands -> {commands.runWithThePlayer(player);});
                            break;
                        }
                        case MESSAGES_SENDING: {
                            getMessages().stream().forEach(messages -> {
                                Map<String, String> placeholders = new HashMap();
                                placeholders.put("{continuous}", String.valueOf(playerData.getContinuousSignIn()));
                                placeholders.put("{queue}", queue);
                                placeholders.put("{total-number}", String.valueOf(playerData.getCumulativeNumber()));
                                placeholders.put("{player}", player.getName());
                                player.sendMessage(MessageUtil.toColor(MessageUtil.replacePlaceholders(player, messages, placeholders)));
                            });
                            break;
                        }
                        case BROADCAST_MESSAGES_SENDING: {
                            getBroadcastMessages().stream().forEach(messages -> {
                                Bukkit.getOnlinePlayers().stream().forEach(players -> {
                                    Map<String, String> placeholders = new HashMap();
                                    placeholders.put("{continuous}", String.valueOf(playerData.getContinuousSignIn()));
                                    placeholders.put("{queue}", queue);
                                    placeholders.put("{total-number}", String.valueOf(playerData.getCumulativeNumber()));
                                    placeholders.put("{player}", player.getName());
                                    players.sendMessage(MessageUtil.toColor(MessageUtil.replacePlaceholders(player, messages, placeholders)));
                                });
                            });
                            break;
                        }
                        case PLAYSOUNDS: {
                            getSounds().stream().forEach(sounds -> {sounds.playSound(player);});
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public List<ItemStack> getRewardItems(Player player, String configPath) {
        List<ItemStack> list = new ArrayList();
        if (ConfigurationUtil.getConfig(ConfigurationType.REWARDSETTINGS).contains(configPath)) {
            for (String itemData : ConfigurationUtil.getConfig(ConfigurationType.REWARDSETTINGS).getStringList(configPath)) {
                ItemStack item = getItemFromItemData(player, itemData);
                if (item != null) list.add(item);
            }
        }
        return list;
    }
    
    public ItemStack getItemFromItemData(Player player, String item) {
        String[] itemdata = item.split(":");
        try {
            ItemStack is = new ItemStack(Material.valueOf(itemdata[0].toUpperCase()));
            try {
                if (itemdata[1].contains("-")) {
                    is.setAmount(PluginControl.getRandom(itemdata[1]));
                } else {
                    is.setAmount(Integer.valueOf(itemdata[1]));
                }
            } catch (NumberFormatException ex) {}
            return is;
        } catch (IllegalArgumentException e) {
            if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Manual-Settings." + itemdata[0] + ".Item")) {
                ItemStack is;
                try {
                    if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Manual-Settings." + itemdata[0] + ".Data")) {
                        is = new ItemStack(Material.valueOf(ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getString("Manual-Settings." + itemdata[0] + ".Item").toUpperCase()), 1, (short) ConfigurationUtil.getConfig(ConfigurationType.REWARDSETTINGS).getInt("Reward-Items." + itemdata[0] + ".Data"));
                    } else {
                        is = new ItemStack(Material.valueOf(ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getString("Manual-Settings." + itemdata[0] + ".Item").toUpperCase()), 1);
                    }
                } catch (IllegalArgumentException ex2) {
                    return null;
                }
                if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).get("Manual-Settings." + itemdata[0] + ".Head-Owner") != null) {
                    Map<String, String> placeholders = new HashMap();
                    placeholders.put("{player}", player.getName());
                    PluginControl.setHead(is, MessageUtil.replacePlaceholders(player, ConfigurationUtil.getConfig(ConfigurationType.GUISETTINGS).getString("Manual-Settings." + itemdata[0] + ".Head-Owner"), placeholders));
                }
                ItemMeta im = is.getItemMeta();
                if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Manual-Settings." + itemdata[0] + ".Lore")) {
                    List<String> lore = new ArrayList();
                    for (String lores : ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getStringList("Manual-Settings." + itemdata[0] + ".Lore")) {
                        lore.add(MessageUtil.toColor(MessageUtil.toPlaceholderAPIResult(lores, player)));
                    }
                    im.setLore(lore);
                }
                if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Manual-Settings." + itemdata[0] + ".Enchantment")) {
                    for (String name : ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getStringList("Manual-Settings." + itemdata[0] + ".Enchantment")) {
                        String[] data = name.split(":");
                        for (Enchantment enchant : Enchantment.values()) {
                            if (enchant.getName().equalsIgnoreCase(data[0])) {
                                try {
                                    im.addEnchant(enchant, Integer.valueOf(data[1]), true);
                                } catch (NumberFormatException ex) {}
                            }
                        }
                    }
                }
                if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).get("Manual-Settings." + itemdata[0] + ".Hide-Enchants") != null) PluginControl.hideEnchants(im);
                if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Manual-Settings." + itemdata[0] + ".Display-Name")) im.setDisplayName(MessageUtil.toColor(MessageUtil.toPlaceholderAPIResult(ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getString("Manual-Settings." + itemdata[0] + ".Display-Name"), player)));
                is.setItemMeta(im);
                try {
                    if (itemdata[1].contains("-")) {
                        is.setAmount(PluginControl.getRandom(itemdata[1]));
                    } else {
                        is.setAmount(Integer.valueOf(itemdata[1]));
                    }
                } catch (NumberFormatException ex) {
                    is.setAmount(1);
                }
                return is;
            } else if (ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).contains("Item-Collection." + itemdata[0])) {
                ItemStack is = ConfigurationUtil.getConfig(ConfigurationType.CUSTOMITEMS).getItemStack("Item-Collection." + itemdata[0]);
                if (is != null) {
                    try {
                        if (itemdata[1].contains("-")) {
                            is.setAmount(PluginControl.getRandom(itemdata[1]));
                        } else {
                            is.setAmount(Integer.valueOf(itemdata[1]));
                        }
                    } catch (NumberFormatException ex) {
                        is.setAmount(1);
                    }
                    return is;
                }
            }
        }
        return null;
    }
    
    public List<SignInSound> getSounds(String configPath) {
        List<SignInSound> sounds = new ArrayList();
        if (ConfigurationUtil.getConfig(ConfigurationType.REWARDSETTINGS).contains(configPath)) {
            for (String value : ConfigurationUtil.getConfig(ConfigurationType.REWARDSETTINGS).getStringList(configPath)) {
                String[] args = value.split("-");
                try {
                    Sound sound = Sound.valueOf(args[0].toUpperCase());
                    float volume = Float.valueOf(args[1]);
                    float pitch = Float.valueOf(args[2]);
                    boolean broadcast = Boolean.valueOf(args[3]);
                    sounds.add(new SignInSound(sound, volume, pitch, broadcast));
                } catch (IllegalArgumentException ex) {
                    Map<String, String> placeholders = new HashMap();
                    placeholders.put("{sound}", args[0]);
                    SignInPluginProperties.sendOperationMessage("InvalidSound", placeholders);
                } catch (Exception ex) {
                    Map<String, String> placeholders = new HashMap();
                    placeholders.put("{path}", configPath + "." + value);
                    SignInPluginProperties.sendOperationMessage("InvalidSoundSetting", placeholders);
                }
            } 
        }
        return sounds;
    }
}
