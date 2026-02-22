package org.agmas.noellesroles;

import net.minecraft.item.Item;
import org.agmas.noellesroles.item.AntidoteItem;
import org.agmas.noellesroles.item.FineDrinkItem;
import org.agmas.noellesroles.item.TimedBombItem;
import org.agmas.noellesroles.item.IronManVialItem;
import org.agmas.noellesroles.item.PoisonNeedleItem;
import org.agmas.noellesroles.item.PoisonGasBombItem;
import org.agmas.noellesroles.item.CatalystItem;
import org.agmas.noellesroles.item.ThrowingAxeItem;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.agmas.noellesroles.item.IngredientItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static void init() {
    }

    public static final Item MASTER_KEY = register(
            new Item(new Item.Settings().maxCount(1)),
            "master_key"
    );
    public static final Item NEUTRAL_MASTER_KEY = register(
            new Item(new Item.Settings().maxCount(1)),
            "neutral_master_key"
    );
    public static final Item DEFENSE_VIAL = register(
            new Item(new Item.Settings().maxCount(1)),
            "defense_vial"
    );
    public static final Item ROLE_MINE = register(
            new Item(new Item.Settings().maxCount(1)),
            "role_mine"
    );
    public static final Item TIMED_BOMB = register(
            new TimedBombItem(new Item.Settings().maxCount(1)),
            "timed_bomb"
    );
    public static final Item FINE_DRINK = register(
            new FineDrinkItem(new Item.Settings().maxCount(1)),
            "fine_drink"
    );
    public static final Item IRON_MAN_VIAL = register(
            new IronManVialItem(new Item.Settings().maxCount(1)),
            "iron_man_vial"
    );
    public static final Item ANTIDOTE = register(
            new AntidoteItem(new Item.Settings().maxCount(1)),
            "antidote"
    );
    public static final Item POISON_NEEDLE = register(
            new PoisonNeedleItem(new Item.Settings().maxCount(1)),
            "poison_needle"
    );
    public static final Item POISON_GAS_BOMB = register(
            new PoisonGasBombItem(new Item.Settings().maxCount(16)),
            "poison_gas_bomb"
    );
    public static final Item THROWING_AXE = register(
            new ThrowingAxeItem(new Item.Settings().maxCount(1)),
            "throwing_axe"
    );
    public static final Item CATALYST = register(
            new CatalystItem(new Item.Settings().maxCount(1)),
            "catalyst"
    );

    // ---- 调酒师系统 ----
    public static final Item BASE_SPIRIT = register(
            new BaseSpiritItem(new Item.Settings().maxCount(1)),
            "base_spirit"
    );
    public static final Item RUM = register(
            new IngredientItem(new Item.Settings().maxCount(1), "rum"),
            "rum"
    );
    public static final Item GIN = register(
            new IngredientItem(new Item.Settings().maxCount(1), "gin"),
            "gin"
    );
    public static final Item VODKA = register(
            new IngredientItem(new Item.Settings().maxCount(1), "vodka"),
            "vodka"
    );
    public static final Item TEQUILA = register(
            new IngredientItem(new Item.Settings().maxCount(1), "tequila"),
            "tequila"
    );
    public static final Item WHISKEY = register(
            new IngredientItem(new Item.Settings().maxCount(1), "whiskey"),
            "whiskey"
    );
    public static final Item ICE_CUBE = register(
            new IngredientItem(new Item.Settings().maxCount(1), "ice_cube"),
            "ice_cube"
    );

    public static Item register(Item item, String id) {
        // Create the identifier for the item.
        Identifier itemID = Identifier.of(Noellesroles.MOD_ID, id);

        // Register the item.
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);

        // Return the registered item!
        return registeredItem;
    }

}
