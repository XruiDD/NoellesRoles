package org.agmas.noellesroles;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodecs;
import org.agmas.noellesroles.demonhunter.DemonHunterPistolItem;
import org.agmas.noellesroles.item.AntidoteItem;
import org.agmas.noellesroles.item.FineDrinkItem;
import org.agmas.noellesroles.item.TimedBombItem;
import org.agmas.noellesroles.item.IronManVialItem;
import org.agmas.noellesroles.item.PoisonNeedleItem;
import org.agmas.noellesroles.item.PoisonGasBombItem;
import org.agmas.noellesroles.item.CatalystItem;
import org.agmas.noellesroles.item.RepairToolItem;
import org.agmas.noellesroles.item.ThrowingAxeItem;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.agmas.noellesroles.item.IngredientItem;
import org.agmas.noellesroles.item.ingredient.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static void init() {
        // 注册调剂到 IngredientItem 静态注册表
        IngredientItem.register((IngredientItem) RUM);
        IngredientItem.register((IngredientItem) GIN);
        IngredientItem.register((IngredientItem) VODKA);
        IngredientItem.register((IngredientItem) TEQUILA);
        IngredientItem.register((IngredientItem) WHISKEY);
        IngredientItem.register((IngredientItem) ICE_CUBE);
        IngredientItem.register((IngredientItem) SPECIAL_LIQUEUR);
        IngredientItem.register((IngredientItem) SPECIAL_SPICE);
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

    // ---- 猎魔人系统 ----
    public static final ComponentType<Integer> BULLETS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "bullets"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.INTEGER).build()
    );
    public static final Item DEMON_HUNTER_PISTOL = register(
            new DemonHunterPistolItem(new Item.Settings().maxCount(1).component(BULLETS, 0)),
            "demon_hunter_pistol"
    );

    // ---- 工程师系统 ----
    public static final Item REPAIR_TOOL = register(
            new RepairToolItem(new Item.Settings().maxCount(1)),
            "repair_tool"
    );

    // ---- 调酒师系统 ----
    public static final Item BASE_SPIRIT = register(
            new BaseSpiritItem(new Item.Settings().maxCount(1)),
            "base_spirit"
    );
    public static final Item RUM = register(
            new RumItem(new Item.Settings().maxCount(1)),
            "rum"
    );
    public static final Item GIN = register(
            new GinItem(new Item.Settings().maxCount(1)),
            "gin"
    );
    public static final Item VODKA = register(
            new VodkaItem(new Item.Settings().maxCount(1)),
            "vodka"
    );
    public static final Item TEQUILA = register(
            new TequilaItem(new Item.Settings().maxCount(1)),
            "tequila"
    );
    public static final Item WHISKEY = register(
            new WhiskeyItem(new Item.Settings().maxCount(1)),
            "whiskey"
    );
    public static final Item ICE_CUBE = register(
            new IceCubeItem(new Item.Settings().maxCount(1)),
            "ice_cube"
    );
    public static final Item SPECIAL_LIQUEUR = register(
            new SpecialLiqueurItem(new Item.Settings().maxCount(1)),
            "special_liqueur"
    );
    public static final Item SPECIAL_SPICE = register(
            new SpecialSpiceItem(new Item.Settings().maxCount(1)),
            "special_spice"
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
