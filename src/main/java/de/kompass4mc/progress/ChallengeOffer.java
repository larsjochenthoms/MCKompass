package de.kompass4mc.progress;

import java.util.UUID;

/** One saved offer per player and NPC personality, not per spawned entity. */
public final class ChallengeOffer {
    public String ticket = UUID.randomUUID().toString();
    public String taskId;
    public long restockAt;
    public boolean completed;
    public boolean rewarded;
    public boolean materialsIssued;
    public boolean hintUsed;
    public Site site;

    public ChallengeOffer() {}
    public ChallengeOffer(String taskId, long restockAt) { this.taskId = taskId; this.restockAt = restockAt; }
    public record Site(String dimension, int x, int y, int z) {}
    public ChallengeOffer copy() {
        var copy = new ChallengeOffer();
        copy.ticket = ticket; copy.taskId = taskId; copy.restockAt = restockAt;
        copy.completed = completed; copy.rewarded = rewarded; copy.materialsIssued = materialsIssued;
        copy.hintUsed = hintUsed; copy.site = site;
        return copy;
    }
}
