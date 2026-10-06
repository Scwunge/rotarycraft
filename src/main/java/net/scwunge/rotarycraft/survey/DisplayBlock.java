package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Display's block: a dye colours the board, glowstone dust gives back the argon blue, and a book (signed, or a book and quill)
 * sets what it says, its pages run together. Anything else opens the coil's slot.
 */
public class DisplayBlock extends HorizontalSurveyBlock {
    public DisplayBlock(Properties props, Supplier<? extends BlockEntityType<? extends SurveyBlockEntity>> type,
                        BlockEntityType.BlockEntitySupplier<? extends SurveyBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DisplayBlockEntity display)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide()) {
                display.setDye(dye.getDyeColor());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (!level.isClientSide()) {
                display.setDye(null);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        List<String> pages = pagesOf(stack);
        if (pages != null) {
            if (!level.isClientSide()) {
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < pages.size(); i++) {
                    text.append(pages.get(i));
                    if (i < pages.size() - 1 && !pages.get(i).endsWith(" ")) {
                        text.append(' ');
                    }
                }
                display.setMessage(text.toString());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** The text of each page of a book, or null if the item is not one. */
    public static List<String> pagesOf(ItemStack stack) {
        WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            List<String> out = new ArrayList<>();
            written.pages().forEach(page -> out.add(page.get(false).getString()));
            return out;
        }
        WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writable != null) {
            List<String> out = new ArrayList<>();
            writable.pages().forEach(page -> out.add(page.get(false)));
            return out;
        }
        return null;
    }
}
