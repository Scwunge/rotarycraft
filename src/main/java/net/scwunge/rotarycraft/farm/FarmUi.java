package net.scwunge.rotarycraft.farm;

/**
 * How a farming machine's screen is laid out: the picture it is drawn on (a texture under textures/gui, or "basic_storage" for the original's
 * chest background), the size of the panel, where its slots are (x, y pairs, from the panel's top left), and where the player's inventory
 * begins (-1 for a panel with no inventory).
 *
 * @param texture     the name under textures/gui
 * @param width       panel width
 * @param height      panel height
 * @param slots       x and y of each slot of the machine, one pair after another
 * @param inventoryY  the top of the player's inventory (its hotbar is 58 below), or -1 for none
 * @param storageRows for the chest layout, the rows of nine slots, otherwise 0
 */
public record FarmUi(String texture, int width, int height, int[] slots, int inventoryY, int storageRows) {
    /** The chest-like layout: {@code rows} rows of nine slots. */
    public static FarmUi storage(int rows) {
        int[] slots = new int[rows * 18];
        for (int i = 0; i < rows * 9; i++) {
            slots[2 * i] = 8 + (i % 9) * 18;
            slots[2 * i + 1] = 18 + (i / 9) * 18;
        }
        return new FarmUi("basic_storage", 176, 114 + rows * 18, slots, 31 + rows * 18, rows);
    }

    /** A panel of the original's on its own picture, with the slots where its container puts them (and the usual 166-high panel). */
    public static FarmUi custom(String texture, int... slots) {
        return new FarmUi(texture, 176, 166, slots, 84, 0);
    }

    /** A panel on its own picture of another height, with the player's inventory at {@code inventoryY} (-1 for none). */
    public static FarmUi panel(String texture, int height, int inventoryY, int... slots) {
        return new FarmUi(texture, 176, height, slots, inventoryY, 0);
    }

    public int slotCount() {
        return slots.length / 2;
    }
}
