package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.SpringMachineBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.joml.Vector3f;

/**
 * Particle Emitter (TileEntityParticleEmitter): a decorative machine run by a wound coil (no shaft), that puffs the chosen particle above itself, three a tick,
 * for as long as the coil lasts. The 27 particles of the original are picked on its screen; each coil charge lasts 600 ticks times the coil's stiffness.
 */
public class ParticleEmitterBlockEntity extends SpringMachineBlockEntity {
    public static final String NAME = "particle_emitter";
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(176, 194).slot(80, 28).extras(1).customScreen().build();
    public static final int PARTICLES_PER_TICK = 3;

    /** The particles, in the original's order (ReikaParticleHelper), with its atlas place; those the original does not offer are not listed. */
    public enum Kind {
        SMOKE(0, () -> ParticleTypes.SMOKE),
        CRITICAL(1, () -> ParticleTypes.CRIT),
        ENCHANTMENT(2, () -> ParticleTypes.ENCHANTED_HIT),
        FLAME(3, () -> ParticleTypes.FLAME),
        REDSTONE(4, () -> new DustParticleOptions(new Vector3f(1F, 0F, 0F), 1F)),
        BONEMEAL(5, () -> ParticleTypes.HAPPY_VILLAGER),
        BUBBLE(6, () -> ParticleTypes.BUBBLE),
        VOID(7, () -> ParticleTypes.MYCELIUM),
        LARGESMOKE(8, () -> ParticleTypes.LARGE_SMOKE),
        SNOWBALL(9, () -> ParticleTypes.ITEM_SNOWBALL),
        PORTAL(10, () -> ParticleTypes.PORTAL),
        RAIN(11, () -> ParticleTypes.SPLASH),
        EXPLODE(14, () -> ParticleTypes.EXPLOSION),
        HEART(15, () -> ParticleTypes.HEART),
        CLOUD(16, () -> ParticleTypes.CLOUD),
        NOTE(17, () -> ParticleTypes.NOTE),
        ENCHANT(18, () -> ParticleTypes.ENCHANT),
        LAVA(19, () -> ParticleTypes.LAVA),
        SPRINT(20, () -> ParticleTypes.POOF),
        SLIME(21, () -> ParticleTypes.ITEM_SLIME),
        FIREWORK(22, () -> ParticleTypes.FIREWORK),
        MOBSPELL(24, () -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF80FF80)),
        AMBIENTMOBSPELL(25, () -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF8080FF)),
        SPELL(26, () -> ParticleTypes.EFFECT),
        INSTANTSPELL(27, () -> ParticleTypes.INSTANT_EFFECT),
        WITCH(28, () -> ParticleTypes.WITCH),
        ANGRY(30, () -> ParticleTypes.ANGRY_VILLAGER);

        public static final Kind[] LIST = values();
        /** Where its picture is in the original's particle atlas (16 pictures to a row). */
        public final int atlasIndex;
        private final java.util.function.Supplier<ParticleOptions> options;

        Kind(int atlasIndex, java.util.function.Supplier<ParticleOptions> options) {
            this.atlasIndex = atlasIndex;
            this.options = options;
        }

        public ParticleOptions options() {
            return options.get();
        }
    }

    private int kind;
    private final double speedX = 0;
    private final double speedY = 0;
    private final double speedZ = 0;
    private boolean useRedstone;

    public ParticleEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.PARTICLE_EMITTER.type().get(), pos, state, 1, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    protected int baseDischargeTime() {
        return 600;
    }

    public Kind kind() {
        return Kind.LIST[Math.floorMod(kind, Kind.LIST.length)];
    }

    public void setKind(int index) {
        if (index >= 0 && index < Kind.LIST.length) {
            kind = index;
            setChanged();
        }
    }

    public boolean canEmit() {
        return hasCoil() && (!useRedstone || level.hasNeighborSignal(worldPosition));
    }

    @Override
    protected void springTick(boolean hasCoil) {
        if (!(level instanceof ServerLevel server) || !MachineConfig.enabled("particleEmitter") || !canEmit()) {
            return;
        }
        unwind();
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            server.sendParticles(kind().options(), worldPosition.getX() + level.random.nextDouble(), worldPosition.getY() + 2 + level.random.nextDouble() * 4,
                    worldPosition.getZ() + level.random.nextDouble(), 0, speedX, speedY, speedZ, 1);
        }
    }

    @Override
    public int extra(int index) {
        return kind;
    }

    @Override
    protected int extraCount() {
        return 1;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id >= 0 && id < Kind.LIST.length) {
            setKind(id);
            return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("kind", kind);
        tag.putBoolean("redstone", useRedstone);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kind = tag.getInt("kind");
        useRedstone = tag.getBoolean("redstone");
    }
}
