package net.scwunge.rotarycraft.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything is in one machine's screen, taken from the original's container and GUI classes: the item slots, the fluid gauges
 * (drawn with the real fluid over the gauge's place in the picture), and progress bars cut from the GUI picture. {@code name} is both
 * the menu's name and the picture's: {@code textures/gui/<name>.png}.
 */
public record GuiLayout(String name, int width, int height, List<SlotPos> slots, List<Gauge> gauges, List<Bar> bars, int tankCount, int extraCount,
                        int inventoryX, int inventoryY, int storageRows, boolean customScreen) {
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

        public Builder extras(int count) {
            extras = Math.max(extras, count);
            return this;
        }

        public GuiLayout build() {
            return new GuiLayout(name, width, height, List.copyOf(slots), List.copyOf(gauges), List.copyOf(bars), tanks, extras, inventoryX, inventoryY, storageRows, customScreen);
        }
    }

    /** How many numbers the menu keeps in step: torque and speed (two shorts each), then three per tank and two per extra. */
    public int dataCount() {
        return 4 + 3 * tankCount + 2 * extraCount;
    }
}
