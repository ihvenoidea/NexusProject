package main_plugin.items;

import main_plugin.NexusCore;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WeaponToolListener implements Listener {

    private final NexusCore plugin;
    private final NamespacedKey nameKey;
    private final Map<Material, Material> smeltMap = new HashMap<>();
    private final Random random = new Random();
    
    // 환영 도구 텔레포트 쿨타임 관리용
    private final Map<UUID, Long> teleportCooldown = new HashMap<>();

    public WeaponToolListener(NexusCore plugin) {
        this.plugin = plugin;
        this.nameKey = plugin.getSetItemManager().getNameKey();

        // 전천후 자동 정제 레시피 등록
        smeltMap.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.RAW_IRON, Material.IRON_INGOT);
        smeltMap.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        smeltMap.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        smeltMap.put(Material.OAK_LOG, Material.CHARCOAL);
        smeltMap.put(Material.SPRUCE_LOG, Material.CHARCOAL);
        smeltMap.put(Material.BIRCH_LOG, Material.CHARCOAL);
        smeltMap.put(Material.JUNGLE_LOG, Material.CHARCOAL);
        smeltMap.put(Material.ACACIA_LOG, Material.CHARCOAL);
        smeltMap.put(Material.DARK_OAK_LOG, Material.CHARCOAL);
        smeltMap.put(Material.MANGROVE_LOG, Material.CHARCOAL);
        smeltMap.put(Material.CHERRY_LOG, Material.CHARCOAL);
        smeltMap.put(Material.SAND, Material.GLASS);
        smeltMap.put(Material.RED_SAND, Material.GLASS);
        smeltMap.put(Material.COBBLESTONE, Material.STONE);
        smeltMap.put(Material.CLAY, Material.TERRACOTTA);

        startPassiveTasks();
    }

    // 아이템을 들고만 있어도 발동하는 효과 처리 (1초 주기)
    private void startPassiveTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (!mainHand.hasItemMeta()) continue;
                    
                    String setName = mainHand.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                    if (setName == null) continue;

                    // [화염 도구] 지옥(Nether)에서 이 도구를 들고 있으면 상시 성급함 II 부여
                    if (setName.equals("화염") && !mainHand.getType().toString().contains("SWORD") && !mainHand.getType().toString().contains("BOW")) {
                        if (player.getWorld().getName().endsWith("_nether")) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onWeaponHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta()) return;

        String setName = weapon.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        if (weapon.getType().toString().contains("SWORD")) {
            switch (setName) {
                case "도약": // 공중 낙하 타격(크리티컬) 시 데미지 20% 추가 증가
                    if (!player.isOnGround() && player.getVelocity().getY() < 0) {
                        event.setDamage(event.getDamage() * 1.2);
                        // [수정됨] 마인크래프트 1.21 대응: Particle.CRIT_MAGIC -> Particle.ENCHANTED_HIT
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 15);
                    }
                    break;
                case "탐욕": // 타격 시 2% 확률로 랜덤 광물 드롭
                    if (random.nextDouble() < 0.02) {
                        Material[] ores = {Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.DIAMOND, Material.EMERALD};
                        target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(ores[random.nextInt(ores.length)]));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
                    }
                    break;
                case "화염": // 광역 발화 및 이미 불타는 적에게 1.5배 폭딜
                    if (target.getFireTicks() > 0) {
                        event.setDamage(event.getDamage() * 1.5);
                        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.5f);
                    }
                    for (org.bukkit.entity.Entity ent : target.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            ent.setFireTicks(100);
                        }
                    }
                    target.setFireTicks(100);
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    break;
                case "신속": // 타격 시 적 2초 둔화 부여
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 10);
                    break;
                case "혹한": // 타격 시 5% 확률로 적을 2초간 완전 빙결
                    if (random.nextDouble() < 0.05) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5));
                        target.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    }
                    break;
                case "환영": // 적의 배후(뒤)에서 공격 시 데미지 1.5배 증폭
                    Vector pDir = player.getLocation().getDirection().setY(0).normalize();
                    Vector tDir = target.getLocation().getDirection().setY(0).normalize();
                    if (pDir.dot(tDir) > 0.5) { 
                        event.setDamage(event.getDamage() * 1.5);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
                    }
                    break;
                case "권능": // 흡혈 효과 (가한 최종 피해량의 4%만큼 체력 회복)
                    double heal = event.getFinalDamage() * 0.04;
                    double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHp, player.getHealth() + heal));
                    target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5);
                    break;
                case "재앙": // 타격 시 전방 3x3 범위 내 적들에게 휩쓸기 데미지
                    for (org.bukkit.entity.Entity ent : target.getNearbyEntities(1.5, 1.0, 1.5)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            ((LivingEntity) ent).damage(event.getDamage() * 0.5, player);
                        }
                    }
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3);
                    break;
                case "불멸": // 자신의 체력이 30% 이하일 때 가하는 데미지 2배 증가
                    double pMaxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    if (player.getHealth() <= pMaxHp * 0.3) {
                        event.setDamage(event.getDamage() * 2.0);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    }
                    break;
            }
        } else if (setName.equals("혹한")) { 
            // 혹한 도구류: 적 타격 시 3초간 강력한 빙결(둔화) 부여
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        String setName = bow.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        event.getProjectile().getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, setName);

        if (setName.equals("견고")) {
            event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(1.5));
        } 
        else if (setName.equals("도약")) { // 화살 발사 시 반동으로 뒤로 밀려남
            player.setVelocity(player.getLocation().getDirection().multiply(-1.0));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        }
        else if (setName.equals("신속")) { // 적을 추적하는 유도 화살 발사
            startHomingArrowTask((Arrow) event.getProjectile());
        }
        else if (setName.equals("불멸")) { // 자신의 잃은 체력에 비례하여 화살 공격력 증가
            double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double missingHp = maxHp - player.getHealth();
            if (missingHp > 0 && event.getProjectile() instanceof AbstractArrow arrow) {
                arrow.setDamage(arrow.getDamage() + (missingHp * 0.2)); 
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        
        String setName = arrow.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        Location hitLoc = arrow.getLocation();
        LivingEntity target = event.getHitEntity() instanceof LivingEntity ? (LivingEntity) event.getHitEntity() : null;

        switch (setName) {
            case "재생": // 화살 명중 시 배고픔 1칸 회복
                if (arrow.getShooter() instanceof Player p && target != null) {
                    p.setFoodLevel(Math.min(20, p.getFoodLevel() + 2));
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.5f, 1.0f);
                }
                break;
            case "풍요": // 화살 적중 시 10초간 발광 효과
                if (target != null) target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
                break;
            case "화염": // 화살 착탄 시 지형을 파괴하지 않는 폭발 및 주변 발화
                arrow.getWorld().createExplosion(hitLoc, 2.0F, true, false); 
                arrow.remove();
                break;
            case "혹한": // 착탄 지점 주변 3x3 범위 둔화 장판 생성
                for (org.bukkit.entity.Entity ent : hitLoc.getWorld().getNearbyEntities(hitLoc, 1.5, 1.5, 1.5)) {
                    if (ent instanceof LivingEntity && !ent.equals(arrow.getShooter())) {
                        ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    }
                }
                arrow.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 50, 1, 1, 1, 0.1);
                break;
            case "환영": // 화살이 적중한 위치로 즉시 순간이동
                if (arrow.getShooter() instanceof Player p) {
                    Location safeLoc = hitLoc.clone();
                    safeLoc.setYaw(p.getLocation().getYaw());
                    safeLoc.setPitch(p.getLocation().getPitch());
                    p.teleport(safeLoc);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
                break;
            case "권능": // 화살 착탄 시 소형 폭발 발생
                arrow.getWorld().createExplosion(hitLoc, 2.0F, false, false);
                arrow.remove();
                break;
            case "재앙": // 화살 착탄 지점에 벼락 소환
                arrow.getWorld().strikeLightningEffect(hitLoc);
                if (target != null && arrow.getShooter() instanceof Player p) target.damage(8.0, p);
                arrow.remove();
                break;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!tool.hasItemMeta()) return;

        String setName = tool.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;
        if (player.hasMetadata("is_mining_skill")) return;

        Block block = event.getBlock();
        String type = tool.getType().toString();
        boolean isTool = type.contains("PICKAXE") || type.contains("AXE") || type.contains("SHOVEL");

        if (!isTool) return;

        switch (setName) {
            case "도약": // 블록 파괴 시 3초간 이동 속도 증가
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
                break;
            case "재생": // 블록 파괴 시 1% 확률로 체력 1(반 칸) 회복
                if (random.nextDouble() < 0.01) {
                    double maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHp, player.getHealth() + 1.0));
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1);
                }
                break;
            case "풍요": // 전천후 자동 정제 (원목->목탄, 광물->주괴 등)
                if (smeltMap.containsKey(block.getType())) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smeltMap.get(block.getType())));
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);
                    player.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                }
                break;
            case "탐욕": // 광물 파괴 시 획득 경험치 2배 증가
                if (block.getType().toString().contains("ORE")) {
                    event.setExpToDrop(event.getExpToDrop() * 2);
                }
                break;
            case "화염": // 네더(지옥)에서 네더 블록 채굴 시 5% 확률로 보너스 광물 획득
                if (player.getWorld().getName().endsWith("_nether") && block.getType().toString().contains("NETHER")) {
                    if (random.nextDouble() < 0.05) {
                        Material drop = random.nextBoolean() ? Material.QUARTZ : Material.GOLD_NUGGET;
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(drop));
                    }
                }
                break;
            case "신속": // 채광/벌목 시 상시 성급함 I 부여
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1));
                break;
            case "권능": // 3x3 범위 채광/벌목 기술 발동
                player.setMetadata("is_mining_skill", new FixedMetadataValue(plugin, true));
                Location center = block.getLocation();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block targetBlock = center.clone().add(x, y, z).getBlock();
                            if (targetBlock.getType().getHardness() >= 0 && targetBlock.getType() != Material.AIR) {
                                targetBlock.breakNaturally(tool);
                            }
                        }
                    }
                }
                player.removeMetadata("is_mining_skill", plugin);
                break;
            case "재앙": // 블록 파괴 시 10% 확률로 해당 위치에 벼락 소환 및 주변 적 데미지
                if (random.nextDouble() < 0.1) {
                    block.getWorld().strikeLightningEffect(block.getLocation());
                    for (org.bukkit.entity.Entity ent : block.getWorld().getNearbyEntities(block.getLocation(), 2, 2, 2)) {
                        if (ent instanceof LivingEntity && !ent.equals(player)) {
                            ((LivingEntity) ent).damage(5.0, player);
                        }
                    }
                }
                break;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon.hasItemMeta()) {
                String setName = weapon.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if (setName == null) return;
                
                if (setName.equals("재생") && weapon.getType().toString().contains("SWORD")) {
                    killer.setFoodLevel(Math.min(20, killer.getFoodLevel() + 2)); 
                } else if (setName.equals("풍요") && weapon.getType().toString().contains("SWORD")) {
                    event.setDroppedExp((int) (event.getDroppedExp() * 1.5));
                }
            }
        }
        
        // 탐욕 세트 활로 처치 시 경험치 대량 드롭 (3배)
        if (entity.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent dmgEvent) {
            if (dmgEvent.getDamager() instanceof Arrow arrow) {
                String arrowSet = arrow.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
                if ("탐욕".equals(arrowSet)) {
                    event.setDroppedExp(event.getDroppedExp() * 3);
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;
        
        String setName = item.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (setName == null) return;

        if (setName.equals("불멸")) {
            event.setCancelled(true); // 불멸 도구: 내구도가 절대 닳지 않음
        } else if (setName.equals("견고")) {
            if (random.nextDouble() < 0.15) {
                event.setCancelled(true); // 15% 확률로 내구도 소모 방지
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!tool.hasItemMeta()) return;

        String setName = tool.getItemMeta().getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        
        // [환영 도구] Shift + 우클릭 시 바라보는 방향으로 5칸 순간이동 (쿨타임 10초)
        if ("환영".equals(setName)) {
            String type = tool.getType().toString();
            if (type.contains("PICKAXE") || type.contains("AXE") || type.contains("SHOVEL")) {
                
                long lastUse = teleportCooldown.getOrDefault(player.getUniqueId(), 0L);
                if (System.currentTimeMillis() - lastUse < 10000) { 
                    player.sendMessage("§c[!] 아직 순간이동을 사용할 수 없습니다.");
                    return;
                }

                RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), 5.0, FluidCollisionMode.NEVER, true);
                Location targetLoc;
                
                if (ray != null && ray.getHitBlock() != null) {
                    targetLoc = ray.getHitPosition().toLocation(player.getWorld());
                    targetLoc.subtract(player.getLocation().getDirection().multiply(0.5)); 
                } else {
                    targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5.0));
                }

                targetLoc.setYaw(player.getLocation().getYaw());
                targetLoc.setPitch(player.getLocation().getPitch());

                player.teleport(targetLoc);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                
                teleportCooldown.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    private void startHomingArrowTask(Arrow arrow) {
        new BukkitRunnable() {
            int tickAlive = 0;
            @Override
            public void run() {
                tickAlive++;
                if (tickAlive > 200 || arrow.isDead() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }
                
                LivingEntity closest = null;
                double closestDist = 100.0;
                for (LivingEntity ent : arrow.getWorld().getLivingEntities()) {
                    if (ent == arrow.getShooter() || ent.isDead()) continue;
                    
                    double dist = ent.getLocation().distanceSquared(arrow.getLocation());
                    if (dist < 100 && dist < closestDist) {
                        closestDist = dist;
                        closest = ent;
                    }
                }

                if (closest != null) {
                    Vector direction = closest.getLocation().add(0, closest.getHeight() / 2, 0).toVector().subtract(arrow.getLocation().toVector()).normalize();
                    arrow.setVelocity(arrow.getVelocity().add(direction.multiply(0.2)).normalize().multiply(arrow.getVelocity().length()));
                    arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}