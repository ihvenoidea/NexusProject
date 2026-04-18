package main_plugin.user;

import main_plugin.NexusCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final NexusCore plugin;

    public PlayerListener(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 유저가 서버에 접속할 때 호출됩니다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuidStr = player.getUniqueId().toString();
        String name = player.getName();

        // 1. [DB 처리] 데이터가 없으면 'INSERT IGNORE'로 초기 행 생성
        // 이 과정이 선행되어야 관리자가 돈을 지급할 때 '유저 없음' 에러가 나지 않습니다.
        plugin.getDatabaseManager().setupPlayer(uuidStr, name);

        // 2. [메모리 로드] UserManager를 통해 유저 객체를 생성하고 데이터를 불러옵니다.
        // 수정됨: 인자로 UUID와 닉네임(name)을 모두 전달합니다.
        if (plugin.getUserManager() != null) {
            plugin.getUserManager().loadUserData(player.getUniqueId(), name);
        }

        plugin.getLogger().info("[Join] " + name + "님의 데이터 초기화 및 로드 완료.");
    }

    /**
     * 유저가 서버를 나갈 때 호출됩니다.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 유저가 나갈 때 메모리에 있던 변경사항을 DB에 최종 저장합니다.
        if (plugin.getUserManager() != null) {
            plugin.getUserManager().getUser(player.getUniqueId()).ifPresent(user -> {
                plugin.getUserManager().saveUserData(user);
                plugin.getUserManager().removeUser(player.getUniqueId());
            });
        }
        
        plugin.getLogger().info("[Quit] " + player.getName() + "님의 데이터를 안전하게 저장했습니다.");
    }
}