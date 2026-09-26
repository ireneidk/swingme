package com.swingme.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Reads the SkyBlock item UUID Hypixel writes into an item's {@code custom_data} NBT.
 * This is the same identity Skyblocker's item-lock keybind uses, and it is unique per
 * item instance — two identical weapons have different UUIDs.
 * <p>
 * Only SkyBlock-issued items carry one; vanilla and stackable items return "".
 */
public final class ItemIdentity {

    private static final String UUID_KEY = "uuid";

    private ItemIdentity() {}

    /** @return the item's SkyBlock UUID, or an empty string when it has none. */
    public static String uuidOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return "";
        return data.copyTag().getStringOr(UUID_KEY, "");
    }
}
