# 📝 Historique des changements

Les changements récents du plugin, du plus récent au plus ancien. Le guide du joueur est dans le [README](README.md).

## 25/10/2026

### Documentation
- README réécrit pour être plus simple à lire (français et [anglais](README.en.md)), avec une partie dédiée aux administrateurs.
- Quantités des Âges 3 à 5 corrigées (elles ne correspondaient plus à `config.yml`) et 6ᵉ spécialisation (Rôdeur Solitaire) documentée.
- Textes du menu `/guild spec` corrigés : le Syndicat Marchand n'a pas de taxe à l'Hôtel des Ventes (au lieu de « +25 % ») et le rayon du Conclave frappe toutes les 2 secondes.

### Corrections
- `/money` était réservé aux administrateurs : les joueurs peuvent maintenant voir leur solde (les sous-commandes restent réservées aux administrateurs).

### Site web (PR #6 et #7)
- Site public en lecture seule lancé avec le plugin (port 80 par défaut) : accueil, classement des guildes, fiche de guilde, joueurs, API JSON.
- Design moderne : thème clair/sombre, cartes arrondies, avatars colorés, médailles, responsive.
- Sécurité : pages pré-rendues, limite de requêtes par IP, file d'attente bornée, aucune coordonnée, joueurs en vanish masqués.

### Traités entre guildes (PR #5)
- Alliance, pacte de non-agression et vassalité (`/traite`), avec indemnité et préavis en cas de trahison.
- Pas de raid entre guildes liées, trains gratuits et pas de dégâts entre alliés (potions comprises).
- Correctifs de sécurité : rupture atomique (plus de double indemnité), annulation réservée aux officiers.
- Tableau de bord (`/hud`) optimisé : il n'est mis à jour que si son contenu change.

### Réseau ferroviaire (PR #4)
- Gares (`[Gare]`), voyage instantané le long des rails (`/station`), péages et relations entre guildes (`/rail`).

### Gameplay (PR #3)
- Contrats quotidiens, événements serveur (lune de sang, météorites, marché noir, boss mondial), saisons et mémorial, tableau de bord.

### Fonctionnalités manquantes et sécurité (PR #1 et #2)
- **Arcaniste** : +15 % de dégâts subis en mêlée (attaques directes uniquement).
- **Forgeron** : enclumes, meules et hauts-fourneaux ne sont fabricables que par les Forgerons jusqu'à l'Âge 3 (leur usage reste libre).
- **Enclave Champêtre** : reproduction animale ×3 ; les armures en Netherite sont retirées automatiquement.
- **Industrie Souterraine** : potions de niveau II refusées.
- **Siège de Pillards** : 1 Ravageur sur 6 et 1 Vindicateur sur 3.
- **Syndicat Marchand** : les 10 % de dividende sont prélevés sur la prime du joueur (plus de création d'argent).
- Protections de claim complétées (pistons, fluides, feu, chorus, entités), dupe des bonus de minage corrigé, Nexus impossible à reposer, quotas d'Âge basés sur le maximum de membres, limite de 10 membres, invitations qui expirent.
- Cooldowns de raid et pièges sauvegardés en base (survivent aux redémarrages).
- Sauvegarde périodique en une seule transaction SQLite.
