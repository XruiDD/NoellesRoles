package org.agmas.noellesroles.hunter;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

import java.util.Set;

public class HunterShopHandler {
    private static final Set<String> ALLOWED_VANILLA_IDS = Set.of(
        "knife",
        "body_bag",
        "crowbar",
        "scorpion"
    );

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.HUNTER)) {
                return;
            }

            context.getEntries().removeIf(entry -> !ALLOWED_VANILLA_IDS.contains(entry.id()));

            context.addEntry(0, new ShopEntry.Builder("hunter_trap", ModItems.HUNTER_TRAP.getDefaultStack(), 75, ShopEntry.Type.WEAPON).build());
            context.addEntry(0, new ShopEntry.Builder("double_barrel_shell", ModItems.DOUBLE_BARREL_SHELL.getDefaultStack(), 100, ShopEntry.Type.WEAPON).build());
            context.addEntry(0, new ShopEntry.Builder("double_barrel_shotgun", ModItems.DOUBLE_BARREL_SHOTGUN.getDefaultStack(), 200, ShopEntry.Type.WEAPON).stock(1).build());
        });
    }
}
