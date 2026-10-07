package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.item.ScrewdriverItem;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryMenus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Jet Engine: 1024 N*m at 65536 rad/s (67 MW) on jet fuel, 10 mB every 2 ticks (25 with the afterburner, which doubles
 * the torque). The intake is the back; the exhaust and shaft come out of the front. Faithful to the original:
 * <ul>
 * <li>it pulls entities in from a widening cone in front of the intake; items and XP are blown out of the exhaust (armour
 * and tools are damaged, stone, cobble and gravel are crushed and damage the engine), creatures die and most of them
 * damage it ("FOD"), as does a screwdriver;</li>
 * <li>each point of FOD halves the torque; at 8 it stops. A damaged engine can start failing: it backfires, burns what is
 * behind it and heats up until it explodes past 1000 C;</li>
 * <li>a turbine repairs it fully, a compressor takes off one point;</li>
 * <li>a block over the intake chokes it (fences, walls, panes let some air through);</li>
 * <li>the exhaust heats machines behind it (up to 1200 C, 1750 with the afterburner) and burns creatures;</li>
 * <li>fuel near the intake can set it off; lava damages it.</li>
 * </ul>
 * Players are only pulled in when the server allows it ({@code jetHarmsPlayers}); creative players never are.
 */
public class JetEngineBlockEntity extends FuelEngineBlockEntity {
    public static final int TORQUE = 1024;
    public static final int SPEED = 65536;
    public static final int BASE_CONSUMPTION = 10;
    public static final int AFTERBURNER_CONSUMPTION = 25;
    public static final int MAX_FOD = 8;
    public static final ResourceKey<DamageType> INGESTED = ResourceKey.create(Registries.DAMAGE_TYPE, RotaryCraft.id("jet_ingest"));
    /** The original's medium-difficulty failure odds: 1 in 1800 x (9 - FOD) each tick with FOD. */
    private static final int JET_FAILURE = 1800;
    /** The original's medium-difficulty chance that swallowing a creature starts a failure. */
    private static final float INGEST_FAIL = 0.2F;

    private int fod;
    private boolean failing;
    private boolean canAfterburn;
    private boolean burnerActive;
    private int temperature = Integer.MIN_VALUE;
    private int ticks;

    public JetEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.JET_ENGINE.get(), pos, state);
    }

    public int fod() {
        return fod;
    }

    public void setFod(int f) {
        fod = Math.max(0, Math.min(MAX_FOD, f));
        setChanged();
    }

    public boolean isFailing() {
        return failing;
    }

    public boolean canAfterburn() {
        return canAfterburn;
    }

    public void installAfterburner() {
        canAfterburn = true;
        setChanged();
    }

    public boolean burnerActive() {
        return burnerActive;
    }

    public void setBurnerActive(boolean on) {
        burnerActive = on;
        setChanged();
    }

    public boolean isAfterburning() {
        return canAfterburn && burnerActive;
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    private Direction intake() {
        return facing().getOpposite();
    }

    @Override
    protected int ratedTorque() {
        int t = isAfterburning() ? TORQUE * 2 : TORQUE;
        return t >> fod;
    }

    @Override
    protected int targetSpeed() {
        return (int) (SPEED * chokedFraction());
    }

    @Override
    protected TagKey<Fluid> fuelTag() {
        return MicroturbineBlockEntity.JET_FUEL;
    }

    @Override
    protected Fluid fuelFluid() {
        return RotaryFluids.JET_FUEL.get();
    }

    @Override
    protected Item fuelItem() {
        return Items.AIR;
    }

    @Override
    protected int slotCount() {
        return 0;
    }

    @Override
    protected int fuelUnitTicks() {
        return 2;
    }

    @Override
    protected int fuelPerUnit() {
        return isAfterburning() ? AFTERBURNER_CONSUMPTION : BASE_CONSUMPTION;
    }

    @Override
    protected boolean canRun() {
        return fod < MAX_FOD && super.canRun();
    }

    @Override
    protected int temperatureForDisplay() {
        return temperature();
    }

    /** How much air gets past the block over the intake (the original's rules). */
    public float chokedFraction() {
        if (level == null) {
            return 1;
        }
        BlockPos p = worldPosition.relative(intake());
        BlockState s = level.getBlockState(p);
        VoxelShape shape = s.getCollisionShape(level, p);
        if (s.isAir() || shape.isEmpty() || s.is(Blocks.IRON_BARS)) {
            return 1;
        }
        if (s.is(BlockTags.FENCES)) {
            return 0.75F;
        }
        if (s.is(BlockTags.WALLS)) {
            return 0.25F;
        }
        if (s.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock) {
            return 0.5F; // glass panes
        }
        AABB b = shape.bounds();
        if (b.maxX > 0.875 && b.maxY > 0.875 && b.maxZ > 0.875 && b.minX < 0.125 && b.minY < 0.125 && b.minZ < 0.125) {
            return 0;
        }
        double dx = b.maxX <= 0.125 || b.minX >= 0.875 ? 0 : b.getXsize();
        double dy = b.maxY <= 0.125 || b.minY >= 0.875 ? 0 : b.getYsize();
        double dz = b.maxZ <= 0.125 || b.minZ >= 0.875 ? 0 : b.getZsize();
        if (b.maxY >= 0.75) {
            dy += 0.125;
        }
        return (float) Math.max(0, 1 - dx * dy * dz);
    }

    @Override
    protected void afterTick(boolean running) {
        super.afterTick(running);
        ticks++;
        if (omega <= 0) {
            if (temperature() > Ambient.temperature(level, worldPosition) && ticks % 20 == 0) {
                int t = temperature();
                temperature = t - Math.max(1, (t - Ambient.temperature(level, worldPosition)) / 256);
            }
            return;
        }
        checkFailure();
        if (level.getBlockEntity(worldPosition) != this) {
            return; // exploded
        }
        ingest();
        fluidIngest();
        heatJet(running);
        if (isAfterburning() && ticks % 200 == 0) {
            temperature = temperature() + 1;
            if (temperature > 2000) {
                fail();
                return;
            }
            if (temperature >= 600) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1);
            }
        }
        setChanged();
    }

    // failure ---------------------------------------------------------------------------------------------------------

    private void checkFailure() {
        if (failing) {
            detonation();
        } else if (fod > 0 && level.random.nextInt(JET_FAILURE * (9 - fod)) == 0) {
            triggerFailing();
        }
    }

    private void triggerFailing() {
        failing = true;
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2, 0.75F);
        Direction ex = facing();
        AABB front = new AABB(worldPosition).expandTowards(ex.getStepX() * 8, 0, ex.getStepZ() * 8).inflate(3);
        Set<Entity> hit = new HashSet<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, front)) {
            e.hurt(level.damageSources().generic(), 8);
            hit.add(e);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(4))) {
            if (!hit.contains(e)) {
                e.hurt(level.damageSources().generic(), 4);
            }
        }
    }

    private void detonation() {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, flameZone())) {
            e.igniteForSeconds(2);
        }
        level.playSound(null, worldPosition, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 1, 1);
        int fuelLeft = fuel.getFluidAmount();
        if ((fuelLeft < CAPACITY / 12 && level.random.nextInt(10) == 0) || (fuelLeft < CAPACITY / 4 && level.random.nextInt(20) == 0)
                || level.random.nextInt(40) == 0) {
            backfire();
        }
        if (level.random.nextBoolean()) {
            temperature = temperature() + 1;
        }
        if (temperature() > 1000) {
            fail();
        }
    }

    private void backfire() {
        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 2 * level.random.nextFloat(), false,
                blockInteraction());
    }

    private void fail() {
        Level lvl = level;
        BlockPos pos = worldPosition;
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12F, true, blockInteraction());
        for (int m = 0; m < 6; m++) {
            lvl.explode(null, pos.getX() - 4 + lvl.random.nextInt(11), pos.getY() - 4 + lvl.random.nextInt(11), pos.getZ() - 4 + lvl.random.nextInt(11),
                    4F + lvl.random.nextFloat() * 2, true, blockInteraction());
        }
    }

    private static Level.ExplosionInteraction blockInteraction() {
        return RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
    }

    private void damageEngine() {
        fod = Math.min(MAX_FOD, fod + 1);
        if (level.random.nextFloat() < INGEST_FAIL) {
            failing = true;
            temperature = Math.max(temperature(), 800);
        }
    }

    /** Turbine: back to new. */
    public boolean repairFully() {
        if (fod <= 0) {
            return false;
        }
        fod = 0;
        failing = false;
        temperature = Math.max(Ambient.temperature(level, worldPosition), temperature() / 2);
        setChanged();
        return true;
    }

    /** Compressor: one point of FOD. */
    public boolean repairPartly() {
        if (fod <= 0) {
            return false;
        }
        fod--;
        setChanged();
        return true;
    }

    // suction ---------------------------------------------------------------------------------------------------------

    private AABB suctionZone(int step) {
        Direction in = intake();
        BlockPos c = worldPosition.relative(in, 1 + step);
        AABB box = new AABB(c).inflate(in.getAxis() == Direction.Axis.X ? 0 : step, step, in.getAxis() == Direction.Axis.Z ? 0 : step);
        return box.inflate(0.25);
    }

    private AABB flameZone() {
        Direction ex = facing();
        return new AABB(worldPosition).expandTowards(ex.getStepX() * 6, 0, ex.getStepZ() * 6);
    }

    private Vec3 intakePoint() {
        Direction in = intake();
        return Vec3.atCenterOf(worldPosition).add(in.getStepX() * 0.51, 0, in.getStepZ() * 0.51);
    }

    /** Line of sight from just outside the intake (a ray from inside the engine would stop on the engine itself). */
    private boolean canSuck(Entity e, Vec3 intakePoint) {
        Direction in = intake();
        Vec3 from = intakePoint.add(in.getStepX() * 0.02, 0, in.getStepZ() * 0.02);
        for (int i = 0; i <= 2; i++) {
            Vec3 to = new Vec3(e.getX(), e.getY() + e.getBbHeight() * i / 2, e.getZ());
            HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
            if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.01) {
                return true;
            }
        }
        return false;
    }

    /** 1 normally; 0 for things the jet can't pull (creative players, the dragon, and players when the server forbids it). */
    private static float suction(Entity e) {
        if (e instanceof Player p) {
            if (p.isCreative() || p.isSpectator() || !RotaryConfig.get(RotaryConfig.JET_HARMS_PLAYERS)) {
                return 0;
            }
        }
        if (e instanceof EnderDragon) {
            return 0;
        }
        return 1;
    }

    private void ingest() {
        if (fod >= MAX_FOD) {
            return;
        }
        Vec3 p = intakePoint();
        Set<Entity> seen = new HashSet<>();
        for (int step = 0; step < 8; step++) {
            List<Entity> caught = level.getEntitiesOfClass(Entity.class, suctionZone(step), e -> !seen.contains(e));
            for (Entity e : caught) {
                seen.add(e);
                if (!canSuck(e, p)) {
                    continue;
                }
                float mult = suction(e);
                if (mult > 0) {
                    Vec3 pull = Vec3.atCenterOf(worldPosition).subtract(e.position()).scale(mult / 20D);
                    e.setDeltaMovement(e.getDeltaMovement().add(pull));
                    e.hurtMarked = true;
                }
                if (e.position().distanceTo(new Vec3(p.x, worldPosition.getY() + 0.5, p.z)) < 1.2) {
                    swallow(e, mult <= 0);
                }
            }
        }
    }

    private void swallow(Entity e, boolean immune) {
        Direction ex = facing();
        Vec3 out = Vec3.atCenterOf(worldPosition.relative(ex)).add(0, -0.125, 0);
        if (e instanceof ItemEntity item) {
            ItemStack stack = item.getItem().copy();
            item.discard();
            ItemStack after = modifyIngested(stack);
            if (stack.getItem() instanceof ScrewdriverItem) {
                fod = Math.max(fod, 2);
                triggerFailing();
            }
            if (!after.isEmpty()) {
                ItemEntity blown = new ItemEntity(level, out.x, out.y, out.z, after);
                blown.setDeltaMovement(ex.getStepX() * 1.5, 0.15, ex.getStepZ() * 1.5);
                level.addFreshEntity(blown);
            }
        } else if (e instanceof ExperienceOrb orb) {
            int xp = orb.getValue();
            orb.discard();
            ExperienceOrb blown = new ExperienceOrb(level, out.x, out.y, out.z, xp);
            blown.setDeltaMovement(ex.getStepX() * 1.5, 0.15, ex.getStepZ() * 1.5);
            level.addFreshEntity(blown);
        } else if (e instanceof LivingEntity living && !(e instanceof Player && immune)) {
            living.igniteForSeconds(2);
            if (living.isAlive() && canDamageEngine(living)) {
                damageEngine();
            }
            level.playSound(null, worldPosition, net.scwunge.rotarycraft.registry.MachineSoundRegistry.get("ingest_short").get(), SoundSource.BLOCKS, 1, 1.4F);
            living.hurt(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(INGESTED)), 10000);
        }
    }

    private ItemStack modifyIngested(ItemStack is) {
        if (is.isDamageableItem() && (is.getItem() instanceof ArmorItem || is.getItem() instanceof net.minecraft.world.item.TieredItem)) {
            int add = level.random.nextInt(is.getItem() instanceof ArmorItem ? 200 : 40);
            if (is.getDamageValue() + add >= is.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
            is.setDamageValue(is.getDamageValue() + add);
        }
        // hard things chip the blades and come out crushed
        Item crushed = is.is(Items.STONE) ? Items.COBBLESTONE : is.is(Items.COBBLESTONE) ? Items.GRAVEL : is.is(Items.GRAVEL) ? Items.SAND : null;
        if (crushed == null) {
            return is;
        }
        fod = Math.min(MAX_FOD, fod + (is.is(Items.STONE) ? 4 : is.is(Items.COBBLESTONE) ? 2 : 1));
        return is.transmuteCopy(crushed, is.getCount());
    }

    private static boolean canDamageEngine(LivingEntity e) {
        if (e.noPhysics || e instanceof Chicken || e instanceof Bat || e instanceof Silverfish) {
            return false;
        }
        String name = e.getName().getString().toLowerCase(java.util.Locale.ROOT);
        return !name.contains("bird") && !name.contains("firefly") && !name.contains("butterfly");
    }

    private void fluidIngest() {
        BlockPos p = worldPosition.relative(intake());
        var fs = level.getFluidState(p);
        if (fs.isEmpty()) {
            return;
        }
        String name = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fs.getType()).getPath();
        if (name.contains("fuel")) {
            if (!failing && level.random.nextInt(200) == 0) {
                temperature = 900;
                failing = true;
                detonation();
            }
            return;
        }
        int t = fs.getType().getFluidType().getTemperature();
        if (t >= 500 && (t >= 2000 || level.random.nextInt(1 + (2000 - t) / 500) == 0)
                && fod < MAX_FOD && ticks % 20 == 0 && level.random.nextInt(1 + 2 * fod) == 0) {
            damageEngine();
        }
    }

    // exhaust heat ----------------------------------------------------------------------------------------------------

    public int maxExhaustTemperature() {
        return isAfterburning() ? 1750 : 1200;
    }

    private void heatJet(boolean running) {
        if (running && ticks % 10 == 0) {
            int max = (int) ((long) maxExhaustTemperature() * omega / SPEED);
            int t = temperature();
            if (max > t) {
                temperature = Math.min(t + Math.max(1, (max - t) / 16), max);
            } else if (!isAfterburning()) {
                temperature = Math.max(t - Math.max(1, (t - max) / 32), max);
            }
        }
        int t = temperature();
        Direction ex = facing();
        int reach = isAfterburning() ? 6 : 4;
        for (int i = 1; i < reach; i++) {
            if (level.getBlockEntity(worldPosition.relative(ex, i)) instanceof Heatable h) {
                h.addTemperature(t - h.getTemperature());
            }
        }
        AABB box = new AABB(worldPosition).expandTowards(ex.getStepX() * 4, 0, ex.getStepZ() * 4);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            e.hurt(level.damageSources().onFire(), isAfterburning() ? 4 : 1);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.jet_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelEngineMenu(RotaryMenus.JET_ENGINE.get(), id, inventory, this, data);
    }

    /** The turbine screen's status word (the slot other fuel engines use for additives): afterburner fitted, burner on, FOD. */
    @Override
    protected int additives() {
        return (canAfterburn ? 1 : 0) | (burnerActive ? 2 : 0) | (fod << 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("fod", fod);
        tag.putBoolean("failing", failing);
        tag.putBoolean("afterburner", canAfterburn);
        tag.putBoolean("burner", burnerActive);
        tag.putInt("temperature", temperature());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fod = tag.getInt("fod");
        failing = tag.getBoolean("failing");
        canAfterburn = tag.getBoolean("afterburner");
        burnerActive = tag.getBoolean("burner");
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
    }

    @Override
    protected int statusKey() {
        return canAfterburn ? 1 : 0;
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putBoolean("afterburner", canAfterburn);
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        canAfterburn = tag.getBoolean("afterburner");
    }

    @Override
    protected boolean isTurbine() {
        return true;
    }
}
