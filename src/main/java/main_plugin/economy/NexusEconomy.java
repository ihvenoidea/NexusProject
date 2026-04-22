package main_plugin.economy;

import main_plugin.NexusCore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public class NexusEconomy implements Economy {

    private final NexusCore plugin;

    public NexusEconomy(NexusCore plugin) {
        this.plugin = plugin;
    }

    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "NexusEconomy"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 0; }
    @Override public String format(double amount) { return String.format("%,.0f원", amount); }
    @Override public String currencyNamePlural() { return "원"; }
    @Override public String currencyNameSingular() { return "원"; }

    @Override
    public double getBalance(OfflinePlayer player) {
        return plugin.getDatabaseManager().getMoney(player.getUniqueId().toString());
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "음수는 뺄 수 없습니다.");
        if (plugin.getDatabaseManager().deductMoney(player.getUniqueId().toString(), amount)) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "잔액이 부족합니다.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "음수는 더할 수 없습니다.");
        if (plugin.getDatabaseManager().addMoney(player.getUniqueId().toString(), amount)) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "DB 오류로 지급 실패");
    }

    // --- 에러 방지를 위한 인터페이스 구현 필수 메서드들 ---
    @Override public boolean hasAccount(OfflinePlayer player) { return true; }
    @Override public boolean hasAccount(String playerName) { return true; }
    @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return true; }
    @Override public boolean hasAccount(String playerName, String worldName) { return true; }
    @Override public double getBalance(String playerName) { return 0; }
    @Override public double getBalance(String playerName, String world) { return 0; }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
    @Override public boolean has(String playerName, double amount) { return false; }
    @Override public boolean has(String playerName, String worldName, double amount) { return false; }
    @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return null; }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return null; }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return null; }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return null; }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }
    @Override public EconomyResponse createBank(String name, String player) { return null; }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return null; }
    @Override public EconomyResponse deleteBank(String name) { return null; }
    @Override public EconomyResponse bankBalance(String name) { return null; }
    @Override public EconomyResponse bankHas(String name, double amount) { return null; }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return null; }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return null; }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return null; }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return null; }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return null; }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return null; }
    @Override public List<String> getBanks() { return Collections.emptyList(); }
    @Override public boolean createPlayerAccount(String playerName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return true; }
}