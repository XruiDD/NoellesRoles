package org.agmas.noellesroles.commander;

import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.Noellesroles;

import java.util.Set;

public class CommanderShopHandler {
    private static final Set<String> REMOVED_IDS = Set.of(
            "lockpick",
            "psycho_mode",
            "crowbar",
            "knife"
    );

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (!gameWorld.isRole(player, Noellesroles.COMMANDER)) {
                return;
            }

            context.getEntries().removeIf(entry -> REMOVED_IDS.contains(entry.id()));

            for (int i = 0; i < context.getEntries().size(); i++) {
                ShopEntry entry = context.getEntries().get(i);
                switch (entry.id()) {
                    case "revolver" -> context.setEntry(i,
                            new ShopEntry.Builder("revolver", WatheItems.REVOLVER.getDefaultStack(), 150, ShopEntry.Type.WEAPON).build());
                    case "blackout" -> context.setEntry(i,
                            new ShopEntry.Builder("blackout", WatheItems.BLACKOUT.getDefaultStack(), 300, ShopEntry.Type.TOOL).build());
                    case "grenade" -> context.setEntry(i,
                            new ShopEntry.Builder("grenade", WatheItems.GRENADE.getDefaultStack(), 400, ShopEntry.Type.WEAPON).build());
                    default -> {
                    }
                }
            }
        });
    }
}
