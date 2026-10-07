package net.scwunge.rotarycraft.client.process;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.farm.FarmClient;
import net.scwunge.rotarycraft.client.machine.MachineRenderer;
import net.scwunge.rotarycraft.client.machine.MachineRenderer.Look;
import net.scwunge.rotarycraft.process.AirCompressorBlockEntity;
import net.scwunge.rotarycraft.process.BoilerBlockEntity;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.MagneticMotorBlockEntity;
import net.scwunge.rotarycraft.process.PneumaticEngineBlockEntity;
import net.scwunge.rotarycraft.process.SteamTurbineBlockEntity;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/** Renderers and item renderers of the fluid and process machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ProcessClient {
    private ProcessClient() {}

    private static double spin(int omega, double base) {
        return omega <= 0 ? 0 : Math.pow(Math.log(omega + 1) / Math.log(base), 1.05);
    }

    /**
     * How the original stood its upright cylinders (the Compressor and the Dynamo) for each way they face: upright, or upside down and
     * a block lower, or laid on their sides and turned by {@code side} degrees for the way they point.
     */
    private static Look.Orient cylinder(Direction upright, int south, int east, int north, int west) {
        return (pose, facing) -> {
            if (facing.getAxis().isVertical()) {
                if (facing != upright) {
                    pose.mulPose(Axis.ZP.rotationDegrees(180));
                    pose.translate(0, -2, 0);
                }
                return;
            }
            pose.mulPose(Axis.XP.rotationDegrees(90));
            int turn = switch (facing) {
                case SOUTH -> south;
                case EAST -> east;
                case NORTH -> north;
                default -> west;
            };
            pose.mulPose(Axis.ZP.rotationDegrees(turn));
            pose.translate(0, -1, -1);
        };
    }

    static final Look<BoilerBlockEntity> BOILER = Look.<BoilerBlockEntity>spinning("boiler", "boiler", null, be -> spin(be.getOmega(), 2), -1);
    static final Look<SteamTurbineBlockEntity> STEAM_TURBINE = Look.<SteamTurbineBlockEntity>spinning("steam_turbine", "steam_turbine",
            new float[] {270, 90, 0, 180}, be -> spin(be.getOmega(), 4), -1);
    static final Look<AirCompressorBlockEntity> AIR_COMPRESSOR = Look.<AirCompressorBlockEntity>spinning("air_compressor", "air_compressor", null, be -> spin(be.getOmega(), 2), -1)
            .oriented(cylinder(Direction.UP, 0, 90, 180, 270));
    static final Look<PneumaticEngineBlockEntity> PNEUMATIC_ENGINE = Look.<PneumaticEngineBlockEntity>spinning("pneumatic_engine", "pneumatic_engine",
            MachineRenderer.ENGINE, be -> spin(be.getOmega(), 2), -1);
    static final Look<MagneticMotorBlockEntity> MAGNETIC_MOTOR = Look.<MagneticMotorBlockEntity>spinning("magnetic_motor", "magnetic_motor",
            new float[] {90, 270, 180, 0}, be -> spin(be.getOmega(), 2), -1);
    static final Look<DynamoBlockEntity> DYNAMO = Look.<DynamoBlockEntity>spinning("dynamo", "dynamo", null, be -> spin(be.getOmega(), 2), -1)
            .oriented(cylinder(Direction.DOWN, 180, 270, 0, 90)).textured(be -> be.getOmega() > 0 ? "dynamo_running" : "dynamo");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ProcessRegistry.BOILER_BE.get(), c -> new MachineRenderer<>(BOILER));
        event.registerBlockEntityRenderer(ProcessRegistry.STEAM_TURBINE_BE.get(), c -> new MachineRenderer<>(STEAM_TURBINE));
        event.registerBlockEntityRenderer(ProcessRegistry.AIR_COMPRESSOR_BE.get(), c -> new MachineRenderer<>(AIR_COMPRESSOR));
        event.registerBlockEntityRenderer(ProcessRegistry.PNEUMATIC_ENGINE_BE.get(), c -> new MachineRenderer<>(PNEUMATIC_ENGINE));
        event.registerBlockEntityRenderer(ProcessRegistry.MAGNETIC_MOTOR_BE.get(), c -> new MachineRenderer<>(MAGNETIC_MOTOR));
        event.registerBlockEntityRenderer(ProcessRegistry.DYNAMO_BE.get(), c -> new MachineRenderer<>(DYNAMO));
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        FarmClient.item(event, ProcessRegistry.BOILER, BOILER, "boiler", "boiler");
        FarmClient.item(event, ProcessRegistry.STEAM_TURBINE, STEAM_TURBINE, "steam_turbine", "steam_turbine");
        FarmClient.item(event, ProcessRegistry.AIR_COMPRESSOR, AIR_COMPRESSOR, "air_compressor", "air_compressor");
        FarmClient.item(event, ProcessRegistry.PNEUMATIC_ENGINE, PNEUMATIC_ENGINE, "pneumatic_engine", "pneumatic_engine");
        FarmClient.item(event, ProcessRegistry.MAGNETIC_MOTOR, MAGNETIC_MOTOR, "magnetic_motor", "magnetic_motor");
        FarmClient.item(event, ProcessRegistry.DYNAMO, DYNAMO, "dynamo", "dynamo");
    }
}
