package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.HeatEffects;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

import java.util.List;
import java.util.function.Predicate;

/**
 * Igniter (TileEntityIgniter): burns fuel to heat itself, up to 2500 C, and then sets alight, melts or boils things all round it, in a range that grows with its
 * heat, a number of random spots every tick (one for every 50 degrees), and from 280 C sets the living things there on fire for a second (the player who placed
 * it excepted). The fuels, hottest used first: wood 400 C, coal 600, blaze powder 800, a lava bucket 1200, and thermite 2500 (aluminium dust and iron). Needs
 * 1 N*m at 1024 rad/s and 32 kW, from any side. Every spot it changes is checked against claims as its owner, and it is off unless the server enables it.
 */
public class IgniterBlockEntity extends InventoryMachineBlockEntity implements Heatable {
    /** The block is the original's "firestarter": the name igniter is taken by a part. */
    public static final String NAME = "firestarter";
    public static final int SLOTS = 18;
    public static final int MAX_TEMPERATURE = 2500;
    public static final int ANIMAL_IGNITION = 280;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1024, 32768);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(176, 167).inventoryAt(8, 85).grid(8, 18, 9, 2).build();

    /** A fuel: the temperature it gives, and what must be in the slots (one of each kind of ingredient). */
    public enum Fuel {
        WOOD(400, tag(ItemTags.PLANKS).or(tag(ItemTags.LOGS))),
        COAL(600, s -> s.is(Items.COAL)),
        BLAZE(800, s -> s.is(Items.BLAZE_POWDER)),
        LAVA(1200, s -> s.is(Items.LAVA_BUCKET)),
        THERMITE(2500, tag(ItemTags.create(ResourceLocation.parse("c:dusts/aluminum"))), s -> s.is(Items.IRON_INGOT) || s.is(Items.RAW_IRON)
                || s.is(ItemTags.create(ResourceLocation.parse("c:dusts/iron"))));

        public static final Fuel[] LIST = values();
        public final int temperature;
        private final List<Predicate<ItemStack>> needs;

        @SafeVarargs
        Fuel(int temperature, Predicate<ItemStack>... needs) {
            this.temperature = temperature;
            this.needs = List.of(needs);
        }

        private static Predicate<ItemStack> tag(TagKey<Item> tag) {
            return s -> s.is(tag);
        }

        public boolean accepts(ItemStack stack) {
            return needs.stream().anyMatch(n -> n.test(stack));
        }
    }

    private int temperature = 20;
    private Fuel fuel;
    private int ticks;

    public IgniterBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.IGNITER.type().get(), pos, state, SLOTS, NAME);
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
    protected boolean acceptsItem(int slot, ItemStack stack) {
        for (Fuel f : Fuel.LIST) {
            if (f.accepts(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    // ---- Heatable ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    public void setCurrentTemperature(int t) {
        temperature = t;
        setChanged();
    }

    public Fuel fuel() {
        return fuel;
    }

    /** How far it reaches: sixteen blocks and more, rounded up to eights, as it gets hotter. */
    public int range() {
        int more = (int) Math.sqrt(temperature * 2D);
        return 16 + (more + 7) / 8 * 8;
    }

    /** The hottest fuel that has all of its ingredients in the slots. */
    private Fuel findFuel() {
        for (int f = Fuel.LIST.length - 1; f >= 0; f--) {
            Fuel candidate = Fuel.LIST[f];
            boolean all = true;
            for (Predicate<ItemStack> need : candidate.needs) {
                boolean found = false;
                for (int i = 0; i < SLOTS && !found; i++) {
                    found = need.test(items.getStackInSlot(i));
                }
                if (!found) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return candidate;
            }
        }
        return null;
    }

    private void burn(Fuel fuel) {
        for (Predicate<ItemStack> need : fuel.needs) {
            for (int i = 0; i < SLOTS; i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (need.test(stack)) {
                    ItemStack left = stack.getCraftingRemainingItem();
                    items.extractItem(i, 1, false);
                    for (int k = 0; k < SLOTS && !left.isEmpty(); k++) {
                        left = items.insertItem(k, left, false);
                    }
                    if (!left.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
                    }
                    break;
                }
            }
        }
        if (temperature < fuel.temperature) {
            temperature += (fuel.temperature - temperature) / 4;
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("igniter")) {
            return;
        }
        if (++ticks >= 40) {
            ticks = 0;
            int ambient = Ambient.temperature(server, worldPosition);
            if (temperature > ambient) {
                temperature -= (int) Math.log(temperature - ambient);
            } else if (temperature < ambient) {
                temperature += (ambient - temperature) / 24;
            }
            temperature = Math.min(temperature, MAX_TEMPERATURE);
            fuel = findFuel();
            if (fuel != null && temperature < fuel.temperature) {
                burn(fuel);
            }
        }
        if (fuel == null) {
            return;
        }
        int spread = range();
        int ySpread = spread / 2;
        int n = 1 + temperature / 50;
        for (int i = 0; i < n; i++) {
            BlockPos at = worldPosition.offset(server.random.nextInt(2 * spread + 1) - spread, server.random.nextInt(2 * ySpread + 1) - ySpread,
                    server.random.nextInt(2 * spread + 1) - spread);
            HeatEffects.affect(server, at, temperature, owner);
        }
        if (server.getGameTime() % 4 == 0) {
            server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
        }
        if (temperature >= ANIMAL_IGNITION) {
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(spread, ySpread, spread))) {
                if (!(e instanceof Player player && owner != null && owner.id().equals(player.getUUID()))) {
                    e.igniteForSeconds(1);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : 20;
    }
}
