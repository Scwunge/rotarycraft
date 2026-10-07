package net.scwunge.rotarycraft.logistics;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What an Item Filter lets through: the properties of the item in its template slot (the item, its damage, its mod, its tags, its class and the interfaces it
 * has, and every value in its components), each set to Match (the item tested must have it too), Mismatch (it must not) or Ignore. The original's metadata
 * and ore dictionary names are the damage and the item tags here, and its NBT is the data components.
 */
public final class MatchData {
    public enum MatchType {
        MATCH(0x008000, "Match"),
        MISMATCH(0xa00000, "Mismatch"),
        IGNORE(0xffd000, "Ignore");

        private static final MatchType[] LIST = values();
        public final String label;
        public final int color;

        MatchType(int color, String label) {
            this.color = color;
            this.label = label;
        }

        public MatchType next() {
            return LIST[(ordinal() + 1) % LIST.length];
        }

        public boolean check(boolean matches) {
            return switch (this) {
                case IGNORE -> true;
                case MATCH -> matches;
                case MISMATCH -> !matches;
            };
        }

        static MatchType of(int ordinal) {
            return ordinal >= 0 && ordinal < LIST.length ? LIST[ordinal] : MATCH;
        }
    }

    /** The pages of the screen. */
    public enum Page {
        BASIC("Basic"), TAGS("Tags"), CLASS("Classes"), COMPONENTS("Components");

        public final String label;

        Page(String label) {
            this.label = label;
        }

        public Page previous() {
            return ordinal() == 0 ? this : values()[ordinal() - 1];
        }

        public Page next() {
            return ordinal() == values().length - 1 ? this : values()[ordinal() + 1];
        }
    }

    /** One line of a page. */
    public record Row(String name, String value, MatchType setting) {
    }

    private static final String[] BASIC_NAMES = {"Item ID", "Damage", "Mod ID", "Components Overall", "Tags Overall"};

    private final ResourceLocation itemId;
    private final Item item;
    private final int damage;
    private final String mod;
    private final List<String> tags;
    private final List<String> classes;
    private final Map<String, Tag> leaves;

    private final MatchType[] basic = {MatchType.MATCH, MatchType.MATCH, MatchType.MATCH, MatchType.MATCH, MatchType.MATCH};
    private final Map<String, MatchType> matchTags = new LinkedHashMap<>();
    private final Map<String, MatchType> matchClass = new LinkedHashMap<>();
    private final Map<String, MatchType> matchLeaf = new LinkedHashMap<>();

    private MatchData(ResourceLocation itemId, Item item, int damage, String mod, List<String> tags, List<String> classes, Map<String, Tag> leaves) {
        this.itemId = itemId;
        this.item = item;
        this.damage = damage;
        this.mod = mod;
        this.tags = tags;
        this.classes = classes;
        this.leaves = leaves;
    }

    /** The data of an item, every setting on Match. */
    public static MatchData of(ItemStack stack, HolderLookup.Provider registries) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<String> tags = new ArrayList<>(stack.getTags().map(t -> t.location().toString()).sorted().toList());
        return new MatchData(id, stack.getItem(), stack.getDamageValue(), id.getNamespace(), tags, classes(stack.getItem()), leaves(stack, registries));
    }

    private static List<String> classes(Item item) {
        List<String> out = new ArrayList<>();
        Class<?> c = item.getClass();
        int n = 0;
        do {
            String prefix = ">".repeat(n);
            out.add(prefix + c.getSimpleName());
            for (Class<?> in : c.getInterfaces()) {
                out.add(prefix + "%" + in.getSimpleName());
            }
            c = c.getSuperclass();
            n++;
        } while (c != null && c != Item.class);
        return out;
    }

    /** Every value in the stack's components, each at a path (the component, then the names and list positions down to the value). */
    @SuppressWarnings("unchecked")
    public static Map<String, Tag> leaves(ItemStack stack, HolderLookup.Provider registries) {
        Map<String, Tag> out = new LinkedHashMap<>();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : stack.getComponentsPatch().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            DataComponentType<Object> type = (DataComponentType<Object>) entry.getKey();
            Codec<Object> codec = type.codec();
            ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (codec == null || key == null) {
                continue;
            }
            codec.encodeStart(ops, entry.getValue().get()).result().ifPresent(tag -> flatten(key.toString(), tag, out));
        }
        return out;
    }

    private static void flatten(String path, Tag tag, Map<String, Tag> out) {
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                flatten(path + "." + key, Objects.requireNonNull(compound.get(key)), out);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                flatten(path + "#" + i, list.get(i), out);
            }
        } else {
            out.put(path, tag);
        }
    }

    /** Takes the settings of {@code old} where this one has the same property: changing the template item keeps what was chosen. */
    public MatchData loadFrom(@Nullable MatchData old) {
        if (old != null) {
            System.arraycopy(old.basic, 0, basic, 0, basic.length);
            for (String s : tags) {
                matchTags.put(s, old.matchTags.getOrDefault(s, MatchType.MATCH));
            }
            for (String s : classes) {
                matchClass.put(s, old.matchClass.getOrDefault(s, MatchType.MATCH));
            }
            for (String s : leaves.keySet()) {
                matchLeaf.put(s, old.matchLeaf.getOrDefault(s, MatchType.MATCH));
            }
        }
        return this;
    }

    private MatchType tagSetting(String name) {
        return matchTags.getOrDefault(name, MatchType.MATCH);
    }

    private MatchType classSetting(String name) {
        return matchClass.getOrDefault(name, MatchType.MATCH);
    }

    private MatchType leafSetting(String name) {
        return matchLeaf.getOrDefault(name, MatchType.MATCH);
    }

    // ---- the screen's view ----

    public List<Row> rows(Page page) {
        List<Row> out = new ArrayList<>();
        switch (page) {
            case BASIC -> {
                String[] values = {itemId.toString(), String.valueOf(damage), mod, leaves.isEmpty() ? "" : leaves.size() + " values", tags.isEmpty() ? "" : tags.size() + " tags"};
                for (int i = 0; i < BASIC_NAMES.length; i++) {
                    out.add(new Row(BASIC_NAMES[i], values[i], basic[i]));
                }
            }
            case TAGS -> tags.forEach(s -> out.add(new Row("Tag", s, tagSetting(s))));
            case CLASS -> {
                for (String s : classes) {
                    String name = "Item Class";
                    String value = s;
                    if (s.startsWith(">")) {
                        int n = 0;
                        while (value.startsWith(">")) {
                            value = value.substring(1);
                            n++;
                        }
                        name = "Parent Class x" + n;
                    }
                    if (value.startsWith("%")) {
                        value = value.substring(1);
                        name = "Interface";
                    }
                    out.add(new Row(name, value, classSetting(s)));
                }
            }
            case COMPONENTS -> leaves.forEach((path, tag) -> out.add(new Row(path, tag.getAsString(), leafSetting(path))));
        }
        return out;
    }

    /** The key of row {@code index} of a page in the settings maps. */
    @Nullable
    private String key(Page page, int index) {
        List<String> keys = switch (page) {
            case TAGS -> tags;
            case CLASS -> classes;
            case COMPONENTS -> new ArrayList<>(leaves.keySet());
            default -> List.of();
        };
        return index >= 0 && index < keys.size() ? keys.get(index) : null;
    }

    /** Sets a row to the next of Match, Mismatch, Ignore. */
    public void increment(Page page, int index) {
        set(page, index, rows(page).size() > index && index >= 0 ? rows(page).get(index).setting().next() : MatchType.MATCH);
    }

    public void set(Page page, int index, MatchType type) {
        if (page == Page.BASIC) {
            if (index >= 0 && index < basic.length) {
                basic[index] = type;
            }
            return;
        }
        String key = key(page, index);
        if (key != null) {
            (switch (page) {
                case TAGS -> matchTags;
                case CLASS -> matchClass;
                default -> matchLeaf;
            }).put(key, type);
        }
    }

    public void setAll(Page page, MatchType type) {
        int n = rows(page).size();
        for (int i = 0; i < n; i++) {
            set(page, i, type);
        }
    }

    public Item item() {
        return item;
    }

    // ---- matching ----

    public boolean match(ItemStack stack, HolderLookup.Provider registries) {
        if (!basic[0].check(stack.is(item))) {
            return false;
        }
        if (!basic[1].check(stack.getDamageValue() == damage)) {
            return false;
        }
        if (!basic[2].check(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(mod))) {
            return false;
        }
        if (basic[4] != MatchType.IGNORE) {
            Set<String> has = new HashSet<>(stack.getTags().map(t -> t.location().toString()).toList());
            for (String s : tags) {
                if (!tagSetting(s).check(has.contains(s))) {
                    return false;
                }
            }
        }
        Class<?> c1 = item.getClass();
        Class<?> c2 = stack.getItem().getClass();
        int n = 0;
        do {
            String prefix = ">".repeat(n);
            MatchType m = classSetting(prefix + c1.getSimpleName());
            if (!m.check(c1 == c2)) {
                return false;
            }
            Set<Class<?>> ints2 = new HashSet<>(List.of(c2.getInterfaces()));
            for (Class<?> in : c1.getInterfaces()) {
                m = classSetting(prefix + "%" + in.getSimpleName());
                if (!m.check(ints2.contains(in))) {
                    return false;
                }
            }
            c1 = c1.getSuperclass();
            c2 = c2.getSuperclass();
            n++;
        } while (c1 != null && c1 != Item.class && c2 != null && c2 != Item.class);
        if (basic[3] != MatchType.IGNORE) {
            Map<String, Tag> there = leaves(stack, registries);
            boolean all = true;
            if (leaves.isEmpty()) {
                all = there.isEmpty();
            } else {
                for (Map.Entry<String, Tag> entry : leaves.entrySet()) {
                    MatchType m = leafSetting(entry.getKey());
                    if (m != MatchType.IGNORE && !m.check(Objects.equals(there.get(entry.getKey()), entry.getValue()))) {
                        all = false;
                        break;
                    }
                }
            }
            return basic[3].check(all);
        }
        return true;
    }

    // ---- saving ----

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", itemId.toString());
        tag.putInt("damage", damage);
        tag.putString("mod", mod);
        ListTag tagList = new ListTag();
        tags.forEach(s -> tagList.add(StringTag.valueOf(s)));
        tag.put("tags", tagList);
        ListTag classList = new ListTag();
        classes.forEach(s -> classList.add(StringTag.valueOf(s)));
        tag.put("classes", classList);
        ListTag leafList = new ListTag();
        leaves.forEach((path, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("path", path);
            entry.put("value", value.copy());
            leafList.add(entry);
        });
        tag.put("leaves", leafList);
        CompoundTag settings = new CompoundTag();
        for (int i = 0; i < basic.length; i++) {
            settings.putInt("basic" + i, basic[i].ordinal());
        }
        CompoundTag t = new CompoundTag();
        matchTags.forEach((k, v) -> t.putInt(k, v.ordinal()));
        settings.put("tags", t);
        CompoundTag c = new CompoundTag();
        matchClass.forEach((k, v) -> c.putInt(k, v.ordinal()));
        settings.put("classes", c);
        CompoundTag l = new CompoundTag();
        matchLeaf.forEach((k, v) -> l.putInt(k, v.ordinal()));
        settings.put("leaves", l);
        tag.put("settings", settings);
        return tag;
    }

    @Nullable
    public static MatchData fromTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("item"));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        List<String> tags = new ArrayList<>();
        tag.getList("tags", Tag.TAG_STRING).forEach(t -> tags.add(t.getAsString()));
        List<String> classes = new ArrayList<>();
        tag.getList("classes", Tag.TAG_STRING).forEach(t -> classes.add(t.getAsString()));
        Map<String, Tag> leaves = new LinkedHashMap<>();
        for (Tag t : tag.getList("leaves", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            leaves.put(entry.getString("path"), entry.get("value"));
        }
        MatchData data = new MatchData(id, BuiltInRegistries.ITEM.get(id), tag.getInt("damage"), tag.getString("mod"), tags, classes, leaves);
        CompoundTag settings = tag.getCompound("settings");
        for (int i = 0; i < data.basic.length; i++) {
            data.basic[i] = MatchType.of(settings.getInt("basic" + i));
        }
        for (String k : settings.getCompound("tags").getAllKeys()) {
            data.matchTags.put(k, MatchType.of(settings.getCompound("tags").getInt(k)));
        }
        for (String k : settings.getCompound("classes").getAllKeys()) {
            data.matchClass.put(k, MatchType.of(settings.getCompound("classes").getInt(k)));
        }
        for (String k : settings.getCompound("leaves").getAllKeys()) {
            data.matchLeaf.put(k, MatchType.of(settings.getCompound("leaves").getInt(k)));
        }
        return data;
    }

    /** Whether a stack's tag is one of this data's, for the tests. */
    public boolean hasTag(TagKey<Item> key) {
        return tags.contains(key.location().toString());
    }
}
