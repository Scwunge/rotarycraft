package net.scwunge.rotarycraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.process.BigFurnaceBlockEntity;
import net.scwunge.rotarycraft.process.BoilerBlockEntity;
import net.scwunge.rotarycraft.process.DistillerBlockEntity;
import net.scwunge.rotarycraft.process.FuelEnhancerBlockEntity;
import net.scwunge.rotarycraft.recipe.BlastCraftingRecipe;
import net.scwunge.rotarycraft.recipe.BlastFurnaceRecipe;
import net.scwunge.rotarycraft.recipe.CentrifugeRecipe;
import net.scwunge.rotarycraft.recipe.CompactingRecipe;
import net.scwunge.rotarycraft.recipe.CompostingRecipe;
import net.scwunge.rotarycraft.recipe.CrystallizingRecipe;
import net.scwunge.rotarycraft.recipe.DryingRecipe;
import net.scwunge.rotarycraft.recipe.ExtractionRecipe;
import net.scwunge.rotarycraft.recipe.FrictionHeatingRecipe;
import net.scwunge.rotarycraft.recipe.GrindingRecipe;
import net.scwunge.rotarycraft.recipe.MeltingRecipe;
import net.scwunge.rotarycraft.recipe.PulseSmeltingRecipe;
import net.scwunge.rotarycraft.recipe.WorktableRecipe;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JEI pages for RotaryCraft's machines: one category for each kind of recipe the mod adds (grinding, extraction, the blast furnace, and the rest),
 * and pages for the machines whose conversions are in their code (the Boiler, Distiller and Fuel Enhancer). Every page shows what goes in, what
 * comes out, and the temperature, chance or speed that matters, with the machine as its catalyst.
 */
@JeiPlugin
public class RotaryJeiPlugin implements IModPlugin {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final ResourceLocation UID = RotaryCraft.id("jei");

    /** One page: items and fluids in, items and fluids out, and a line or two of notes. */
    record Page(List<List<ItemStack>> itemsIn, List<List<FluidStack>> fluidsIn, List<ItemStack> itemsOut, List<FluidStack> fluidsOut, List<String> notes) {
    }

    /** A kind of page: its recipe type, the machine that does it, and what to call it. */
    private record Kind(String id, String title, String machine, RecipeType<Page> type) {
        Kind(String id, String title, String machine) {
            this(id, title, machine, RecipeType.create(RotaryCraft.MOD_ID, id, Page.class));
        }
    }

    private static final List<Kind> RECIPES = List.of(
            new Kind("grinding", "Grinder", "grinder"), new Kind("extraction", "Extractor", "extractor"), new Kind("blast_furnace", "Blast Furnace", "blast_furnace"),
            new Kind("blast_crafting", "Blast Furnace Crafting", "blast_furnace"), new Kind("friction_heating", "Friction Heater", "friction_heater"),
            new Kind("compacting", "Compactor", "compactor"), new Kind("centrifuge", "Centrifuge", "centrifuge"), new Kind("crystallizing", "Crystallizer", "crystallizer"),
            new Kind("composting", "Composter", "composter"), new Kind("drying", "Dryer", "dryer"), new Kind("pulse_smelting", "Pulse Furnace", "pulse_furnace"),
            new Kind("melting", "Rock Melter", "rock_melter"), new Kind("worktable", "Worktable", "worktable"));
    private static final Kind BOILER = new Kind("boiler", "Boiler", "boiler");
    private static final Kind DISTILLER = new Kind("distiller", "Distiller", "distiller");
    private static final Kind ENHANCER = new Kind("fuel_enhancer", "Fuel Enhancer", "fuel_enhancer");

    private static List<Kind> kinds() {
        List<Kind> all = new ArrayList<>(RECIPES);
        all.addAll(List.of(BOILER, DISTILLER, ENHANCER));
        return all;
    }

    private static ItemLike block(String name) {
        Block b = BuiltInRegistries.BLOCK.get(RotaryCraft.id(name));
        return b;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Kind k : kinds()) {
            registration.addRecipeCategories(new PageCategory(gui, k.type(), k.title(), block(k.machine())));
        }
        LOGGER.info("RotaryCraft: registered {} JEI categories", kinds().size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Kind k : kinds()) {
            registration.addRecipeCatalyst(new ItemStack(block(k.machine())), k.type());
        }
        registration.addRecipeCatalyst(new ItemStack(block("big_furnace")), RecipeTypes.SMELTING);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var manager = Minecraft.getInstance().level.getRecipeManager();
        for (Kind k : RECIPES) {
            var type = BuiltInRegistries.RECIPE_TYPE.get(RotaryCraft.id(k.id()));
            if (type == null) {
                continue;
            }
            List<Page> pages = new ArrayList<>();
            for (RecipeHolder<?> holder : manager.getAllRecipesFor(castType(type))) {
                Page page = page(holder.value());
                if (page != null) {
                    pages.add(page);
                }
            }
            registration.addRecipes(k.type(), pages);
        }
        registration.addRecipes(BOILER.type(), List.of(new Page(List.of(), List.of(List.of(new FluidStack(Fluids.WATER, 1))), List.of(),
                List.of(new FluidStack(ProcessRegistry.steam(), BoilerBlockEntity.STEAM_PER_WATER)),
                List.of("Needs shaft power; the boiler warms to over 100 C first", BoilerBlockEntity.ENERGY_PER_MB + " energy for each mB of water",
                        "Explodes past " + BoilerBlockEntity.MAX_TEMPERATURE + " C"))));
        registration.addRecipes(DISTILLER.type(), distillations());
        registration.addRecipes(ENHANCER.type(), enhancements());
        LOGGER.info("RotaryCraft: JEI pages added");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Recipe<?>> net.minecraft.world.item.crafting.RecipeType<T> castType(net.minecraft.world.item.crafting.RecipeType<?> type) {
        return (net.minecraft.world.item.crafting.RecipeType<T>) type;
    }

    private static List<ItemStack> stacks(Ingredient ingredient) {
        return Arrays.asList(ingredient.getItems());
    }

    private static List<FluidStack> fluidsOf(TagKey<Fluid> tag, int amount) {
        List<FluidStack> list = new ArrayList<>();
        BuiltInRegistries.FLUID.getTagOrEmpty(tag).forEach(h -> {
            if (h.value().isSource(h.value().defaultFluidState())) {
                list.add(new FluidStack(h.value(), amount));
            }
        });
        return list;
    }

    private static List<Page> distillations() {
        List<Page> pages = new ArrayList<>();
        for (DistillerBlockEntity.Conversion c : DistillerBlockEntity.CONVERSIONS) {
            pages.add(new Page(List.of(), List.of(fluidsOf(c.input(), c.consumed())), List.of(), List.of(new FluidStack(c.output().get(), c.produced())),
                    List.of("Any fluid in the " + c.input().location() + " tag", "Every " + DistillerBlockEntity.PERIOD + " ticks, at " + c.minTorque() + " N*m and " + c.minPower() + " W")));
        }
        return pages;
    }

    private static List<Page> enhancements() {
        List<Page> pages = new ArrayList<>();
        List<List<ItemStack>> items = new ArrayList<>();
        for (ItemStack i : FuelEnhancerBlockEntity.ingredients()) {
            items.add(List.of(i));
        }
        for (FuelEnhancerBlockEntity.Conversion c : FuelEnhancerBlockEntity.CONVERSIONS) {
            pages.add(new Page(items, List.of(fluidsOf(c.input(), c.ratio())), List.of(), List.of(new FluidStack(RotaryFluids.JET_FUEL.get(), 1)),
                    List.of("Any fluid in the " + c.input().location() + " tag, " + c.ratio() + " mB for each mB", "All five items must be in it; they are used up slowly")));
        }
        return pages;
    }

    private static Page page(Recipe<?> recipe) {
        if (recipe instanceof GrindingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(r.result()), List.of(), List.of());
        } else if (recipe instanceof ExtractionRecipe r) {
            List<ItemStack> out = r.bonus().isEmpty() ? List.of() : List.of(r.bonus());
            return new Page(List.of(stacks(r.ore())), List.of(), out, List.of(), List.of("Four stages: dust, slurry, solution, flakes",
                    "Doubles a stage " + (int) (r.doublingChance() * 100) + "% of the time" + (out.isEmpty() ? "" : "; bonus " + (int) (r.bonusChance() * 100) + "%")));
        } else if (recipe instanceof BlastFurnaceRecipe r) {
            List<List<ItemStack>> in = new ArrayList<>();
            in.add(stacks(r.main()));
            for (var additive : r.additives()) {
                additive.ifPresent(a -> in.add(stacks(a.ingredient())));
            }
            return new Page(in, List.of(), List.of(r.result()), List.of(), List.of("Needs " + r.temperature() + " C", r.mainPerResult() + " of the first for each made"));
        } else if (recipe instanceof BlastCraftingRecipe r) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (Ingredient i : r.pattern().ingredients()) {
                if (!i.isEmpty()) {
                    in.add(stacks(i));
                }
            }
            return new Page(in, List.of(), List.of(r.result()), List.of(), List.of("Crafted in the blast furnace at " + r.temperature() + " C"));
        } else if (recipe instanceof FrictionHeatingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(r.result()), List.of(), List.of("Needs " + r.temperature() + " C", r.duration() + " ticks"));
        } else if (recipe instanceof CompactingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(r.result()), List.of(),
                    List.of("Needs " + r.pressure() + " atm and " + r.temperature() + " C", "Stage " + r.stage()));
        } else if (recipe instanceof CentrifugeRecipe r) {
            List<ItemStack> out = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            for (var o : r.outputs()) {
                out.add(o.item());
                notes.add(o.item().getHoverName().getString() + ": " + (int) (o.chance() * 100) + "%");
            }
            List<List<FluidStack>> fluid = r.fluid().isEmpty() ? List.of() : List.of(List.of(r.fluid()));
            return new Page(List.of(stacks(r.ingredient())), List.of(), out, fluid.isEmpty() ? List.of() : fluid.get(0), notes);
        } else if (recipe instanceof CrystallizingRecipe r) {
            return new Page(List.of(), List.of(List.of(r.fluid())), List.of(r.result()), List.of(), List.of("Freezes at " + r.freezingPoint() + " K"));
        } else if (recipe instanceof CompostingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(), List.of(), List.of("Makes " + r.value() + " compost"));
        } else if (recipe instanceof DryingRecipe r) {
            return new Page(List.of(), List.of(List.of(r.fluid())), List.of(r.result()), List.of(), List.of());
        } else if (recipe instanceof PulseSmeltingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(r.result()), List.of(), List.of("Needs " + r.temperature() + " C"));
        } else if (recipe instanceof MeltingRecipe r) {
            return new Page(List.of(stacks(r.ingredient())), List.of(), List.of(), List.of(r.result()), List.of("Needs " + r.temperature() + " C", r.energy() + " energy"));
        } else if (recipe instanceof WorktableRecipe r) {
            List<List<ItemStack>> in = new ArrayList<>();
            for (Ingredient i : r.pattern().ingredients()) {
                if (!i.isEmpty()) {
                    in.add(stacks(i));
                }
            }
            return new Page(in, List.of(), List.of(r.result()), List.of(), List.of());
        }
        return null;
    }

    /** The one layout every page uses: inputs on the left, an arrow, outputs on the right, notes underneath. */
    private static final class PageCategory implements IRecipeCategory<Page> {
        private static final int WIDTH = 160;
        private static final int HEIGHT = 76;
        private static final int PER_ROW = 4;

        private final RecipeType<Page> type;
        private final Component title;
        private final IDrawable background;
        private final IDrawable icon;

        PageCategory(IGuiHelper gui, RecipeType<Page> type, String title, ItemLike icon) {
            this.type = type;
            this.title = Component.literal(title);
            this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = gui.createDrawableItemStack(new ItemStack(icon));
        }

        @Override
        public RecipeType<Page> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
            int n = 0;
            for (List<ItemStack> stacks : page.itemsIn()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 2 + 18 * (n % PER_ROW), 2 + 18 * (n / PER_ROW)).addItemStacks(stacks);
                n++;
            }
            for (List<FluidStack> fluids : page.fluidsIn()) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, 2 + 18 * (n % PER_ROW), 2 + 18 * (n / PER_ROW));
                if (!fluids.isEmpty()) {
                    slot.addIngredients(NeoForgeTypes.FLUID_STACK, fluids).setFluidRenderer(fluids.get(0).getAmount(), false, 16, 16);
                }
                n++;
            }
            int out = 0;
            for (ItemStack stack : page.itemsOut()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 20 - 18 * (out % 2), 2 + 18 * (out / 2)).addItemStack(stack);
                out++;
            }
            for (FluidStack fluid : page.fluidsOut()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 20 - 18 * (out % 2), 2 + 18 * (out / 2)).addIngredient(NeoForgeTypes.FLUID_STACK, fluid)
                        .setFluidRenderer(fluid.getAmount(), false, 16, 16);
                out++;
            }
        }

        @Override
        public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            Minecraft mc = Minecraft.getInstance();
            graphics.drawString(mc.font, "->", WIDTH / 2 - 4, 7, 0x808080, false);
            int y = 40;
            for (String note : page.notes()) {
                graphics.drawString(mc.font, mc.font.plainSubstrByWidth(note, WIDTH - 4), 2, y, 0x404040, false);
                y += 10;
                if (y > HEIGHT - 8) {
                    break;
                }
            }
        }
    }
}
