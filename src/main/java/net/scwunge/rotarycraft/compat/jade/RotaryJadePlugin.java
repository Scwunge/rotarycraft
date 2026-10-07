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
            if (be instanceof FarmBlockEntity farm) {
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
                case "pipePump" -> tooltip.add(Component.translatable("jade.rotarycraft.pump", s[0]));
                case "converter" -> tooltip.add(Component.translatable("jade.rotarycraft.stored", s[0], s[1]));
                case "dynamo" -> tooltip.add(Component.translatable("jade.rotarycraft.generated", s[0]));
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
