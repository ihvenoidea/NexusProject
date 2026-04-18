package main_plugin.discord;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 마인크래프트 서버와 파이썬 디스코드 봇(main.py) 사이의 통신을 담당합니다.
 */
public class DiscordManager {
    private final NexusCore plugin;

    public DiscordManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 도감 항목이 해금되었을 때 파이썬 봇에게 POST 요청을 보냅니다.
     * * @param playerName 플레이어 닉네임
     * @param entryName  해금된 항목 이름
     * @param grade      항목의 등급 (common, rare, legendary, synergy)
     */
    public void broadcastUnlock(String playerName, String entryName, String grade) {
        // 메인 스레드 렉 방지를 위해 비동기로 실행합니다.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // main.py에 추가할 웹 서버 주소 (5000번 포트 사용 가정)
                URL url = new URL("http://localhost:5000/nexus/unlock");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // 파이썬 봇이 이해할 수 있는 JSON 데이터 생성
                String json = String.format(
                    "{\"player\":\"%s\", \"entry\":\"%s\", \"grade\":\"%s\"}",
                    playerName, entryName, grade
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                // 응답 확인 (정상 수신 여부)
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("디스코드 봇 응답 에러: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("디스코드 봇 서버가 꺼져 있거나 연결할 수 없습니다.");
            }
        });
    }
}