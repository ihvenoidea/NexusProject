package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 심연의 시련(불길한 금고) 클리어 시 지정된 확률로 
 * 넥서스 세트 방어구를 드랍하는 리스너입니다.
 */
public class AbyssalTrialListener implements Listener {

    private final NexusCore plugin;
    private final Random random;

    public AbyssalTrialListener(NexusCore plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler
    public void onVaultDispense(BlockDispenseLootEvent event) {
        Block block = event.getBlock();
        
        // 1. 금고(Vault)인지 확인
        if (block.getType() == Material.VAULT) {
            
            // 2. 불길한(Ominous) 상태인지 확인
            if (block.getBlockData() instanceof Vault vaultData) {
                if (vaultData.isOminous()) {
                    
                    List<ItemStack> currentLoot = event.getDispensedLoot();
                    List<ItemStack> newLoot = new ArrayList<>(currentLoot);
                    
                    // 3. 확률에 따라 생성된 세트 방어구를 전리품에 추가
                    newLoot.add(getRandomNexusPiece());
                    
                    event.setDispensedLoot(newLoot);
                    
                    Player player = event.getPlayer();
                    if (player != null) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                        player.sendMessage("§5§l[심연의 시련] §d불길한 금고에서 넥서스 장비가 뿜어져 나옵니다!");
                    }
                }
            }
        }
    }

    /**
     * 지정된 확률에 따라 세트 방어구를 생성하여 반환합니다.
     * 실버(70%), 골드(24%), 프리즘(5%), 신화(1%)
     */
    private ItemStack getRandomNexusPiece() {
        // 0.0 ~ 100.0 사이의 난수 생성
        double chance = random.nextDouble() * 100.0;
        String setName;
        
        // 1. 확률에 따른 등급(세트 이름) 결정
        if (chance < 70.0) {
            setName = "신속"; // 70% (실버)
        } else if (chance < 94.0) {
            setName = "방어"; // 24% (골드)
        } else if (chance < 99.0) {
            setName = "격류"; // 5% (프리즘)
        } else {
            setName = "황금"; // 1% (신화)
        }

        // 2. 부위 랜덤 결정 (방어구 4종)
        String[] parts = {"투구", "갑옷", "각반", "장화"};
        String part = parts[random.nextInt(parts.length)];

        // 3. SetItemManager를 통해 아이템을 온전한 상태로 생성하여 반환
        return plugin.getSetItemManager().createSetItem(setName, part);
    }
}