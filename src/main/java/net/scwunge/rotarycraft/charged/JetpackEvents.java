package net.scwunge.rotarycraft.charged;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.ToolRegistry;
import net.scwunge.rotarycraft.tool.BedrockTools;
import net.scwunge.rotarycraft.tool.Forced;

/**
 * Flying with a jetpack. Both sides read the wearer's input (the jump key, the movement keys and sneak reach the server in the player input packet): the player's
 * own client does the moving (a server cannot move a player), and the server does what must not be trusted to the client, the fuel, the fall distance, the
 * explosion and the sound.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class JetpackEvents {
    /** How many ticks the jump key is held on the ground before a conservative pack fires. */
    public static final int HOLD = 6;
    private static final String HELD = "rotarycraft_pack_hold";
    private static final double SPEED_CAP = 4;

    private JetpackEvents() {}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack pack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (pack.getItem() instanceof Jetpack jetpack) {
            fly(player, pack, jetpack);
        } else if (player.getPersistentData().getInt(HELD) != 0) {
            player.getPersistentData().putInt(HELD, 0);
        }
    }

    /** The one that moves this player: their own client (a fake player, which has no client, is moved by the server). */
    private static boolean mover(Player player) {
        return player.level().isClientSide() ? player.isLocalPlayer() : player instanceof FakePlayer;
    }

    private static boolean ready(Player player) {
        int held = player.jumping ? player.getPersistentData().getInt(HELD) + 1 : 0;
        player.getPersistentData().putInt(HELD, held);
        if (!player.jumping) {
            return false;
        }
        return !RotaryConfig.get(RotaryConfig.JETPACK_CONSERVATIVE) || held >= HOLD;
    }

    static void fly(Player ep, ItemStack pack, Jetpack jetpack) {
        Level level = ep.level();
        boolean server = !level.isClientSide();
        boolean flying = ready(ep) && !ep.getAbilities().flying;
        boolean fuel = Jetpack.fuel(pack) > 0;
        boolean cooled = Jetpack.Upgrade.COOLING.on(pack);

        if (server && fuel && !cooled) {
            if (ep.isInLava() && level.getDifficulty() != Difficulty.PEACEFUL) {
                explode(ep, jetpack);
                return;
            } else if (ep.isOnFire() && level.getDifficulty().getId() > Difficulty.EASY.getId() && flying) {
                explode(ep, jetpack);
                return;
            }
        }

        boolean hover = flying && ep.isShiftKeyDown();
        boolean jetbonus = !RotaryConfig.get(RotaryConfig.JETPACK_NEEDS_JET_FUEL) && Jetpack.jetFueled(pack);
        boolean propel = Jetpack.Upgrade.JET.on(pack) && Jetpack.jetFueled(pack);
        boolean winged = Jetpack.winged(pack);
        float forward = Math.signum(ep.zza);
        float strafe = Math.signum(ep.xxa);
        boolean canFly = !hover || (!ep.onGround() && vertical(ep) < 0);
        boolean thrusting = flying && canFly && fuel;

        if (server && flying && canFly && !ep.getAbilities().instabuild && level.getGameTime() % 2 == 0) {
            int factor = jetpack.kind().fuelFactor();
            Jetpack.use(pack, (hover ? 2 : 1) * factor);
            if (Jetpack.fuel(pack) > 0) {
                Jetpack.use(pack, ((forward != 0 ? 1 : 0) + (strafe != 0 ? 1 : 0)) * factor);
            }
        }

        if (thrusting && server) {
            ep.fallDistance = -2;
            if (RotaryConfig.get(RotaryConfig.JETPACK_BYPASSES_FLY_CHECK) && ep instanceof ServerPlayer sp && sp.connection != null) {
                sp.connection.aboveGroundTickCount = 0;
                sp.connection.aboveGroundVehicleTickCount = 0;
            }
            long time = level.getGameTime();
            if (time % 6 == 0) {
                float pitch = 1 + 0.5F * (float) Math.sin(time * 0.3);
                level.playSound(null, ep.getX(), ep.getY(), ep.getZ(), MachineSoundRegistry.get("pack").get(), SoundSource.PLAYERS, 0.75F, pitch);
            }
            if (propel && time % 3 == 0) {
                level.playSound(null, ep.getX(), ep.getY(), ep.getZ(), MachineSoundRegistry.get("shortjet").get(), SoundSource.PLAYERS, 0.15F, 1F);
            }
        }

        if (server && flying && fuel && level.getDifficulty() != Difficulty.PEACEFUL && level.random.nextInt(4) == 0) {
            ignite(ep);
        }

        if (mover(ep)) {
            if (thrusting) {
                thrust(ep, hover, jetbonus, propel, winged, forward, strafe);
            }
            if (winged && !hover) {
                glide(ep, true);
            }
        } else if (server && winged && !hover) {
            glide(ep, false);
        }
    }

    private static double vertical(Player ep) {
        return ep.level().isClientSide() || ep instanceof FakePlayer ? ep.getDeltaMovement().y : ep.getY() - ep.yo;
    }

    private static void thrust(Player ep, boolean hover, boolean jetbonus, boolean propel, boolean winged, float forward, float strafe) {
        float maxSpeed = jetbonus ? 3 : 1.25F;
        boolean horizontal = forward != 0 || strafe != 0;
        float thrust = winged ? 0.15F : hover ? 0.05F : 0.1F;
        if (propel) {
            thrust *= hover ? 2 : 4;
        }
        if (jetbonus) {
            thrust *= 1.25F;
        }
        if (ep.isPassenger()) {
            thrust *= 1.25F;
        }
        Vec3 motion = ep.getDeltaMovement();
        double vy = motion.y;
        if (hover) {
            vy = vy > 0 ? Math.max(vy * 0.75, 0) : Math.min(vy + 0.15, 0);
        } else {
            double deltav = vy > 0 ? Math.min(0.2, Math.max(0.05, (maxSpeed - vy) * 0.25)) : 0.2;
            if (jetbonus && !horizontal) {
                deltav *= 1.5;
            }
            if (ep.isPassenger()) {
                deltav *= 1.5;
            }
            vy = Math.min(vy + deltav, maxSpeed);
        }
        ep.setDeltaMovement(motion.x, vy, motion.z);
        if (horizontal) {
            ep.moveRelative(thrust * thrust, new Vec3(strafe, 0, forward));
            Vec3 now = ep.getDeltaMovement();
            double h = Math.sqrt(now.x * now.x + now.z * now.z);
            if (h > SPEED_CAP) {
                ep.setDeltaMovement(now.x * SPEED_CAP / h, now.y, now.z * SPEED_CAP / h);
            }
        }
        if (ep.isPassenger()) {
            ep.getVehicle().setDeltaMovement(ep.getDeltaMovement());
            ep.getVehicle().fallDistance = ep.fallDistance;
        }
    }

    /** What the wings do on the way down: slow the fall (steeper when looking level) and carry you forward. */
    private static void glide(Player ep, boolean move) {
        if (ep.isSleeping() || ep.onGround()) {
            return;
        }
        double vy = move ? ep.getDeltaMovement().y : ep.getY() - ep.yo;
        if (vy >= 0) {
            return;
        }
        boolean sneak = ep.isShiftKeyDown() != RotaryConfig.get(RotaryConfig.JETPACK_WINGS_ON_SNEAK);
        double ang = Math.cos(Math.toRadians(ep.getXRot()));
        double d = vy <= -2 ? 0.0625 : vy <= -1 ? 0.125 : vy <= -0.5 ? 0.25 : 0.5;
        if (sneak) {
            d *= 0.125;
        }
        double fac = 1 - d * ang;
        if (move) {
            Vec3 motion = ep.getDeltaMovement();
            double vh = 0.05 * ang;
            double yaw = Math.toRadians(ep.getYRot() + 90);
            ep.setDeltaMovement(motion.x + Math.cos(yaw) * vh, motion.y * fac, motion.z + Math.sin(yaw) * vh);
        }
        fac *= sneak ? 0.999 : 0.9;
        ep.fallDistance *= (float) fac;
    }

    /** The exhaust lights the fuel (the mod's ethanol and jet fuel) that is underneath you. */
    private static void ignite(Player ep) {
        Level level = ep.level();
        BlockPos at = ep.blockPosition().offset(level.random.nextInt(3) - 1, -level.random.nextInt(3), level.random.nextInt(3) - 1);
        var type = level.getFluidState(at).getType().getFluidType();
        if (type == RotaryFluids.ETHANOL.type.get() || type == RotaryFluids.JET_FUEL.type.get()) {
            level.setBlockAndUpdate(at, Blocks.FIRE.defaultBlockState());
        }
    }

    /** A burning pack in lava (or on a burning wearer): the pack is destroyed (a plain chestplate is left), a blast and a throw. */
    public static void explode(Player ep, Jetpack jetpack) {
        Level level = ep.level();
        ItemStack left = switch (jetpack.kind()) {
            case PLAIN -> ItemStack.EMPTY;
            case STEEL -> new ItemStack(ToolRegistry.STEEL_CHESTPLATE.get());
            case BEDROCK -> Forced.stackOf(ToolRegistry.BEDROCK_CHESTPLATE.get(), level.registryAccess(), BedrockTools.CHESTPLATE);
        };
        ep.setItemSlot(EquipmentSlot.CHEST, left);
        level.explode(ep, ep.getX(), ep.getY(), ep.getZ(), 2, Level.ExplosionInteraction.MOB);
        double angle = Math.toRadians(level.random.nextDouble() * 360);
        ep.push(4 * Math.cos(angle), 1.25, 4 * Math.sin(angle));
        ep.hurtMarked = true;
    }
}
