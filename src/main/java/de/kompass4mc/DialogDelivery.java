package de.kompass4mc;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Send native dialogs without routing a player's UUID through a command selector. */
public final class DialogDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger("kompass4mc");

    private DialogDelivery() {}

    static Holder<Dialog> dialog(JsonObject definition) {
        Objects.requireNonNull(definition, "definition");
        return Holder.direct(Dialog.DIRECT_CODEC.parse(JsonOps.INSTANCE, definition).getOrThrow());
    }

    public static boolean open(ServerPlayer player, JsonObject definition) {
        try {
            player.openDialog(dialog(definition));
            return true;
        } catch (RuntimeException exception) {
            LOGGER.error("Dialog für Spieler {} konnte nicht geöffnet werden", player.getStringUUID(), exception);
            player.sendSystemMessage(Component.literal(
                    "[Kompass] Das Gespräch konnte nicht geöffnet werden. Bitte versuche es erneut. "
                            + "Die Fehlerdetails stehen in der Minecraft-Protokolldatei latest.log."));
            return false;
        }
    }
}
