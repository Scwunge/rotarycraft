package net.scwunge.rotarycraft.client.farm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.client.machine.MachineRenderer;
import net.scwunge.rotarycraft.client.machine.MachineRenderer.Look;
import net.scwunge.rotarycraft.farm.AutoBreederBlockEntity;
import net.scwunge.rotarycraft.farm.BaitBoxBlockEntity;
import net.scwunge.rotarycraft.farm.DefoliatorBlockEntity;
import net.scwunge.rotarycraft.farm.FanBlockEntity;
import net.scwunge.rotarycraft.farm.FertilizerBlockEntity;
import net.scwunge.rotarycraft.farm.GroundHydratorBlockEntity;
import net.scwunge.rotarycraft.farm.LawnSprinklerBlockEntity;
import net.scwunge.rotarycraft.farm.MobHarvesterBlockEntity;
import net.scwunge.rotarycraft.farm.SpawnerControllerBlockEntity;
import net.scwunge.rotarycraft.farm.SprinklerBlockEntity;
import net.scwunge.rotarycraft.farm.VacuumBlockEntity;
import net.scwunge.rotarycraft.farm.WoodcutterBlockEntity;
import net.scwunge.rotarycraft.registry.FarmRegistry;

/** Renderers, item renderers and screens of the farming and automation machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FarmClient {
    private FarmClient() {}

    /** The turn of a spinning part: degrees a tick, from the speed of the shaft (the original's formula). */
    private static double spin(int omega, double scale) {
        return scale * Math.pow(Math.log(omega + 1) / Math.log(2), 1.05);
    }

    static final Look<FanBlockEntity> FAN = Look.<FanBlockEntity>spinning("fan", "fan", MachineRenderer.FAN,
            be -> be.getPower() < FanBlockEntity.REQUIREMENT.minPower() ? 0 : spin(be.getOmega(), 3), -1).textured(be -> be.isWide() ? "fan_wide" : "fan");
    static final Look<SprinklerBlockEntity> SPRINKLER = Look.still("sprinkler", "sprinkler", MachineRenderer.FAN);
    static final Look<LawnSprinklerBlockEntity> LAWN_SPRINKLER = Look.<LawnSprinklerBlockEntity>spinning("lawn_sprinkler", "lawn_sprinkler", null,
            be -> be.isWorking() ? 24 : 0, 1);
    static final Look<GroundHydratorBlockEntity> HYDRATOR = Look.still("reservoir", "ground_hydrator", null);
    static final Look<FertilizerBlockEntity> FERTILIZER = Look.<FertilizerBlockEntity>spinning("fertilizer", "fertilizer", null,
            be -> be.getPower() < FertilizerBlockEntity.REQUIREMENT.minPower() ? 0 : spin(be.getOmega(), 1), 1);
    static final Look<DefoliatorBlockEntity> DEFOLIATOR = Look.<DefoliatorBlockEntity>spinning("defoliator", "defoliator", null,
            be -> be.getPower() < DefoliatorBlockEntity.REQUIREMENT.minPower() ? 0 : spin(be.getOmega(), 1), -1);
    static final Look<WoodcutterBlockEntity> WOODCUTTER = Look.<WoodcutterBlockEntity>spinning("woodcutter", "woodcutter", MachineRenderer.BEAM,
            be -> be.getPower() < WoodcutterBlockEntity.REQUIREMENT.minPower() ? 0 : spin(be.getOmega(), 1), 1);
    static final Look<VacuumBlockEntity> VACUUM = Look.still("vacuum", "vacuum", null);
    static final Look<AutoBreederBlockEntity> AUTO_BREEDER = Look.<AutoBreederBlockEntity>still("auto_breeder", "auto_breeder", null)
            .textured(be -> be.hasWheat() ? "auto_breeder" : "auto_breeder_empty").withFlags(AutoBreederBlockEntity::feedFlags);
    static final Look<BaitBoxBlockEntity> BAIT_BOX = Look.still("bait_box", "bait_box", null);
    static final Look<MobHarvesterBlockEntity> MOB_HARVESTER = Look.still("mob_harvester", "mob_harvester", null);
    static final Look<SpawnerControllerBlockEntity> SPAWNER_CONTROLLER = Look.<SpawnerControllerBlockEntity>still("spawner_controller", "spawner_controller", null)
            .withFlags(be -> new boolean[] {be.isValidClient()});

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FarmRegistry.FAN_BE.get(), c -> new MachineRenderer<>(FAN));
        event.registerBlockEntityRenderer(FarmRegistry.SPRINKLER_BE.get(), c -> new MachineRenderer<>(SPRINKLER));
        event.registerBlockEntityRenderer(FarmRegistry.LAWN_SPRINKLER_BE.get(), c -> new MachineRenderer<>(LAWN_SPRINKLER));
        event.registerBlockEntityRenderer(FarmRegistry.GROUND_HYDRATOR_BE.get(), c -> new MachineRenderer<>(HYDRATOR));
        event.registerBlockEntityRenderer(FarmRegistry.FERTILIZER_BE.get(), c -> new MachineRenderer<>(FERTILIZER));
        event.registerBlockEntityRenderer(FarmRegistry.DEFOLIATOR_BE.get(), c -> new MachineRenderer<>(DEFOLIATOR));
        event.registerBlockEntityRenderer(FarmRegistry.WOODCUTTER_BE.get(), c -> new MachineRenderer<>(WOODCUTTER));
        event.registerBlockEntityRenderer(FarmRegistry.VACUUM_BE.get(), c -> new MachineRenderer<>(VACUUM));
        event.registerBlockEntityRenderer(FarmRegistry.AUTO_BREEDER_BE.get(), c -> new MachineRenderer<>(AUTO_BREEDER));
        event.registerBlockEntityRenderer(FarmRegistry.BAIT_BOX_BE.get(), c -> new MachineRenderer<>(BAIT_BOX));
        event.registerBlockEntityRenderer(FarmRegistry.MOB_HARVESTER_BE.get(), c -> new MachineRenderer<>(MOB_HARVESTER));
        event.registerBlockEntityRenderer(FarmRegistry.SPAWNER_CONTROLLER_BE.get(), c -> new MachineRenderer<>(SPAWNER_CONTROLLER));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(FarmRegistry.FARM_MENU.get(), FarmScreen::new);
    }

    /** Registers the item form of a machine drawn by a {@link MachineRenderer}. */
    public static <T extends PowerBlockEntity> void item(RegisterClientExtensionsEvent event, DeferredBlock<? extends Block> block, Look<T> look, String model,
                                                         String texture, boolean... flags) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new MachineRenderer<>(look), model, texture, flags);
                }
                return renderer;
            }
        }, block.get().asItem());
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        item(event, FarmRegistry.FAN, FAN, "fan", "fan");
        item(event, FarmRegistry.SPRINKLER, SPRINKLER, "sprinkler", "sprinkler");
        item(event, FarmRegistry.LAWN_SPRINKLER, LAWN_SPRINKLER, "lawn_sprinkler", "lawn_sprinkler");
        item(event, FarmRegistry.GROUND_HYDRATOR, HYDRATOR, "reservoir", "ground_hydrator");
        item(event, FarmRegistry.FERTILIZER, FERTILIZER, "fertilizer", "fertilizer");
        item(event, FarmRegistry.DEFOLIATOR, DEFOLIATOR, "defoliator", "defoliator");
        item(event, FarmRegistry.WOODCUTTER, WOODCUTTER, "woodcutter", "woodcutter");
        item(event, FarmRegistry.VACUUM, VACUUM, "vacuum", "vacuum");
        item(event, FarmRegistry.AUTO_BREEDER, AUTO_BREEDER, "auto_breeder", "auto_breeder_empty", true, true, true, true, true);
        item(event, FarmRegistry.BAIT_BOX, BAIT_BOX, "bait_box", "bait_box");
        item(event, FarmRegistry.MOB_HARVESTER, MOB_HARVESTER, "mob_harvester", "mob_harvester");
        item(event, FarmRegistry.SPAWNER_CONTROLLER, SPAWNER_CONTROLLER, "spawner_controller", "spawner_controller", true);
    }
}
