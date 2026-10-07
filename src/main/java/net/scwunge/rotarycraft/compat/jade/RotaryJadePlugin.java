package net.scwunge.rotarycraft.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.EnergyConverterBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltips for RotaryCraft's machines: the torque, speed and power a machine is turned with (or gives), and what the process machines hold and are doing
 * (tanks, temperature, progress). The server sends the numbers; the machines already keep them in the figures their own screens show.
 */
@WailaPlugin
public class RotaryJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = RotaryCraft.id("machine");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, PowerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, Block.class);
        com.mojang.logging.LogUtils.getLogger().info("RotaryCraft: Jade tooltips registered");
    }

    /** What the server tells the tooltip. */
    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (!(be instanceof PowerBlockEntity power)) {
                return;
            }
            data.putInt("torque", power.getTorque());
            data.putInt("omega", power.getOmega());
            if (be instanceof net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity inventory && !inventory.tanks().isEmpty()) {
                net.minecraft.nbt.ListTag tanks = new net.minecraft.nbt.ListTag();
                for (var tank : inventory.tanks()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString("fluid", tank.isEmpty() ? "" : tank.getFluid().getHoverName().getString());
                    entry.putInt("amount", tank.getFluidAmount());
                    entry.putInt("capacity", tank.getCapacity());
                    tanks.add(entry);
                }
                data.put("tanks", tanks);
            }
            if (be instanceof net.scwunge.rotarycraft.logistics.ScaleChestBlockEntity chest) {
                data.putString("kind", "scaleChest");
                data.putIntArray("status", new int[] {chest.numberSlots(), chest.numberPages(), chest.powerChanges()});
            } else if (be instanceof net.scwunge.rotarycraft.logistics.DropProcessorBlockEntity drops) {
                data.putString("kind", "dropProcessor");
                data.putIntArray("status", new int[] {drops.overflowCount()});
            } else if (be instanceof FarmBlockEntity farm) {
                data.putString("kind", farm.kind());
                int[] status = new int[FarmBlockEntity.DATA_COUNT - 2];
                var values = farm.data();
                for (int i = 0; i < status.length; i++) {
                    status[i] = values.get(2 + i);
                }
                data.putIntArray("status", status);
            } else if (be instanceof EnergyConverterBlockEntity converter) {
                data.putString("kind", "converter");
                data.putIntArray("status", new int[] {converter.stored(), converter.maxStorage()});
            } else if (be instanceof DynamoBlockEntity dynamo) {
                data.putString("kind", "dynamo");
                data.putIntArray("status", new int[] {dynamo.generated()});
            } else if (be instanceof net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity gear) {
                data.putString("kind", "advancedGear");
                data.putString("gear", gear.kind().name());
                data.putInt("ratio", gear.effectiveRatio());
                data.putLong("energy", gear.energy());
                data.putLong("capacity", gear.capacity());
                data.putBoolean("releasing", gear.isReleasing());
            } else if (be instanceof net.scwunge.rotarycraft.transmission.BeltHubBlockEntity belt) {
                data.putString("kind", "belt");
                data.putIntArray("status", new int[] {belt.hasValidConnection() ? 1 : 0, belt.isReceivingEnd() ? 1 : 0, belt.isSlipping() ? 1 : 0, belt.isWet() ? 1 : 0});
            } else if (be instanceof net.scwunge.rotarycraft.transmission.EngineControllerBlockEntity ecu) {
                data.putString("kind", "ecu");
                data.putString("setting", ecu.setting().name().toLowerCase(java.util.Locale.ROOT));
                data.putBoolean("redstone", ecu.redstoneMode());
            } else if (be instanceof net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity clutch) {
                data.putString("kind", "distribution");
                data.putString("control", clutch.control().name().toLowerCase(java.util.Locale.ROOT));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    /** What the tooltip shows. */
    enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("torque")) {
                return;
            }
            int torque = data.getInt("torque"), omega = data.getInt("omega");
            if (torque > 0 || omega > 0) {
                tooltip.add(Component.translatable("jade.rotarycraft.power", String.format("%,d", (long) torque * omega), String.format("%,d", torque), String.format("%,d", omega)));
            } else {
                tooltip.add(Component.translatable("jade.rotarycraft.idle"));
            }
            for (net.minecraft.nbt.Tag t : data.getList("tanks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                CompoundTag tank = (CompoundTag) t;
                tooltip.add(Component.translatable("jade.rotarycraft.tank.contents", tank.getString("fluid").isEmpty() ? "-" : tank.getString("fluid"), tank.getInt("amount"), tank.getInt("capacity")));
            }
            int[] s = data.getIntArray("status");
            switch (data.getString("kind")) {
                case "boiler" -> {
                    tooltip.add(Component.translatable("jade.rotarycraft.boiler.water", s[0], s[3]));
                    tooltip.add(Component.translatable("jade.rotarycraft.boiler.steam", s[1], s[3]));
                    tooltip.add(Component.translatable("jade.rotarycraft.temperature", s[2]));
                }
                case "airCompressor" -> tooltip.add(Component.translatable("jade.rotarycraft.tank.air", s[0], s[1]));
                case "gasTank" -> tooltip.add(Component.translatable("jade.rotarycraft.tank.held", s[0], s[1]));
                case "distiller", "fuelEnhancer" -> {
                    tooltip.add(Component.translatable("jade.rotarycraft.tank.input", s[0], s[2]));
                    tooltip.add(Component.translatable("jade.rotarycraft.tank.output", s[1], s[2]));
                }
                case "bigFurnace" -> {
                    tooltip.add(Component.translatable("jade.rotarycraft.temperature", s[0]));
                    tooltip.add(Component.translatable("jade.rotarycraft.tank.lava", s[1], s[2]));
                    tooltip.add(Component.translatable("jade.rotarycraft.progress", s[3] * 100 / Math.max(1, s[4])));
                }
                case "scaleChest" -> tooltip.add(Component.translatable(s[2] > 0 ? "jade.rotarycraft.chest.unstable" : "jade.rotarycraft.chest.slots", s[0], s[1]));
                case "dropProcessor" -> {
                    if (s[0] > 0) {
                        tooltip.add(Component.translatable("jade.rotarycraft.drops.waiting", s[0]));
                    }
                }
                case "pipePump" -> tooltip.add(Component.translatable("jade.rotarycraft.pump", s[0]));
                case "converter" -> tooltip.add(Component.translatable("jade.rotarycraft.stored", s[0], s[1]));
                case "dynamo" -> tooltip.add(Component.translatable("jade.rotarycraft.generated", s[0]));
                case "advancedGear" -> {
                    switch (data.getString("gear")) {
                        case "CVT" -> tooltip.add(Component.translatable(data.getInt("ratio") >= 0 ? "jade.rotarycraft.cvt.torque" : "jade.rotarycraft.cvt.speed", Math.abs(data.getInt("ratio"))));
                        case "COIL", "BEDROCK_COIL" -> {
                            tooltip.add(Component.translatable("jade.rotarycraft.coil.energy", String.format("%,d", data.getLong("energy")), String.format("%,d", data.getLong("capacity"))));
                            tooltip.add(Component.translatable(data.getBoolean("releasing") ? "jade.rotarycraft.coil.releasing" : "jade.rotarycraft.coil.holding"));
                        }
                        default -> {
                        }
                    }
                }
                case "belt" -> {
                    tooltip.add(Component.translatable(s[0] == 1 ? (s[1] == 1 ? "jade.rotarycraft.belt.receiving" : "jade.rotarycraft.belt.driving") : "jade.rotarycraft.belt.unjoined"));
                    if (s[2] == 1) {
                        tooltip.add(Component.translatable("jade.rotarycraft.belt.slipping"));
                    }
                    if (s[3] == 1) {
                        tooltip.add(Component.translatable("jade.rotarycraft.belt.wet"));
                    }
                }
                case "ecu" -> tooltip.add(Component.translatable("jade.rotarycraft.ecu", Component.translatable("message.rotarycraft.ecu.setting." + data.getString("setting")),
                        Component.translatable(data.getBoolean("redstone") ? "jade.rotarycraft.ecu.redstone" : "jade.rotarycraft.ecu.manual")));
                case "distribution" -> tooltip.add(Component.translatable("jade.rotarycraft.distribution", Component.translatable("gui.rotarycraft.distribution_clutch." + data.getString("control"))));
                default -> {
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
