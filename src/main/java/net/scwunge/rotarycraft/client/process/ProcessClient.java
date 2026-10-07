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
import net.scwunge.rotarycraft.process.BigFurnaceBlockEntity;
import net.scwunge.rotarycraft.process.DistillerBlockEntity;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.FuelEnhancerBlockEntity;
import net.scwunge.rotarycraft.client.machine.ModelMachineRenderer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.process.GasTankBlockEntity;
import net.scwunge.rotarycraft.process.PipePumpBlockEntity;
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

    static final Look<GasTankBlockEntity> GAS_TANK = Look.still("gas_tank", "gas_tank", null);
    /** Laid along the pipe's axis as the original laid it: a quarter turn about the vertical for the north-south run, about the length for an upright one. */
    static final Look<PipePumpBlockEntity> PIPE_PUMP = Look.<PipePumpBlockEntity>still("pipe_pump", "pipe_pump", null).oriented((pose, facing) -> {
        switch (facing) {
            case WEST, EAST -> { }
            case NORTH, SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> {
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.translate(1, -1, 0);
            }
            case DOWN -> {
                pose.mulPose(Axis.ZP.rotationDegrees(270));
                pose.translate(-1, -1, 0);
            }
        }
    });

    /** The sheet of fluid at the level of a tank, over the model's floor at {@code base}, as the original filled its glass (a third of the block, a little less). */
    private static void level(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, FluidTank tank, int capacity, double base) {
        if (tank.getFluidAmount() > 0) {
            Fluid fluid = tank.getFluid().getFluid();
            ModelMachineRenderer.fluidSurface(pose, buffers, fluid, base + 0.001 + 0.95 / 3 * tank.getFluidAmount() / capacity, 0.02, light);
        }
    }

    static final Look<DistillerBlockEntity> DISTILLER = Look.<DistillerBlockEntity>still("distiller", "distiller", null).extra((be, pose, buffers, light) -> {
        level(pose, buffers, light, be.input(), DistillerBlockEntity.CAPACITY, 1 / 16D);
        level(pose, buffers, light, be.output(), DistillerBlockEntity.CAPACITY, 10 / 16D);
    });
    static final Look<FuelEnhancerBlockEntity> FUEL_ENHANCER = Look.<FuelEnhancerBlockEntity>spinning("fuel_enhancer", "fuel_enhancer", null, be -> spin(be.getOmega(), 2), -1)
            .extra((be, pose, buffers, light) -> {
                level(pose, buffers, light, be.input(), FuelEnhancerBlockEntity.CAPACITY, 10 / 16D);
                level(pose, buffers, light, be.output(), FuelEnhancerBlockEntity.CAPACITY, 1 / 16D);
            });
    static final Look<BigFurnaceBlockEntity> BIG_FURNACE = Look.<BigFurnaceBlockEntity>still("big_furnace", "big_furnace", null).extra((be, pose, buffers, light) -> {
        if (be.lava().getFluidAmount() > 0) {
            ModelMachineRenderer.fluidSurface(pose, buffers, be.lava().getFluid().getFluid(), 0.0625 + 14 / 16D * be.lava().getFluidAmount() / BigFurnaceBlockEntity.CAPACITY, 0.0625, light);
        }
    });

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ProcessRegistry.BOILER_BE.get(), c -> new MachineRenderer<>(BOILER));
        event.registerBlockEntityRenderer(ProcessRegistry.STEAM_TURBINE_BE.get(), c -> new MachineRenderer<>(STEAM_TURBINE));
        event.registerBlockEntityRenderer(ProcessRegistry.AIR_COMPRESSOR_BE.get(), c -> new MachineRenderer<>(AIR_COMPRESSOR));
        event.registerBlockEntityRenderer(ProcessRegistry.PNEUMATIC_ENGINE_BE.get(), c -> new MachineRenderer<>(PNEUMATIC_ENGINE));
        event.registerBlockEntityRenderer(ProcessRegistry.MAGNETIC_MOTOR_BE.get(), c -> new MachineRenderer<>(MAGNETIC_MOTOR));
        event.registerBlockEntityRenderer(ProcessRegistry.DYNAMO_BE.get(), c -> new MachineRenderer<>(DYNAMO));
        event.registerBlockEntityRenderer(ProcessRegistry.GAS_TANK_BE.get(), c -> new MachineRenderer<>(GAS_TANK));
        event.registerBlockEntityRenderer(ProcessRegistry.DISTILLER_BE.get(), c -> new MachineRenderer<>(DISTILLER));
        event.registerBlockEntityRenderer(ProcessRegistry.FUEL_ENHANCER_BE.get(), c -> new MachineRenderer<>(FUEL_ENHANCER));
        event.registerBlockEntityRenderer(ProcessRegistry.BIG_FURNACE_BE.get(), c -> new MachineRenderer<>(BIG_FURNACE));
        event.registerBlockEntityRenderer(ProcessRegistry.PIPE_PUMP_BE.get(), c -> new MachineRenderer<>(PIPE_PUMP));
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        FarmClient.item(event, ProcessRegistry.BOILER, BOILER, "boiler", "boiler");
        FarmClient.item(event, ProcessRegistry.STEAM_TURBINE, STEAM_TURBINE, "steam_turbine", "steam_turbine");
        FarmClient.item(event, ProcessRegistry.AIR_COMPRESSOR, AIR_COMPRESSOR, "air_compressor", "air_compressor");
        FarmClient.item(event, ProcessRegistry.PNEUMATIC_ENGINE, PNEUMATIC_ENGINE, "pneumatic_engine", "pneumatic_engine");
        FarmClient.item(event, ProcessRegistry.MAGNETIC_MOTOR, MAGNETIC_MOTOR, "magnetic_motor", "magnetic_motor");
        FarmClient.item(event, ProcessRegistry.DYNAMO, DYNAMO, "dynamo", "dynamo");
        FarmClient.item(event, ProcessRegistry.GAS_TANK, GAS_TANK, "gas_tank", "gas_tank");
        FarmClient.item(event, ProcessRegistry.DISTILLER, DISTILLER, "distiller", "distiller");
        FarmClient.item(event, ProcessRegistry.FUEL_ENHANCER, FUEL_ENHANCER, "fuel_enhancer", "fuel_enhancer");
        FarmClient.item(event, ProcessRegistry.BIG_FURNACE, BIG_FURNACE, "big_furnace", "big_furnace");
        FarmClient.item(event, ProcessRegistry.PIPE_PUMP, PIPE_PUMP, "pipe_pump", "pipe_pump");
    }
}
