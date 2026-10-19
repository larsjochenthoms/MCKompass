package de.kompass4mc;

import static org.junit.jupiter.api.Assertions.*;

import de.kompass4mc.learning.LearningCatalog;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class NpcSkinTest {
    @Test
    void eachLearningCharacterHasAnIndependentLocalSkin() {
        var ids = new HashSet<String>();
        for (var skin : NpcSkin.values()) {
            assertTrue(ids.add(skin.id()));
            assertEquals(skin, NpcSkin.fromMarker(skin.marker()).orElseThrow());
            assertEquals(skin, NpcSkin.fromResidentName(skin.residentName()).orElseThrow());
        }
        assertEquals(3, ids.size());
        LearningCatalog.all().forEach(task -> assertTrue(NpcSkin.fromResidentName(task.npcName()).isPresent()));
    }

    @Test
    void ordinaryNamesAndUnrecognizedMarkersDoNotSelectAPlayerRenderer() {
        for (String marker : new String[] {null, "", "Mila", "mila", "kompass4mc:npc_skin/unknown",
                "other:npc_skin/mila", "kompass4mc:npc_skin/../mila", "kompass4mc:npc_skin/mila.png"}) {
            assertTrue(NpcSkin.fromMarker(marker).isEmpty(), "Unknown marker must use the default renderer");
        }
        assertTrue(NpcSkin.fromResidentName(null).isEmpty());
        assertTrue(NpcSkin.fromResidentName("Mila · auf Lernreise").isEmpty());
    }

    @Test
    void allThreePackagedSkinsAreDistinctModernPlayerTextures() throws Exception {
        var hashes = new HashSet<String>();
        for (var skin : NpcSkin.values()) {
            try (var resource = getClass().getResourceAsStream("/assets/kompass4mc/textures/entity/" + skin.id() + ".png")) {
                assertNotNull(resource, "Bundled NPC skin must be available offline");
                byte[] bytes = resource.readAllBytes();
                assertTrue(hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))));
                var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                assertNotNull(image);
                assertEquals(64, image.getWidth());
                assertEquals(64, image.getHeight());
                assertEquals(255, image.getRGB(12, 12) >>> 24, "The face must be opaque");
            }
        }
    }
}
