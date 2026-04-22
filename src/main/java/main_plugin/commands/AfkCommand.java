package main_plugin.commands;

import main_plugin.NexusCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class AfkCommand implements CommandExecutor {

    private final NexusCore plugin;

    public AfkCommand(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("게임 내에서만 사용할 수 있습니다.");
            return true;
        }

        player.sendMessage(ChatColor.AQUA + "잠수 서버로 이동합니다! 잠시만 기다려주세요...");

        // [프록시 통신] BungeeCord 채널을 통해 서버 이동 명령을 보냅니다.
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect"); // 행동: 연결해라
        out.writeUTF("afk");     // 대상: velocity.toml에 적었던 잠수 서버의 별명("afk")

        // 플레이어 객체를 통해 프록시로 메시지 전송
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

        return true;
    }
}