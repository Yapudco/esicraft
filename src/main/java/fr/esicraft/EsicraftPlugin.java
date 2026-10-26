package fr.esicraft;

import fr.esicraft.age.AgeManager;
import fr.esicraft.age.listener.AgeDimensionListener;
import fr.esicraft.chat.ChatFormatListener;
import fr.esicraft.command.AgeCommand;
import fr.esicraft.command.EnglishCommand;
import fr.esicraft.command.BaltopCommand;
import fr.esicraft.command.EsicraftAdminCommand;
import fr.esicraft.command.ContractCommand;
import fr.esicraft.command.EsicraftCommand;
import fr.esicraft.command.FunCommand;
import fr.esicraft.command.SeasonCommand;
import fr.esicraft.command.GameCommand;
import fr.esicraft.command.GuideCommand;
import fr.esicraft.command.GuildCommand;
import fr.esicraft.command.HubCommand;
import fr.esicraft.command.MarketCommand;
import fr.esicraft.command.MoneyCommand;
import fr.esicraft.command.NexusCommand;
import fr.esicraft.command.PayCommand;
import fr.esicraft.command.PvPCommand;
import fr.esicraft.command.RaceCommand;
import fr.esicraft.data.DataManager;
import fr.esicraft.data.PlayerConnectionListener;
import fr.esicraft.economy.AntiFarmManager;
import fr.esicraft.economy.listener.PlayerKillEconomyListener;
import fr.esicraft.gui.GUIListener;
import fr.esicraft.guild.ClaimVisualizerManager;
import fr.esicraft.guild.GuildManager;
import fr.esicraft.guild.listener.GuildClaimListener;
import fr.esicraft.market.MarketManager;
import fr.esicraft.nexus.NexusListener;
import fr.esicraft.nexus.NexusManager;
import fr.esicraft.race.RaceManager;
import fr.esicraft.race.listener.CraftRestrictionListener;
import fr.esicraft.race.listener.RaceBuffTask;
import fr.esicraft.guild.listener.SpecializationRulesListener;
import fr.esicraft.race.listener.RaceCombatListener;
import fr.esicraft.race.listener.RaceInteractionListener;
import fr.esicraft.raid.RaidManager;
import fr.esicraft.rail.RailGuiListener;
import fr.esicraft.rail.RailListener;
import fr.esicraft.rail.RailManager;
import fr.esicraft.command.RailCommand;
import fr.esicraft.command.TreatyCommand;
import fr.esicraft.web.WebServer;
import fr.esicraft.util.Lang;
import fr.esicraft.life.AchievementManager;
import fr.esicraft.life.Chronicle;
import fr.esicraft.combat.CombatManager;
import fr.esicraft.economy.BalanceManager;
import fr.esicraft.economy.LoanManager;
import fr.esicraft.social.MentorManager;
import fr.esicraft.social.RansomManager;
import fr.esicraft.social.ReputationManager;
import fr.esicraft.life.CosmeticManager;
import fr.esicraft.life.DisguiseManager;
import fr.esicraft.life.GuildContractManager;
import fr.esicraft.life.GuildSynergy;
import fr.esicraft.life.HelpCallManager;
import fr.esicraft.life.LifeCommand;
import fr.esicraft.life.NatureSeasonManager;
import fr.esicraft.util.LangListener;
import fr.esicraft.treaty.AlliedProtectionListener;
import fr.esicraft.treaty.TreatyGuiListener;
import fr.esicraft.treaty.TreatyManager;
import fr.esicraft.command.StationCommand;
import fr.esicraft.fun.ContractListener;
import fr.esicraft.fun.ContractManager;
import fr.esicraft.fun.EventManager;
import fr.esicraft.fun.HudManager;
import fr.esicraft.fun.SeasonManager;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EsicraftPlugin extends JavaPlugin {
   private static EsicraftPlugin instance;
   private DataManager dataManager;
   private AgeManager ageManager;
   private SpecializationRulesListener specializationRules;
   private ContractManager contractManager;
   private SeasonManager seasonManager;
   private EventManager eventManager;
   private HudManager hudManager;
   private RailManager railManager;
   private TreatyManager treatyManager;
   private WebServer webServer;
   private RaceManager raceManager;
   private GuildManager guildManager;
   private NexusManager nexusManager;
   private RaidManager raidManager;
   private MarketManager marketManager;
   private AntiFarmManager antiFarmManager;
   private ClaimVisualizerManager claimVisualizerManager;
   private Chronicle chronicle;
   private AchievementManager achievements;
   private NatureSeasonManager natureSeasons;
   private GuildSynergy synergy;
   private GuildContractManager guildContracts;
   private HelpCallManager helpCalls;
   private DisguiseManager disguiseManager;
   private CombatManager combatManager;
   private AgeDimensionListener ageDimension;
   private CosmeticManager cosmetics;
   private BalanceManager balance;
   private LoanManager loans;
   private ReputationManager reputation;
   private MentorManager mentors;
   private RansomManager ransom;
   private boolean nexusDestructionEnabled = true;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.getLogger().info("=================================================");
      this.getLogger().info("    Démarrage de Esicraft-Plugin v" + this.getPluginMeta().getVersion());
      this.getLogger().info("    Survie Stratégique multi-joueurs 50+ joueurs");
      this.getLogger().info("=================================================");
      this.ageManager = new AgeManager(this);
      this.raceManager = new RaceManager(this);
      this.guildManager = new GuildManager(this);
      this.nexusManager = new NexusManager(this);
      this.raidManager = new RaidManager(this);
      this.marketManager = new MarketManager(this);
      this.antiFarmManager = new AntiFarmManager(this);
      this.claimVisualizerManager = new ClaimVisualizerManager(this);
      this.contractManager = new ContractManager(this);
      this.seasonManager = new SeasonManager(this);
      this.eventManager = new EventManager(this);
      this.hudManager = new HudManager(this);
      this.railManager = new RailManager(this);
      this.treatyManager = new TreatyManager(this);
      this.chronicle = new Chronicle(this);
      this.achievements = new AchievementManager(this);
      this.natureSeasons = new NatureSeasonManager(this);
      this.synergy = new GuildSynergy(this);
      this.guildContracts = new GuildContractManager(this);
      this.helpCalls = new HelpCallManager(this);
      this.disguiseManager = new DisguiseManager(this);
      this.combatManager = new CombatManager(this);
      this.balance = new BalanceManager(this);
      this.loans = new LoanManager(this);
      this.reputation = new ReputationManager(this);
      this.mentors = new MentorManager(this);
      this.ransom = new RansomManager(this);
      this.cosmetics = new CosmeticManager(this);
      this.dataManager = new DataManager(this);
      this.dataManager.init();
      Lang.init(this);
      this.chronicle.init();
      this.achievements.init();
      this.seasonManager.init();
      this.natureSeasons.init();
      this.synergy.init();
      this.guildContracts.init();
      this.helpCalls.init();
      this.balance.init();
      this.loans.init();
      this.reputation.init();
      this.mentors.init();
      this.ransom.start();
      this.cosmetics.init();
      this.guildManager.initCaches();
      this.disguiseManager.init();
      this.railManager.load();
      this.treatyManager.load();
      this.webServer = new WebServer(this);
      this.webServer.start();
      this.eventManager.start();
      this.hudManager.start();
      this.nexusManager.removeAllHolograms();
      this.specializationRules = new SpecializationRulesListener(this);
      PluginManager pm = Bukkit.getPluginManager();
      pm.registerEvents(new PlayerConnectionListener(this), this);
      pm.registerEvents(new LangListener(this), this);
      pm.registerEvents(this.natureSeasons, this);
      pm.registerEvents(this.disguiseManager, this);
      pm.registerEvents(this.combatManager, this);
      pm.registerEvents(this.ransom, this);
      pm.registerEvents(this.cosmetics, this);
      pm.registerEvents(this.loans, this);
      this.combatManager.start();
      pm.registerEvents(new CraftRestrictionListener(this), this);
      pm.registerEvents(new RaceInteractionListener(this), this);
      pm.registerEvents(new RaceCombatListener(this), this);
      pm.registerEvents(new GuildClaimListener(this), this);
      pm.registerEvents(this.specializationRules, this);
      pm.registerEvents(new ContractListener(this), this);
      pm.registerEvents(new TreatyGuiListener(this), this);
      pm.registerEvents(new AlliedProtectionListener(this), this);
      pm.registerEvents(this.eventManager, this);
      pm.registerEvents(new RailListener(this), this);
      pm.registerEvents(new RailGuiListener(this), this);
      pm.registerEvents(new NexusListener(this), this);
      pm.registerEvents(this.raidManager, this);
      pm.registerEvents(new GUIListener(this), this);
      pm.registerEvents(new ChatFormatListener(this), this);
      pm.registerEvents(new PlayerKillEconomyListener(this), this);
      this.ageDimension = new AgeDimensionListener(this);
      pm.registerEvents(this.ageDimension, this);
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      this.registerCommands();
      new RaceBuffTask(this, this.specializationRules).runTaskTimer(this, 40L, 40L);
      this.getLogger().info("Esicraft-Plugin a été activé avec succès !");
   }

   public void onDisable() {
      this.getLogger().info("Arrêt de Esicraft-Plugin en cours...");
      try {
         if (this.achievements != null) {
            this.achievements.shutdown();
         }

         if (this.guildContracts != null) {
            this.guildContracts.flush();
         }
      } catch (RuntimeException e) {
         this.getLogger().warning("Sauvegarde des succès / contrats de guilde impossible : " + e.getMessage());
      }

      if (this.raidManager != null) {
         try {
            this.raidManager.stopAllRaids();
         } catch (Exception var2) {
            this.getLogger().warning("Impossible d'arrêter proprement les raids : " + var2.getMessage());
         }
      }

      if (this.webServer != null) {
         this.webServer.stop();
      }

      if (this.eventManager != null) {
         this.eventManager.shutdown();
      }

      if (this.contractManager != null) {
         this.contractManager.flush();
      }

      if (this.nexusManager != null) {
         this.nexusManager.removeAllHolograms();
      }

      if (this.loans != null) {
         this.loans.shutdown();
      }

      if (this.disguiseManager != null) {
         this.disguiseManager.shutdown();
      }

      if (this.dataManager != null) {
         this.dataManager.close();
      }

      this.getLogger().info("Toutes les données ont été sauvegardées. À bientôt !");
   }

   private void registerCommands() {
      Objects.requireNonNull(this.getCommand("cosmetique")).setExecutor(this.cosmetics);
      Objects.requireNonNull(this.getCommand("merci")).setExecutor(this.reputation);
      Objects.requireNonNull(this.getCommand("merci")).setTabCompleter(this.reputation);
      Objects.requireNonNull(this.getCommand("parrain")).setExecutor(this.mentors);
      Objects.requireNonNull(this.getCommand("parrain")).setTabCompleter(this.mentors);
      Objects.requireNonNull(this.getCommand("rancon")).setExecutor(this.ransom);
      Objects.requireNonNull(this.getCommand("rancon")).setTabCompleter(this.ransom);
      Objects.requireNonNull(this.getCommand("banque")).setExecutor(this.loans);
      Objects.requireNonNull(this.getCommand("banque")).setTabCompleter(this.loans);
      Objects.requireNonNull(this.getCommand("esicraft")).setExecutor(new EsicraftCommand(this));
      RaceCommand raceCommand = new RaceCommand(this);
      Objects.requireNonNull(this.getCommand("race")).setExecutor(raceCommand);
      Objects.requireNonNull(this.getCommand("race")).setTabCompleter(raceCommand);
      GuildCommand guildCommand = new GuildCommand(this);
      Objects.requireNonNull(this.getCommand("guild")).setExecutor(guildCommand);
      Objects.requireNonNull(this.getCommand("guild")).setTabCompleter(guildCommand);
      EnglishCommand englishCommand = new EnglishCommand();
      Objects.requireNonNull(this.getCommand("english")).setExecutor(englishCommand);
      Objects.requireNonNull(this.getCommand("francais")).setExecutor(englishCommand);
      Objects.requireNonNull(this.getCommand("age")).setExecutor(new AgeCommand(this));
      Objects.requireNonNull(this.getCommand("nexus")).setExecutor(new NexusCommand(this));
      Objects.requireNonNull(this.getCommand("ah")).setExecutor(new MarketCommand(this));
      Objects.requireNonNull(this.getCommand("money")).setExecutor(new MoneyCommand(this));
      Objects.requireNonNull(this.getCommand("baltop")).setExecutor(new BaltopCommand(this));
      PayCommand payCommand = new PayCommand(this);
      Objects.requireNonNull(this.getCommand("pay")).setExecutor(payCommand);
      Objects.requireNonNull(this.getCommand("pay")).setTabCompleter(payCommand);
      Objects.requireNonNull(this.getCommand("game")).setExecutor(new GameCommand(this));
      Objects.requireNonNull(this.getCommand("guide")).setExecutor(new GuideCommand());
      PvPCommand pvpCommand = new PvPCommand(this);
      Objects.requireNonNull(this.getCommand("pvp")).setExecutor(pvpCommand);
      Objects.requireNonNull(this.getCommand("pvp")).setTabCompleter(pvpCommand);
      Objects.requireNonNull(this.getCommand("esicraftadmin")).setExecutor(new EsicraftAdminCommand(this));
      Objects.requireNonNull(this.getCommand("hub")).setExecutor(new HubCommand(this));
      Objects.requireNonNull(this.getCommand("contrat")).setExecutor(new ContractCommand(this));
      SeasonCommand seasonCommand = new SeasonCommand(this);
      Objects.requireNonNull(this.getCommand("classement")).setExecutor(seasonCommand);
      Objects.requireNonNull(this.getCommand("histoire")).setExecutor(seasonCommand);
      Objects.requireNonNull(this.getCommand("station")).setExecutor(new StationCommand(this));
      Objects.requireNonNull(this.getCommand("rail")).setExecutor(new RailCommand(this));
      Objects.requireNonNull(this.getCommand("traite")).setExecutor(new TreatyCommand(this));
      LifeCommand lifeCommand = new LifeCommand(this);
      for (String name : new String[]{"journal", "succes", "climat", "aide", "deguisement"}) {
         Objects.requireNonNull(this.getCommand(name)).setExecutor(lifeCommand);
         Objects.requireNonNull(this.getCommand(name)).setTabCompleter(lifeCommand);
      }

      FunCommand funCommand = new FunCommand(this);
      Objects.requireNonNull(this.getCommand("hud")).setExecutor(funCommand);
      Objects.requireNonNull(this.getCommand("evenement")).setExecutor(funCommand);
      Objects.requireNonNull(this.getCommand("evenement")).setTabCompleter(funCommand);
   }

   public boolean isNexusDestructionEnabled() {
      return this.nexusDestructionEnabled;
   }

   public void setNexusDestructionEnabled(boolean enabled) {
      this.nexusDestructionEnabled = enabled;
   }

   public Chronicle getChronicle() {
      return this.chronicle;
   }

   public AchievementManager getAchievements() {
      return this.achievements;
   }

   public NatureSeasonManager getNatureSeasons() {
      return this.natureSeasons;
   }

   public GuildSynergy getSynergy() {
      return this.synergy;
   }

   public GuildContractManager getGuildContracts() {
      return this.guildContracts;
   }

   public HelpCallManager getHelpCalls() {
      return this.helpCalls;
   }

   public CosmeticManager getCosmetics() {
      return this.cosmetics;
   }

   public AgeDimensionListener getAgeDimension() {
      return this.ageDimension;
   }

   public BalanceManager getBalance() {
      return this.balance;
   }

   public LoanManager getLoans() {
      return this.loans;
   }

   public ReputationManager getReputation() {
      return this.reputation;
   }

   public MentorManager getMentors() {
      return this.mentors;
   }

   public RansomManager getRansom() {
      return this.ransom;
   }

   public CombatManager getCombatManager() {
      return this.combatManager;
   }

   public DisguiseManager getDisguiseManager() {
      return this.disguiseManager;
   }

   public static EsicraftPlugin getInstance() {
      return instance;
   }

   public DataManager getDataManager() {
      return this.dataManager;
   }

   public AgeManager getAgeManager() {
      return this.ageManager;
   }

   public ContractManager getContractManager() {
      return this.contractManager;
   }

   public SeasonManager getSeasonManager() {
      return this.seasonManager;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public TreatyManager getTreatyManager() {
      return this.treatyManager;
   }

   public RailManager getRailManager() {
      return this.railManager;
   }

   public HudManager getHudManager() {
      return this.hudManager;
   }

   public RaceManager getRaceManager() {
      return this.raceManager;
   }

   public GuildManager getGuildManager() {
      return this.guildManager;
   }

   public NexusManager getNexusManager() {
      return this.nexusManager;
   }

   public RaidManager getRaidManager() {
      return this.raidManager;
   }

   public MarketManager getMarketManager() {
      return this.marketManager;
   }

   public AntiFarmManager getAntiFarmManager() {
      return this.antiFarmManager;
   }

   public ClaimVisualizerManager getClaimVisualizerManager() {
      return this.claimVisualizerManager;
   }
}
