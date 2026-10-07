package net.scwunge.rotarycraft.registry;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.UUIDUtil;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.UUID;

public class RotaryAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RotaryCraft.MOD_ID);

    /** Who placed a machine (the ownerOnlyMachines option keeps everyone else out of its screen). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> PLACER = ATTACHMENTS.register("placer",
            () -> AttachmentType.builder(() -> new UUID(0, 0)).serialize(UUIDUtil.CODEC).build());

    private RotaryAttachments() {
    }
}
