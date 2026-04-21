package main_plugin.traits;

import main_plugin.NexusCore;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ShardExchangeTrait extends Trait {

    // 파편을 인식하기 위한 전용 NBT 키
    public static final String SHARD_KEY = "abyssal_shard";

    public ShardExchangeTrait() {
        super("shard_exchange");
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (event.getNPC() != this.getNPC()) return;

        Player player = event.getClicker();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 흉조의 열쇠 5개 확인
        if (itemInHand.getType() == Material.OMINOUS_TRIAL_KEY && itemInHand.getAmount() >= 5) {
            itemInHand.setAmount(itemInHand.getAmount() - 5);
            
            // 파편 1개 생성 및 지급
            player.getInventory().addItem(createAbyssalShard(1));

            player.sendMessage("§5§l[교환] §f흉조의 열쇠 5개를 §d흉조의 파편 1개§f로 교환했습니다!");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        } else {
            player.sendMessage("§c[!] 흉조의 열쇠 5개를 손에 들고 우클릭해주세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // 흉조의 파편 아이템을 생성하는 메서드
    public static ItemStack createAbyssalShard(int amount) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§l[ 흉조의 파편 ]");
            meta.setLore(List.of(
                "§7NPC를 통해 흉조의 기운을 응축시킨 조각입니다.", 
                "§7스폰의 가챠 상자에서 장비로 교환할 수 있습니다."
            ));
            NamespacedKey key = new NamespacedKey(NexusCore.getInstance(), SHARD_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}