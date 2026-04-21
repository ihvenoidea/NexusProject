package main_plugin.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64; // [핵심] 삭제된 라이브러리 대신 표준 자바 라이브러리 사용

public class ItemSerializer {

    // 아이템 배열(창고 내용물)을 텍스트로 변환
    public static String toBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            // [수정됨] 최신 인코딩 방식 적용
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("아이템을 저장할 수 없습니다.", e);
        }
    }

    // 텍스트를 다시 아이템 배열로 복구
    public static ItemStack[] fromBase64(String data) {
        try {
            // [수정됨] 최신 디코딩 방식 적용
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("아이템을 불러올 수 없습니다.", e);
        }
    }
}