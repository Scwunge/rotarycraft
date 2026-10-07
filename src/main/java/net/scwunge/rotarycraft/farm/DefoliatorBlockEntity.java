package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.weapon.WorldGuard;

/**
 * The Defoliator, as the original: with 16 kW (from any side) and poison in its tank (4000 mB; a potion of poison in the top slot fills 1000
 * and leaves a glass bottle in the bottom one) it makes 2 x sqrt(speed) tries a tick at random spots within 8 x log2(torque) blocks (at most 128),
 * and strips leaves, plants, vines, cactus, logs and saplings it finds, 1 mB for each, dropping what they drop. Creatures within 3 blocks
 * of a stripped block are poisoned. It breaks as its owner (claims and mobGriefing stop it), and is off by default in the farm config. There is
 * no poison fluid in this mod, so poison comes from potions only.
 */
public class DefoliatorBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 16384);
    public static final int CAPACITY = 4000;
    public static final int MAX_RANGE = 128;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && isPoison(stack);
        }
    };
    private int poison;

    public DefoliatorBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.DEFOLIATOR_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "defoliator";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public FarmUi ui() {
        return FarmUi.custom("defoliator", 80, 17, 80, 53);
    }

    @Override
    public Slot slot(ItemStackHandler handler, int index, int x, int y) {
        return index == 1 ? new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        } : super.slot(handler, index, x, y);
    }

    public static boolean isPoison(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && (contents.is(Potions.POISON) || contents.is(Potions.LONG_POISON) || contents.is(Potions.STRONG_POISON));
    }

    public int poison() {
        return poison;
    }

    public void addPoison(int amount) {
        poison = Math.min(CAPACITY, poison + amount);
        setChanged();
    }

    /** How far out it works. */
    public int range() {
        return Math.min((int) (8 * Math.log(Math.max(1, torque)) / Math.log(2)), MAX_RANGE);
    }

    public int passes() {
        return getPower() < REQUIREMENT.minPower() ? 0 : 2 * (int) Math.sqrt(omega);
    }

    private void drinkPotion() {
        ItemStack in = items.getStackInSlot(0);
        if (!in.isEmpty() && CAPACITY - poison >= 1000 && isPoison(in) && items.getStackInSlot(1).getCount() < 64
                && (items.getStackInSlot(1).isEmpty() || items.getStackInSlot(1).is(Items.GLASS_BOTTLE))) {
            poison += 1000;
            items.extractItem(0, 1, false);
            ItemStack out = items.getStackInSlot(1);
            items.setStackInSlot(1, out.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : out.copyWithCount(out.getCount() + 1));
            setChanged();
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        drinkPotion();
        int r = range();
        int n = passes();
        for (int i = 0; i < n && poison > 0 && r > 0; i++) {
            BlockPos at = worldPosition.offset(server.random.nextInt(2 * r + 1) - r, server.random.nextInt(2 * r + 1) - r, server.random.nextInt(2 * r + 1) - r);
            decay(server, at);
        }
        if (server.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    private static boolean isFoliage(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.VINE) || state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(BlockTags.CROPS) || state.is(Blocks.BAMBOO) || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    private void decay(ServerLevel server, BlockPos at) {
        if (!server.isLoaded(at)) {
            return;
        }
        BlockState state = server.getBlockState(at);
        if (state.isAir() || !isFoliage(state)) {
            return;
        }
        if (!WorldGuard.breakBlock(server, at, owner, true)) {
            return;
        }
        server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(3))) {
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 3));
            e.hurt(server.damageSources().generic(), 0.5F);
        }
        poison--;
        setChanged();
    }

    @Override
    protected int[] status() {
        return new int[] {poison, CAPACITY, range(), passes()};
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("poison", poison);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        poison = tag.getInt("poison");
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("poison", poison);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        poison = tag.getInt("poison");
    }
}
