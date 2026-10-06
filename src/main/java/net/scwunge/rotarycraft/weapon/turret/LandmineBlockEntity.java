package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.CannonTnt;
import net.scwunge.rotarycraft.weapon.LandmineMenu;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Landmine, as the original: a wound coil in the middle slot keeps it armed (it unwinds slowly, and a nearly flat coil makes it
 * go off on its own), gunpowder in the four left slots sets the blast (2 blocks of power each, up to 8), and the four right slots
 * take modifiers: blaze powder makes it fiery, a spider eye poisons, TNT adds a rain of TNT, glass adds shrapnel. It goes off when
 * a creature that is not sneaking walks over it, or an arrow lands by it, and everything within 2 blocks is hurt, blinded and
 * dazed. The blast damages blocks only with weaponBlockDamage, mobGriefing and the owner's permission.
 */
public class LandmineBlockEntity extends BlockEntity implements MenuProvider, OneSlotMenu.Host, Owned {
    public static final int SLOTS = 9;
    public static final int BASE_DISCHARGE_TIME = 360;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return validIn(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }
    };
    private int ticks;
    private boolean exploded;
    @Nullable
    private WorldGuard.Owner owner;

    public LandmineBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.LANDMINE_BE.get(), pos, state);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    static boolean validIn(int slot, ItemStack stack) {
        if (slot == 0) {
            return stack.getItem() instanceof CoilItem;
        }
        if (slot <= 4) {
            return stack.is(Items.GUNPOWDER);
        }
        return slot <= 8 && isModifier(stack);
    }

    private static boolean isModifier(ItemStack stack) {
        return stack.is(Items.BLAZE_POWDER) || stack.is(Items.SPIDER_EYE) || stack.is(Items.TNT) || stack.is(Items.GLASS);
    }

    private boolean hasCoil() {
        ItemStack coil = items.getStackInSlot(0);
        return coil.getItem() instanceof CoilItem && CoilItem.charge(coil) > 0;
    }

    private boolean has(net.minecraft.world.item.Item item) {
        for (int i = 5; i <= 8; i++) {
            if (items.getStackInSlot(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private float explosionPower() {
        int powder = 0;
        for (int i = 1; i <= 4; i++) {
            if (items.getStackInSlot(i).is(Items.GUNPOWDER)) {
                powder++;
            }
        }
        return 2F * powder;
    }

    /** What automation may do: put things in, and take out the coil once it has run down (as the original). */
    public IItemHandler automationItems() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 && CoilItem.charge(items.getStackInSlot(0)) == 0 ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return items.isItemValid(slot, stack);
            }
        };
    }

    public void serverTick() {
        if (exploded || !hasCoil() || !RotaryConfig.weaponEnabled("landmine")) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        ItemStack coil = items.getStackInSlot(0);
        CoilItem item = (CoilItem) coil.getItem();
        if (++ticks > BASE_DISCHARGE_TIME * item.stiffness()) {
            ItemStack unwound = coil.copy();
            CoilItem.setCharge(unwound, CoilItem.charge(coil) - 1);
            items.setStackInSlot(0, unwound);
            ticks = 0;
        }
        if (ageFails(server) || arrowNear(server) || creatureOn(server)) {
            detonate(server);
        }
    }

    /** A coil that has nearly run out makes the mine unstable. */
    private boolean ageFails(ServerLevel server) {
        if (server.random.nextInt(20) > 0) {
            return false;
        }
        int charge = CoilItem.charge(items.getStackInSlot(0));
        return charge < CoilItem.MAX_CHARGE && server.random.nextInt(1 + charge) == 0;
    }

    private boolean arrowNear(ServerLevel server) {
        return !server.getEntitiesOfClass(AbstractArrow.class, new AABB(worldPosition).inflate(1)).isEmpty();
    }

    private boolean creatureOn(ServerLevel server) {
        AABB above = new AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 3, worldPosition.getZ() + 1);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, above)) {
            if (e.onGround() && !e.isShiftKeyDown() && !(e instanceof Player p && p.isSpectator())) {
                return true;
            }
        }
        return false;
    }

    /** Opening the screen of an armed mine can set it off, the likelier the flatter its coil. */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        int charge = CoilItem.charge(items.getStackInSlot(0));
        if (level instanceof ServerLevel server && items.getStackInSlot(0).getItem() instanceof CoilItem
                && server.random.nextInt(Math.max(1, charge)) / 2 == 0 && charge > 0) {
            detonate(server);
            return null;
        }
        return new LandmineMenu(id, inventory, this);
    }

    public void detonate(ServerLevel server) {
        if (exploded) {
            return;
        }
        exploded = true;
        float power = explosionPower();
        boolean flaming = has(Items.BLAZE_POWDER);
        boolean poison = has(Items.SPIDER_EYE);
        boolean chain = has(Items.TNT);
        boolean shrapnel = has(Items.GLASS);
        BlockPos pos = worldPosition;
        boolean damage = RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE) && WorldGuard.mayChange(server, pos, owner);
        ItemStack coil = items.getStackInSlot(0);
        items.setStackInSlot(0, ItemStack.EMPTY);
        server.removeBlock(pos, false);
        if (!coil.isEmpty()) {
            Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, coil);
        }
        if (chain) {
            for (int i = 0; i < 12; i++) {
                server.addFreshEntity(new CannonTnt(server, pos.getX() - 5 + server.random.nextInt(11), pos.getY() - 5 + server.random.nextInt(11),
                        pos.getZ() - 5 + server.random.nextInt(11), 5 + server.random.nextInt(10), owner));
            }
        }
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, flaming && damage,
                damage ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        DamageSource blast = server.damageSources().explosion(null, null);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2))) {
            e.hurt(blast, (int) power * 4);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0));
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 450, 5));
            if (poison) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            }
            if (e instanceof Creeper) {
                server.explode(e, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, damage ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            }
        }
        if (shrapnel) {
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(8))) {
                double d = e.position().distanceTo(pos.getCenter());
                e.hurt(server.damageSources().generic(), d < 4 ? 8 : d < 8 ? 6 : 4);
                server.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("ticks", ticks);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        ticks = tag.getInt("ticks");
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }
}
