package net.scwunge.rotarycraft.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything is in one machine's screen, taken from the original's container and GUI classes: the item slots, the fluid gauges
 * (drawn with the real fluid over the gauge's place in the picture), and progress bars cut from the GUI picture. {@code name} is both
 * the menu's name and the picture's: {@code textures/gui/<name>.png}.
 */
public record GuiLayout(String name, int width, int height, List<SlotPos> slots, List<Gauge> gauges, List<Bar> bars, int tankCount, int extraCount,
                        int inventoryX, int inventoryY, int storageRows, boolean customScreen, List<Field> fields, List<Button> buttons) {
    public record SlotPos(int x, int y) {
    }

    /** A gauge showing tank {@code tank}: filled from {@code bottom} up to {@code height}, {@code capacity} mB being full. */
    public record Gauge(int tank, int x, int bottom, int width, int height, int capacity) {
    }

    /**
     * A bar drawn from the picture's {@code (u, v)} size {@code w} by {@code h} at {@code (x, y)}, as full as extra value {@code value}
     * is of extra value {@code max}; horizontal bars grow rightwards, vertical bars upwards.
     */
    public record Bar(int value, int max, int x, int y, int u, int v, int w, int h, boolean horizontal) {
    }

    /**
     * A box on the screen for a whole number, between {@code min} and {@code max}: its number is machine extra {@code extra}, and typing in it sends the new
     * number to the machine ({@link MachineHost#setField}). {@code label} is a lang key drawn at ({@code labelX}, {@code labelY}), or null.
     */
    public record Field(int index, int extra, int x, int y, int width, int min, int max, String label, int labelX, int labelY, int modeExtra, int modeValue) {
        /** Whether the box is shown, given what the machine says its mode is: always, unless it was set to show in one mode only. */
        public boolean shownIn(int mode) {
            return modeExtra < 0 || mode == modeValue;
        }
    }

    /** A button that sends {@code id} to the machine ({@link MachineHost#menuButton}) and says its state, machine extra {@code state}, in its words: the lang key gui.rotarycraft.&lt;layout&gt;.button&lt;id&gt;.&lt;state&gt;. */
    public record Button(int id, int x, int y, int width, int height, int state) {
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private int width = 176;
        private int height = 166;
        private int inventoryX = 8;
        private int inventoryY = 84;
        private int tanks;
        private int extras;
        private int storageRows;
        private boolean customScreen;
        private final List<SlotPos> slots = new ArrayList<>();
        private final List<Gauge> gauges = new ArrayList<>();
        private final List<Bar> bars = new ArrayList<>();
        private final List<Field> fields = new ArrayList<>();
        private final List<Button> buttons = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            this.inventoryY = height - 82;
            return this;
        }

        /** Where the player's inventory starts, when it is not the usual place. */
        public Builder inventoryAt(int x, int y) {
            this.inventoryX = x;
            this.inventoryY = y;
            return this;
        }

        /**
         * The original's generic storage screen (basicstorage.png): {@code rows} rows of nine slots, the picture drawn in two pieces so it fits any
         * number of rows.
         */
        public Builder storage(int rows) {
            storageRows = rows;
            size(176, 114 + rows * 18);
            return grid(8, 18, 9, rows);
        }

        /** The machine has a screen class of its own (it extends LayoutScreen), registered by the client setup instead of the shared one. */
        public Builder customScreen() {
            customScreen = true;
            return this;
        }

        public Builder slot(int x, int y) {
            slots.add(new SlotPos(x, y));
            return this;
        }

        /** A block of slots, row by row. */
        public Builder grid(int x, int y, int columns, int rows) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    slot(x + 18 * c, y + 18 * r);
                }
            }
            return this;
        }

        public Builder gauge(int tank, int x, int bottom, int width, int height, int capacity) {
            gauges.add(new Gauge(tank, x, bottom, width, height, capacity));
            tanks = Math.max(tanks, tank + 1);
            return this;
        }

        public Builder tanks(int count) {
            tanks = Math.max(tanks, count);
            return this;
        }

        public Builder bar(int value, int max, int x, int y, int u, int v, int w, int h, boolean horizontal) {
            bars.add(new Bar(value, max, x, y, u, v, w, h, horizontal));
            extras = Math.max(extras, Math.max(value, max) + 1);
            return this;
        }

        /** A number box (see {@link Field}); it takes the next free extra, so call {@link #extras} for the machine's own extras first. */
        public Builder field(int x, int y, int width, int min, int max, String label, int labelX, int labelY) {
            fields.add(new Field(fields.size(), extras, x, y, width, min, max, label, labelX, labelY, -1, 0));
            extras++;
            return this;
        }

        /** Makes the box added last show only while machine extra {@code extra} is {@code value} (a machine with modes). */
        public Builder shownWhen(int extra, int value) {
            Field last = fields.remove(fields.size() - 1);
            fields.add(new Field(last.index(), last.extra(), last.x(), last.y(), last.width(), last.min(), last.max(), last.label(), last.labelX(), last.labelY(), extra, value));
            return this;
        }

        /** A button (see {@link Button}) whose words follow machine extra {@code state}, which must be one of the machine's extras. */
        public Builder button(int id, int x, int y, int width, int height, int state) {
            buttons.add(new Button(id, x, y, width, height, state));
            extras = Math.max(extras, state + 1);
            return this;
        }

        public Builder extras(int count) {
            extras = Math.max(extras, count);
            return this;
        }

        public GuiLayout build() {
            return new GuiLayout(name, width, height, List.copyOf(slots), List.copyOf(gauges), List.copyOf(bars), tanks, extras, inventoryX, inventoryY, storageRows, customScreen, List.copyOf(fields), List.copyOf(buttons));
        }
    }

    /** How many numbers the menu keeps in step: torque and speed (two shorts each), then three per tank and two per extra. */
    public int dataCount() {
        return 4 + 3 * tankCount + 2 * extraCount;
    }
}
