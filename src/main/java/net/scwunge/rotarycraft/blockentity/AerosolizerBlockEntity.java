package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aerosolizer (TileEntityAerosolizer): nine slots take potions, which it empties into bottles and keeps as a store of up to 64 per slot (one for a
 * potion, three for an extended one), then every living thing in the space around it, as far as the nearest solid block along each axis, gets the
 * effect: five seconds' worth, topped up every 20 ticks. Several slots of the same potion strengthen it and top it up faster. A stored unit lasts two
 * minutes. Instant potions are not taken. Power comes in from any side; it needs 16 kW. The comparator reads how full it is.
 */
public class AerosolizerBlockEntity extends InventoryMachineBlockEntity implements MachineInteractions {
    public static final String NAME = "aerosolizer";
    public static final int SLOTS = 9;
    public static final int CAPACITY = 64;
    public static final int DRAIN_TICKS = 2400;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 16384);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).grid(62, 17, 3, 3).extras(2 * SLOTS).customScreen().build();

    /** One potion as stored: its effects (each with its strength) and colour. */
    public record Brew(List<Effect> effects, int color, int strength) {
    }

    public record Effect(ResourceLocation id, int amplifier) {
    }

    private final Brew[] brews = new Brew[SLOTS];
    private final int[] levels = new int[SLOTS];
    private int drainTicks;
    private int dispenseTicks;

    public AerosolizerBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.AEROSOLIZER.type().get(), pos, state, SLOTS, NAME);
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

    public static int maxRange() {
        return Math.max(64, MachineConfig.get(MachineConfig.AEROSOLIZER_RANGE));
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    /** Only the empty bottles come out. */
    @Override
    protected boolean mayExtract(int slot) {
        return items.getStackInSlot(slot).is(Items.GLASS_BOTTLE);
    }

    public int level(int slot) {
        return levels[slot];
    }

    public int color(int slot) {
        return brews[slot] == null ? 0 : 0xFF000000 | brews[slot].color();
    }

    public Brew brew(int slot) {
        return brews[slot];
    }

    /** How many units of stored potion there are in all. */
    public int stored() {
        int sum = 0;
        for (int level : levels) {
            sum += level;
        }
        return sum;
    }

    @Override
    public int comparatorSignal() {
        return (int) (stored() / (double) (CAPACITY * SLOTS) * 15);
    }

    public boolean hasPotions() {
        return stored() > 0;
    }

    /** What the potion in {@code stack} gives, or null for water, instant potions and the like. */
    public static Brew brewOf(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        List<Effect> effects = new ArrayList<>();
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().value().isInstantenous()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            effects.add(new Effect(id, Math.min(1, instance.getAmplifier())));
        }
        if (effects.isEmpty()) {
            return null;
        }
        effects.sort(java.util.Comparator.comparing(e -> e.id().toString()));
        boolean extended = contents.potion().flatMap(p -> p.unwrapKey()).map(k -> k.location().getPath().startsWith("long_")).orElse(false);
        return new Brew(List.copyOf(effects), contents.getColor() & 0xFFFFFF, extended ? 3 : 1);
    }

    private static boolean sameEffects(Brew a, Brew b) {
        return a == b || a == null || b == null || a.effects().equals(b.effects());
    }

    /** Empties the potions in the slots into the store, leaving bottles. */
    private void storePotions() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty() || stack.is(Items.GLASS_BOTTLE)) {
                continue;
            }
            Brew brew = brewOf(stack);
            if (brew == null) {
                continue;
            }
            int amount = stack.getCount() * brew.strength();
            if (sameEffects(brew, brews[i]) && levels[i] + amount <= CAPACITY) {
                brews[i] = brew;
                levels[i] += amount;
                items.setStackInSlot(i, new ItemStack(Items.GLASS_BOTTLE, stack.getCount()));
                markClientDirty();
            }
        }
    }

    /** How many slots hold this potion. */
    public int multiplier(int slot) {
        if (brews[slot] == null) {
            return 0;
        }
        int copies = 0;
        for (Brew brew : brews) {
            if (brew != null && brew.effects().equals(brews[slot].effects())) {
                copies++;
            }
        }
        return copies;
    }

    /** The space it fills: along each axis out to the nearest opaque block (or the range), as a box about itself. */
    public AABB room() {
        int range = maxRange();
        int[] reach = new int[6];
        for (Direction dir : Direction.values()) {
            int i = 1;
            while (i < range && level.isLoaded(worldPosition.relative(dir, i))
                    && !level.getBlockState(worldPosition.relative(dir, i)).isSolidRender(level, worldPosition.relative(dir, i))) {
                i++;
            }
            reach[dir.ordinal()] = i - 1;
        }
        return new AABB(worldPosition.getX() - reach[Direction.WEST.ordinal()], worldPosition.getY() - reach[Direction.DOWN.ordinal()],
                worldPosition.getZ() - reach[Direction.NORTH.ordinal()], worldPosition.getX() + reach[Direction.EAST.ordinal()] + 1,
                worldPosition.getY() + reach[Direction.UP.ordinal()] + 1, worldPosition.getZ() + reach[Direction.SOUTH.ordinal()] + 1);
    }

    private void dispense(ServerLevel server, int slot, AABB room) {
        Brew brew = brews[slot];
        int bonus = multiplier(slot) - 1;
        for (LivingEntity mob : server.getEntitiesOfClass(LivingEntity.class, room)) {
            for (Effect effect : brew.effects()) {
                MobEffect type = BuiltInRegistries.MOB_EFFECT.get(effect.id());
                if (type == null) {
                    continue;
                }
                int extra = effect.amplifier() == 1 ? bonus * 2 : bonus;
                mob.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(type), 100, effect.amplifier() + extra));
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        storePotions();
        if (!powered || !MachineConfig.enabled("aerosolizer")) {
            return;
        }
        dispenseTicks++;
        drainTicks++;
        AABB room = null;
        for (int i = 0; i < SLOTS; i++) {
            if (levels[i] <= 0 || brews[i] == null) {
                continue;
            }
            if (dispenseTicks % Math.max(1, 20 / Math.max(multiplier(i), 1)) == 0) {
                if (room == null) {
                    room = room();
                }
                dispense(server, i, room);
            }
        }
        if (drainTicks >= DRAIN_TICKS) {
            drainTicks = 0;
            for (int i = 0; i < SLOTS; i++) {
                if (levels[i] > 0 && --levels[i] <= 0) {
                    brews[i] = null;
                }
            }
            markClientDirty();
        }
        if (dispenseTicks >= 20 * 60) {
            dispenseTicks = 0;
        }
    }

    @Override
    public int extra(int index) {
        return index < SLOTS ? levels[index] : brews[index - SLOTS] == null ? 0 : brews[index - SLOTS].color();
    }

    @Override
    protected int extraCount() {
        return 2 * SLOTS;
    }

    @Override
    protected void writeClient(CompoundTag tag) {
        super.writeClient(tag);
        writeBrews(tag);
    }

    @Override
    protected void readClient(CompoundTag tag) {
        super.readClient(tag);
        readBrews(tag);
    }

    private void writeBrews(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag one = new CompoundTag();
            one.putInt("level", levels[i]);
            if (brews[i] != null) {
                one.putInt("color", brews[i].color());
                one.putInt("strength", brews[i].strength());
                ListTag effects = new ListTag();
                for (Effect effect : brews[i].effects()) {
                    CompoundTag e = new CompoundTag();
                    e.putString("id", effect.id().toString());
                    e.putInt("amp", effect.amplifier());
                    effects.add(e);
                }
                one.put("effects", effects);
            }
            list.add(one);
        }
        tag.put("brews", list);
    }

    private void readBrews(CompoundTag tag) {
        ListTag list = tag.getList("brews", Tag.TAG_COMPOUND);
        for (int i = 0; i < SLOTS; i++) {
            brews[i] = null;
            levels[i] = 0;
            if (i >= list.size()) {
                continue;
            }
            CompoundTag one = list.getCompound(i);
            levels[i] = one.getInt("level");
            if (one.contains("effects")) {
                List<Effect> effects = new ArrayList<>();
                for (Tag t : one.getList("effects", Tag.TAG_COMPOUND)) {
                    CompoundTag e = (CompoundTag) t;
                    effects.add(new Effect(ResourceLocation.parse(e.getString("id")), e.getInt("amp")));
                }
                brews[i] = new Brew(List.copyOf(effects), one.getInt("color"), Math.max(1, one.getInt("strength")));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeBrews(tag);
        tag.putInt("drain", drainTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readBrews(tag);
        drainTicks = tag.getInt("drain");
        for (int i = 0; i < SLOTS; i++) {
            if (levels[i] <= 0) {
                brews[i] = null;
            }
        }
        Objects.requireNonNull(brews);
    }
}
