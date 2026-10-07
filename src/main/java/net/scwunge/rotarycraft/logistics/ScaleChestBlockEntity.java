package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scale-able Chest (TileEntityScaleableChest): nine slots at 4 kW, and a slot more for every 128 W over that, up to 972, shown 54 to a page. It opens only while
 * powered. Switching the power on and off more than three times in a second makes it smoke and fizz, over eight spits sparks, and over ten it blows up (dropping
 * itself with its contents). Its contents stay in it when it is broken.
 */
public class ScaleChestBlockEntity extends InventoryMachineBlockEntity implements MachineInteractions {
    public static final String NAME = "scale_chest";
    public static final int MAX_SIZE = 972;
    public static final int ROWS = 6;
    public static final int PAGE_SIZE = 9 * ROWS;
    public static final int FALLOFF = 128;
    public static final int MIN_POWER = 4096;
    public static final int CHANGE_AGE = 20;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).build();

    private final List<Integer> changes = new ArrayList<>();
    private boolean lastPowered;
    private boolean seen;
    private int numChanges;
    private int users;

    public ScaleChestBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.SCALE_CHEST.type().get(), pos, state, MAX_SIZE, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public boolean isPowered() {
        return getPower() >= MIN_POWER;
    }

    /** The slots it has at this power. */
    public int numberSlots() {
        if (!isPowered()) {
            return 9;
        }
        return (int) Math.min(MAX_SIZE, 9 + (getPower() - MIN_POWER) / FALLOFF);
    }

    public int numberPages() {
        return Math.max(1, Math.min(MAX_SIZE / PAGE_SIZE, (numberSlots() + PAGE_SIZE - 1) / PAGE_SIZE));
    }

    public int powerChanges() {
        return numChanges;
    }

    public int users() {
        return users;
    }

    /** Whether it can be opened: powered, and not being switched on and off. */
    public boolean canOpen() {
        return numChanges == 0 && isPowered() && MachineConfig.enabled("scaleChest");
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return slot < numberSlots();
    }

    @Override
    protected boolean mayExtract(int slot) {
        return true;
    }

    @Override
    public boolean dropsInventory() {
        return false;
    }

    @Override
    public int comparatorSignal() {
        return 15 * numberSlots() / MAX_SIZE;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (!canOpen()) {
            player.displayClientMessage(Component.translatable("gui.rotarycraft.scale_chest.unpowered"), true);
            return null;
        }
        return new ScaleChestMenu(LogisticsRegistry.SCALE_CHEST_MENU.get(), id, inventory, items, this, worldPosition, ScaleChestMenu.dataFor(this));
    }

    /** A player opened or closed its screen. */
    public void opened() {
        if (users++ == 0 && level != null) {
            level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        markClientDirty();
    }

    public void closed() {
        users = Math.max(0, users - 1);
        if (users == 0 && level != null) {
            level.playSound(null, worldPosition, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        markClientDirty();
    }

    /** Counts the times the power came on or went off in the last second. */
    private void testInconsistentPower(ServerLevel server) {
        changes.replaceAll(age -> age + 1);
        changes.removeIf(age -> age > CHANGE_AGE);
        boolean powered = isPowered();
        if (seen && powered != lastPowered) {
            changes.add(0);
        }
        seen = true;
        lastPowered = powered;
        if (numChanges != changes.size()) {
            numChanges = changes.size();
            markClientDirty();
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        if (numChanges > 10) {
            // the blast first, so that it does not take the dropped chest with it
            boolean grief = server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            server.explode(null, x, y, z, 4F, grief ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            if (server.getBlockState(worldPosition).is(getBlockState().getBlock())) {
                server.destroyBlock(worldPosition, true);
            }
        } else if (numChanges > 8) {
            smoke(server, numChanges / 3);
            if (server.random.nextInt(19 - numChanges) == 0) {
                server.explode(null, x, y, z, 0F, Level.ExplosionInteraction.NONE);
            }
        } else if (numChanges > 3) {
            smoke(server, numChanges / 3);
            if (server.random.nextInt(11 - numChanges) == 0) {
                server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
            }
        }
    }

    private void smoke(ServerLevel server, int count) {
        for (int i = 0; i < count; i++) {
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + server.random.nextFloat(), worldPosition.getY() + server.random.nextFloat(),
                    worldPosition.getZ() + server.random.nextFloat(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (level instanceof ServerLevel server) {
            testInconsistentPower(server);
        }
    }

    // ---- what the client draws ----

    @Override
    protected void writeClient(CompoundTag tag) {
        super.writeClient(tag);
        tag.putInt("users", users);
        tag.putInt("changes", numChanges);
    }

    @Override
    protected void readClient(CompoundTag tag) {
        super.readClient(tag);
        users = tag.getInt("users");
        numChanges = tag.getInt("changes");
    }

    // ---- keeping the contents in the item ----

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        List<ChestContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                entries.add(new ChestContents.Entry(i, items.getStackInSlot(i).copy()));
            }
        }
        if (!entries.isEmpty()) {
            components.set(LogisticsRegistry.CHEST_CONTENTS.get(), new ChestContents(entries));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ChestContents held = input.get(LogisticsRegistry.CHEST_CONTENTS.get());
        if (held != null) {
            for (ChestContents.Entry entry : held.entries()) {
                if (entry.slot() >= 0 && entry.slot() < items.getSlots()) {
                    items.setStackInSlot(entry.slot(), entry.stack().copy());
                }
            }
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("items");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("lastPowered", lastPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastPowered = tag.getBoolean("lastPowered");
        users = 0;
    }
}
