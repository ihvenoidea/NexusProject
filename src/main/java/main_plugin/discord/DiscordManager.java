package main_plugin.discord;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 마인크래프트 서버와 파이썬 디스코드 봇 사이의 양방향 통신을 담당합니다.
 */
public class DiscordManager {
    private final NexusCore plugin;
    private HttpServer server;

    public DiscordManager(NexusCore plugin) {
        this.plugin = plugin;
        startLocalServer(); // 플러그인이 켜질 때 로컬 수신 서버를 엽니다.
    }

    /**
     * 파이썬 봇의 신호를 받기 위해 5000번 포트에 로컬 서버를 엽니다.
     */
    private void startLocalServer() {
        try {
            // [중요] 포트 5000번 사용. 만약 이 포트가 사용중이라면 다른 번호로 바꿔야 합니다.
            server = HttpServer.create(new InetSocketAddress(5000), 0);
            server.createContext("/nexus/sync", new SyncHandler());
            server.setExecutor(null); 
            server.start();
            plugin.getLogger().info("✅ 파이썬 봇 수신용 로컬 웹서버가 포트 5000에서 열렸습니다.");
        } catch (IOException e) {
            plugin.getLogger().warning("❌ 로컬 웹서버를 열 수 없습니다. 포트 5000이 이미 사용 중일 수 있습니다.");
        }
    }

    /**
     * 플러그인 종료 시 서버도 안전하게 닫아줍니다. (NexusCore.java 의 onDisable() 에서 호출 권장)
     */
    public void stopLocalServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * POST 요청을 처리하는 핸들러
     */
    class SyncHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String jsonStr = reader.lines().collect(Collectors.joining(""));
                    
                    // 아주 간단한 JSON 파싱 (Gson이나 Jackson 없이 처리)
                    if (jsonStr.contains("\"action\": \"sync_dp\"") && jsonStr.contains("\"player\":")) {
                        // "player": "닉네임" 에서 닉네임만 추출
                        String[] parts = jsonStr.split("\"player\": \"");
                        if (parts.length > 1) {
                            String playerName = parts[1].split("\"")[0];
                            
                            // 플레이어가 온라인이면 즉시 DB에서 새로고침
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Player target = Bukkit.getPlayerExact(playerName);
                                if (target != null && target.isOnline()) {
                                    // DatabaseManager의 동기화 메서드 호출
                                    plugin.getDatabaseManager().syncBalanceFromDB(target.getUniqueId());
                                    target.sendMessage("§a[!] 디스코드 출석체크 보상(DP)이 인게임에 즉시 적용되었습니다!");
                                }
                            });
                        }
                    }
                }
                
                String response = "OK";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    /**
     * 도감 항목이 해금되었을 때 파이썬 봇에게 POST 요청을 보냅니다. (기존 유지)
     */
    public void broadcastUnlock(String playerName, String entryName, String grade) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 파이썬 봇이 도감 해금 신호를 받을 주소 (필요시 구현)
                URL url = new URL("http://localhost:5001/nexus/unlock");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = String.format("{\"player\":\"%s\", \"entry\":\"%s\", \"grade\":\"%s\"}", playerName, entryName, grade);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("디스코드 봇 응답 에러: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                // 봇이 꺼져있을 땐 그냥 조용히 무시합니다.
            }
        });
    }
}