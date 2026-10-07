package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;

import java.util.List;
import java.util.Set;

/**
 * The Auto-Breeder, as the original: 16 kW (from any side) and the food the animals like in its 18 slots. Animals round it (8 blocks, one
 * more for each 2048 W above the minimum, to the configured limit) that would take some of the food walk to it (a try each second); a grown animal
 * that is ready and within 2.4 blocks of it is put in love, using up one food item. Pets that are sitting, or not tamed, are left alone.
 * The animals then pair off and breed by themselves. Villagers are left to their own ways (breeding them needs beds and willingness).
 */
public class AutoBreederBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 16384);
    public static final int SLOTS = 18;
    public static final int FALLOFF = 2048;

    /** What animals eat, for what may go in the slots (an animal still has the last word on what it will take). */
    private static final Set<Item> FEED = Set.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.CARROT,
            Items.POTATO, Items.BEETROOT, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.APPLE, Items.HAY_BLOCK, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.BEEF,
            Items.COOKED_BEEF, Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON, Items.RABBIT, Items.COOKED_RABBIT, Items.ROTTEN_FLESH,
            Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.BAMBOO, Items.SEAGRASS,
            Items.DANDELION, Items.CACTUS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD, Items.SLIME_BALL, Items.SPIDER_EYE, Items.KELP, Items.TROPICAL_FISH,
            Items.AXOLOTL_BUCKET, Items.TROPICAL_FISH_BUCKET, Items.WARPED_FUNGUS, Items.CRIMSON_FUNGUS, Items.SUGAR_CANE, Items.BROWN_MUSHROOM);

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFeed(stack);
        }
    };

    public AutoBreederBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.AUTO_BREEDER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "autoBreeder";
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
    public int menuRows() {
        return SLOTS / 9;
    }

    public static boolean isFeed(ItemStack stack) {
        return FEED.contains(stack.getItem()) || stack.is(RotaryItems.CANOLA_HUSKS.get());
    }

    public static int maxRange() {
        return Math.max(24, RotaryConfig.get(FarmConfig.BREEDER_RANGE));
    }

    public int range() {
        if (getPower() < REQUIREMENT.minPower()) {
            return 0;
        }
        return (int) Math.min(8 + (getPower() - REQUIREMENT.minPower()) / FALLOFF, maxRange());
    }

    @Override
    protected int[] status() {
        return new int[] {range()};
    }

    /** The slot holding food this animal would take, or -1. */
    private int feedSlot(Animal animal) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty() && (animal.isFood(stack) || stack.is(RotaryItems.CANOLA_HUSKS.get()) && isHerbivore(animal))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHerbivore(Animal animal) {
        return animal instanceof net.minecraft.world.entity.animal.Sheep || animal instanceof net.minecraft.world.entity.animal.Cow
                || animal instanceof net.minecraft.world.entity.animal.Chicken || animal instanceof net.minecraft.world.entity.animal.Pig;
    }

    private boolean hasFeedClient;

    public boolean hasFeedClient() {
        return hasFeedClient;
    }

    @Override
    protected void writeClientData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("feed", !isIdle());
    }

    @Override
    protected void readClientData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        hasFeedClient = tag.getBoolean("feed");
    }

    /** Whether any animal that could be fed is idle (the original's comparator reading). */
    public boolean isIdle() {
        for (int i = 0; i < SLOTS; i++) {
            if (isFeed(items.getStackInSlot(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            return;
        }
        ServerLevel server = server();
        boolean pathing = server.getGameTime() % 20 == 0;
        if (pathing && hasFeedClient == isIdle()) {
            hasFeedClient = !isIdle();
            syncNow();
        }
        AABB box = new AABB(worldPosition).inflate(range());
        for (Animal animal : server.getEntitiesOfClass(Animal.class, box)) {
            int slot = feedSlot(animal);
            if (slot < 0) {
                continue;
            }
            boolean pet = animal instanceof TamableAnimal;
            if (pathing && !(pet && ((TamableAnimal) animal).isOrderedToSit())) {
                if (!animal.isInLove() && !animal.isBaby() && animal.getAge() == 0) {
                    animal.getNavigation().moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, 1.0);
                } else {
                    animal.getNavigation().stop();
                }
            }
            if (!animal.isBaby() && animal.getAge() <= 0 && animal.position().distanceTo(worldPosition.getCenter()) <= 2.4
                    && (!pet || ((TamableAnimal) animal).isTame()) && !animal.isInLove()) {
                animal.setInLove(null);
                items.extractItem(slot, 1, false);
                int n = 1 + server.random.nextInt(3);
                for (int i = 0; i < n; i++) {
                    server.sendParticles(ParticleTypes.HEART, animal.getX() + server.random.nextFloat() * animal.getBbWidth() * 2 - animal.getBbWidth(),
                            animal.getY() + 0.5 + server.random.nextFloat() * animal.getBbHeight(),
                            animal.getZ() + server.random.nextFloat() * animal.getBbWidth() * 2 - animal.getBbWidth(), 1, 0, 0, 0, 0.02);
                }
            }
        }
    }

    /** Lets tests see which animals it would work on. */
    public List<Animal> animals() {
        return server().getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(range()));
    }
}
