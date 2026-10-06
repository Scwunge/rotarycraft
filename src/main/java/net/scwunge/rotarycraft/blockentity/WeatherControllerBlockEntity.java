package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.api.WeatherControlEvent;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import net.scwunge.rotarycraft.menu.WeatherMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Weather Controller, as the original: with at least 32 kW from below and open sky above it, it reads what is in its 18 slots and
 * sets the weather to match. Sawdust (or any wood dust) clears it; silver iodide brings rain, with redstone dust on top thunder, with
 * glowstone dust a super storm that keeps lightning striking within 64 blocks for as long as it stays stocked. Each change uses up
 * the items and shoots a spent copy of each into the air (as the original), then rests 10 to 30 seconds. Off unless the server enables it
 * (config, world_machines); the super storm's lightning is held back by mobGriefing, claims and weaponBlockDamage like the weapons.
 */
public class WeatherControllerBlockEntity extends ConsumerBlockEntity implements MenuProvider, Owned {
    public static final int SLOTS = 18;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 32768);
    public static final TagKey<Item> WOOD_DUST = ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "dusts/wood"));
    /** Lightning strikes within this many blocks of a super storm's controller. */
    public static final int STORM_RANGE = 64;

    /** What the controller has last been told to make the weather. */
    public enum Mode {
        NONE, SUN, RAIN, THUNDER, SUPERSTORM;

        public boolean isRain() {
            return ordinal() > SUN.ordinal();
        }

        public boolean isThunder() {
            return ordinal() > RAIN.ordinal();
        }

        public boolean hasAction() {
            return this != NONE;
        }
    }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isWeatherItem(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int cooldown;
    private Mode mode = Mode.NONE;
    @Nullable
    private WorldGuard.Owner owner;

    public WeatherControllerBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.WEATHER_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public ItemStackHandler items() {
        return items;
    }

    public Mode mode() {
        return mode;
    }

    public int cooldown() {
        return cooldown;
    }

    public static boolean isWeatherItem(ItemStack stack) {
        return isSawdust(stack) || stack.is(WorldMachineRegistry.SILVER_IODIDE.get()) || stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST);
    }

    private static boolean isSawdust(ItemStack stack) {
        return stack.is(RotaryItems.SAWDUST.get()) || stack.is(WOOD_DUST);
    }

    /** What automation may do: put the weather items in, and nothing out (as the original). */
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
                return ItemStack.EMPTY;
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

    /** It takes power from below only. */
    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, Direction.DOWN);
        setPower(in.torque(), in.omega());
        machineTick(hasEnoughPower());
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (!powered || !RotaryConfig.worldMachineEnabled("weatherController") || !server.canSeeSky(worldPosition.above())) {
            return;
        }
        if (mode == Mode.SUPERSTORM) {
            if (!server.getLevelData().isRaining() || !server.getLevelData().isThundering()) {
                server.setWeatherParameters(0, stormTicks(server), true, true);
            }
            if (server.random.nextInt(20) == 0) {
                strike(server);
            }
        }
        if (cooldown > 0) {
            return;
        }
        mode = chooseMode(server);
        if (isAlready(server, mode) || !mode.hasAction()) {
            return;
        }
        boolean rain = mode.isRain();
        boolean thunder = mode.isThunder();
        if (rain) {
            server.setWeatherParameters(0, stormTicks(server), true, thunder);
        } else {
            server.setWeatherParameters(12000 + server.random.nextInt(168000), 0, false, false);
        }
        NeoForge.EVENT_BUS.post(new WeatherControlEvent(this, rain, thunder, mode == Mode.SUPERSTORM));
        setChanged();
    }

    /** How long a storm lasts before the world's own weather clock may end it (vanilla's rain lasts 12000 to 24000 ticks). */
    private static int stormTicks(ServerLevel server) {
        return 12000 + server.random.nextInt(12000);
    }

    /** Whether the weather is already set that way (the weather's flags, not its strength, which fades in and out over seconds). */
    private static boolean isAlready(ServerLevel server, Mode mode) {
        return mode.isRain() == server.getLevelData().isRaining() && mode.isThunder() == server.getLevelData().isThundering();
    }

    private int find(java.util.function.Predicate<ItemStack> test) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty() && test.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Reads the slots and decides what to make the weather. When the weather already is that, nothing is used up and the mode stays
     * as it was; otherwise it rests, shoots out spent copies of what it burns and uses it up.
     */
    private Mode chooseMode(ServerLevel server) {
        int sawdust = find(WeatherControllerBlockEntity::isSawdust);
        int iodide = find(s -> s.is(WorldMachineRegistry.SILVER_IODIDE.get()));
        int redstone = find(s -> s.is(Items.REDSTONE));
        int glowdust = find(s -> s.is(Items.GLOWSTONE_DUST));
        Mode chosen;
        int first = -1;
        int second = -1;
        if (sawdust >= 0) {
            chosen = Mode.SUN;
            first = sawdust;
        } else if (iodide >= 0) {
            chosen = Mode.RAIN;
            first = iodide;
            if (redstone >= 0) {
                chosen = Mode.THUNDER;
                second = redstone;
            } else if (glowdust >= 0) {
                chosen = Mode.SUPERSTORM;
                second = glowdust;
            }
        } else {
            chosen = Mode.NONE;
        }
        if (chosen.isRain() && RotaryConfig.get(RotaryConfig.WEATHER_BANS_RAIN)) {
            chosen = Mode.NONE;
        }
        if (isAlready(server, chosen)) {
            return mode;
        }
        cooldown = 200 + server.random.nextInt(400);
        if (chosen.hasAction()) {
            fire(server, first, second);
            items.extractItem(first, 1, false);
            if (second >= 0) {
                items.extractItem(second, 1, false);
            }
        }
        return chosen;
    }

    /** The spent items are shot up out of the top, picked up by no one and gone in a few seconds. */
    private void fire(ServerLevel server, int first, int second) {
        server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
        for (int slot : new int[] {first, second}) {
            if (slot < 0) {
                continue;
            }
            ItemEntity spent = new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0625, worldPosition.getZ() + 0.5,
                    items.getStackInSlot(slot).copyWithCount(1));
            spent.setDeltaMovement((server.random.nextDouble() - 0.5) * 0.4, 3, (server.random.nextDouble() - 0.5) * 0.4);
            spent.setPickUpDelay(5000);
            spent.lifespan = 100;
            server.addFreshEntity(spent);
        }
    }

    /** A bolt at a random spot within range, unless the owner may not change what stands there. */
    private void strike(ServerLevel server) {
        int x = worldPosition.getX() - STORM_RANGE + server.random.nextInt(2 * STORM_RANGE + 1);
        int z = worldPosition.getZ() - STORM_RANGE + server.random.nextInt(2 * STORM_RANGE + 1);
        if (!server.hasChunkAt(new BlockPos(x, worldPosition.getY(), z)) || !RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE)) {
            return;
        }
        BlockPos top = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (!WorldGuard.mayChange(server, top.below(), owner)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(top));
            server.addFreshEntity(bolt);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeatherMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("cooldown", cooldown);
        tag.putInt("mode", mode.ordinal());
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        cooldown = tag.getInt("cooldown");
        mode = Mode.values()[Math.max(0, Math.min(Mode.values().length - 1, tag.getInt("mode")))];
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }
}
