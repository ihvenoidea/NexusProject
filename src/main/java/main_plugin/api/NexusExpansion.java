package main_plugin.api;

import main_plugin.NexusCore;
import main_plugin.user.UserData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Optional;

public class NexusExpansion extends PlaceholderExpansion {

    private final NexusCore plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###"); // 천 단위 콤마 포맷

    public NexusExpansion(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexus"; // PAPI 접두사: %nexus_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "Admin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    // [핵심 추가] PAPI가 이 클래스를 확신하고 등록할 수 있게 해주는 필수 메서드!
    @Override
    public boolean canRegister() {
        return true;
    }

    // PAPI 리로드 시에도 이 확장 기능이 꺼지지 않게 유지합니다.
    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // DB가 아니라 메모리(UserManager)에서 즉시 데이터를 가져옵니다. (렉 제로)
        Optional<UserData> userOpt = plugin.getUserManager().getUser(player.getUniqueId());

        if (userOpt.isEmpty()) {
            return "0"; // 데이터가 아직 안 불러와졌으면 0 처리
        }

        UserData user = userOpt.get();

        // 1. DP (포인트) 불러오기 -> %nexus_dp%
        if (params.equalsIgnoreCase("dp")) {
            return formatter.format(user.getPoints());
        }

        // 2. 돈 불러오기 (혹시 몰라 넥서스 전용도 만듦) -> %nexus_money%
        if (params.equalsIgnoreCase("money")) {
            return formatter.format(user.getMoney());
        }

        // 3. 누적 조공량 불러오기 -> %nexus_tribute%
        if (params.equalsIgnoreCase("tribute")) {
            return formatter.format(user.getTotalTribute());
        }

        return null; // 알 수 없는 변수명일 경우
    }
}