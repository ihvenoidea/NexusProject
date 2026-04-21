package main_plugin.mail;

import main_plugin.NexusCore;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.util.Base64;

public class MailManager {
    
    private final NexusCore plugin;

    public MailManager(NexusCore plugin) {
        this.plugin = plugin;
    }

    public void sendMail(String receiver, ItemStack item, String message) {
        String itemBase64 = itemStackToBase64(item);
        String sql = "INSERT INTO user_mail (receiver, item_data, message) VALUES (?, ?, ?)";
        
        // [수정됨] Connection 자동 종료 방지
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
            ps.setString(1, receiver);
            ps.setString(2, itemBase64);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (Exception e) { 
            plugin.getLogger().warning("우편 발송 중 오류 발생: " + e.getMessage());
        }
    }

    public void sendMailToAll(ItemStack item, String message) {
        String itemBase64 = itemStackToBase64(item);
        String sql = "INSERT INTO user_mail (receiver, item_data, message) SELECT name, ?, ? FROM users";
        
        // [수정됨] Connection 자동 종료 방지
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
            ps.setString(1, itemBase64);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (Exception e) { 
            plugin.getLogger().warning("전체 우편 발송 중 오류 발생: " + e.getMessage());
        }
    }

    public void deleteMail(int mailId) {
        String sql = "DELETE FROM user_mail WHERE id = ?";
        // [수정됨] Connection 자동 종료 방지
        try (PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(sql)) {
            ps.setInt(1, mailId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static String itemStackToBase64(ItemStack item) {
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) { 
            e.printStackTrace(); 
            return ""; 
        }
    }

    public static ItemStack itemStackFromBase64(String data) {
        try {
            if (data == null || data.isEmpty()) return null;
            byte[] bytes = Base64.getDecoder().decode(data);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) { 
            return null; 
        }
    }
}