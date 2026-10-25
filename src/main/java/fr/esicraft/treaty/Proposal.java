package fr.esicraft.treaty;

/** Proposition de traité en attente de réponse (non persistée : elle expire de toute façon en quelques minutes). */
public record Proposal(String fromId, String toId, TreatyType type, String suzerainId, long expiresAt) {
}
