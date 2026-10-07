package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Firework Machine (TileEntityFireworkMachine): fed paper, gunpowder, dyes and the extras of fireworks, it makes stars (a dye and gunpowder, with diamond
 * for a trail, glowstone for a twinkle, and a fire charge, gold nugget, feather or head for a shape, and sometimes a second dye to fade to) and rockets
 * from them, and launches them one after another, so long as it has the turning speed. Ready-made stars are used before new ones are made. It uses up
 * its ingredients only some of the time, less often the more power it has over what it needs.
 */
public class FireworkMachineBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "firework_machine";
    public static final int SLOTS = 27;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 4096, 65536);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).storage(3).build();

    private int tickCount;
    private boolean idle;

    public FireworkMachineBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.FIREWORK_MACHINE.type().get(), pos, state, SLOTS, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean omniSided() {
        return true;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    /** Whether it had nothing to make its last turn. */
    public boolean isIdle() {
        return idle;
    }

    /** The ticks between rockets: 300, less 16 for each doubling of the speed, never under one. */
    public int operationTime() {
        return PowerRequirement.operationTime(300, 16, omega);
    }

    // ---- looking in the slots ----

    private int count(Item item) {
        int total = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean has(Item item) {
        return count(item) > 0;
    }

    private boolean hasDye() {
        for (int i = 0; i < SLOTS; i++) {
            if (items.getStackInSlot(i).getItem() instanceof DyeItem) {
                return true;
            }
        }
        return false;
    }

    /** Takes one of the item, if it is there (and if {@code consume}); true if it was there. */
    private boolean take(Item item, boolean consume) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.getStackInSlot(i).is(item)) {
                if (consume) {
                    items.extractItem(i, 1, false);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isHead(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock;
    }

    private boolean hasHead() {
        for (int i = 0; i < SLOTS; i++) {
            if (isHead(items.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    private void takeHead() {
        for (int i = 0; i < SLOTS; i++) {
            if (isHead(items.getStackInSlot(i))) {
                items.extractItem(i, 1, false);
                return;
            }
        }
    }

    /** Whether an ingredient is used up this time: always, but with power well over what it needs, less and less often. */
    private boolean consumeChance() {
        int excess = (int) (getPower() / REQUIREMENT.minPower());
        return level.random.nextInt(1 + excess / 8) == 0;
    }

    public boolean canCraftARocket() {
        boolean paper = has(Items.PAPER);
        boolean star = has(Items.FIREWORK_STAR);
        return (paper && hasDye() && count(Items.GUNPOWDER) >= 2) || (has(Items.GUNPOWDER) && star);
    }

    /** A random dye from those in the slots, taking one of it if {@code consume}; null if there are none. */
    private DyeItem pickDye(boolean consume) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (items.getStackInSlot(i).getItem() instanceof DyeItem) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return null;
        }
        int slot = slots.get(level.random.nextInt(slots.size()));
        DyeItem dye = (DyeItem) items.getStackInSlot(slot).getItem();
        if (consume) {
            items.extractItem(slot, 1, false);
        }
        return dye;
    }

    private static ItemStack star(FireworkExplosion.Shape shape, IntArrayList colors, IntArrayList fade, boolean trail, boolean twinkle) {
        ItemStack star = new ItemStack(Items.FIREWORK_STAR);
        star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(shape, colors, fade, trail, twinkle));
        return star;
    }

    /** A new star from the ingredients: a dye and gunpowder, and by chance diamond, glowstone, a shape item and a second dye to fade to. Null without the dye or gunpowder. */
    private ItemStack randomStar() {
        DyeItem dye = pickDye(consumeChance() && has(Items.GUNPOWDER));
        if (dye == null || !take(Items.GUNPOWDER, consumeChance())) {
            return null;
        }
        boolean trail = level.random.nextBoolean() && take(Items.DIAMOND, consumeChance());
        boolean twinkle = level.random.nextBoolean() && take(Items.GLOWSTONE_DUST, consumeChance());
        List<FireworkExplosion.Shape> shapes = new ArrayList<>();
        if (has(Items.FIRE_CHARGE)) {
            shapes.add(FireworkExplosion.Shape.LARGE_BALL);
        }
        if (has(Items.GOLD_NUGGET)) {
            shapes.add(FireworkExplosion.Shape.STAR);
        }
        if (has(Items.FEATHER)) {
            shapes.add(FireworkExplosion.Shape.BURST);
        }
        if (hasHead()) {
            shapes.add(FireworkExplosion.Shape.CREEPER);
        }
        FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
        if (!shapes.isEmpty()) {
            shape = shapes.get(level.random.nextInt(shapes.size()));
            if (consumeChance()) {
                switch (shape) {
                    case LARGE_BALL -> take(Items.FIRE_CHARGE, true);
                    case STAR -> take(Items.GOLD_NUGGET, true);
                    case BURST -> take(Items.FEATHER, true);
                    default -> takeHead();
                }
            }
        }
        IntArrayList colors = new IntArrayList();
        colors.add(dye.getDyeColor().getFireworkColor());
        IntArrayList fade = new IntArrayList();
        if (level.random.nextBoolean()) {
            DyeItem second = pickDye(consumeChance());
            if (second != null) {
                fade.add(second.getDyeColor().getFireworkColor());
            }
        }
        return star(shape, colors, fade, trail, twinkle);
    }

    /** A rocket round one star: one to three gunpowder (how long it flies), and paper. */
    private ItemStack rocketFrom(ItemStack star) {
        int gunpowder = level.random.nextInt(3) + 1;
        int flight = 0;
        if (take(Items.GUNPOWDER, consumeChance())) {
            flight++;
        }
        for (int extra = 2; extra <= gunpowder; extra++) {
            if (take(Items.GUNPOWDER, consumeChance())) {
                flight++;
            }
        }
        take(Items.PAPER, consumeChance());
        if (flight == 0) {
            return null;
        }
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosion explosion = star.get(DataComponents.FIREWORK_EXPLOSION);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(flight, explosion == null ? List.of() : List.of(explosion)));
        return rocket;
    }

    /** One launch, if there is what it needs; false if it had nothing to make a rocket from. */
    public boolean makeRocket(ServerLevel server) {
        if (!canCraftARocket()) {
            idle = true;
            return false;
        }
        idle = false;
        ItemStack rocket;
        if (!has(Items.FIREWORK_STAR)) {
            ItemStack star = randomStar();
            if (star == null) {
                return false;
            }
            rocket = rocketFrom(star);
        } else if (has(Items.PAPER) && has(Items.GUNPOWDER)) {
            List<Integer> stars = new ArrayList<>();
            for (int i = 0; i < SLOTS; i++) {
                if (items.getStackInSlot(i).is(Items.FIREWORK_STAR)) {
                    stars.add(i);
                }
            }
            int slot = stars.get(level.random.nextInt(stars.size()));
            ItemStack star = items.getStackInSlot(slot).copyWithCount(1);
            if (consumeChance()) {
                items.extractItem(slot, 1, false);
            }
            rocket = rocketFrom(star);
        } else {
            return false;
        }
        if (rocket == null) {
            return false;
        }
        server.addFreshEntity(new FireworkRocketEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5, rocket));
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("fireworkMachine")) {
            return;
        }
        if (++tickCount < operationTime()) {
            return;
        }
        tickCount = 0;
        makeRocket(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticks", tickCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCount = tag.getInt("ticks");
    }
}
