package de.kompass4mc.learning;

import static org.junit.jupiter.api.Assertions.*;
import de.kompass4mc.village.GridPos;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class BuildChallengesTest {
    @Test void allTwelvePlansHaveExactBuildsWithDetectableMissingWrongAndExtraBlocks() {
        assertEquals(12, BuildChallenges.all().size());
        for (var plan : BuildChallenges.all()) {
            var pattern = plan.pattern();
            assertTrue(plan.supplies() > 0 && plan.supplies() <= 64);
            var actual = new HashMap<>(pattern.required());
            assertTrue(pattern.inspect(actual).complete(), plan.id());
            var pos = actual.keySet().iterator().next();
            actual.remove(pos);
            assertEquals(1, pattern.inspect(actual).missing().size());
            actual.put(pos, "minecraft:diamond_block");
            assertEquals(1, pattern.inspect(actual).wrongMaterial().size());
            actual.put(pos, plan.material());
            actual.put(pattern.forbidden().iterator().next(), plan.material());
            assertEquals(1, pattern.inspect(actual).extra().size());
            assertEquals(Interaction.BUILD, LearningCatalog.byId(plan.id()).orElseThrow().interaction());
            assertEquals("Mila", LearningCatalog.byId(plan.id()).orElseThrow().npcName());
            if (plan.shape() == BuildChallenges.Shape.TOWERS) {
                assertEquals(2 * plan.height(), plan.supplies());
                assertTrue(pattern.required().containsKey(new GridPos(0, plan.height() - 1, 0)));
            }
        }
    }
}
