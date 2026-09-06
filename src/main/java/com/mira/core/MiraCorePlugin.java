package com.mira.core;

import com.mira.core.api.*;
import com.mira.core.listener.CoreBossBarListener;
import com.mira.core.listener.CoreMaintenanceListener;
import com.mira.core.listener.CoreMotdListener;
import com.mira.core.listener.CoreProfileListener;
import com.mira.core.listener.CoreRewardGuiListener;
import com.mira.core.gui.CoreRewardGui;
import com.mira.core.service.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraCorePlugin extends JavaPlugin {
    private CoreMessageService messageService;
    private CoreServiceRegistry serviceRegistry;
    private CoreCooldownService cooldownService;
    private CoreModuleRegistry moduleRegistry;
    private CorePlayerProfileService profileService;
    private CoreNotificationService notificationService;
    private CoreAuditService auditService;
    private CorePaginationService paginationService;
    private CorePermissionDebugService permissionDebugService;
    private CoreMilestoneService milestoneService;
    private CoreBossBarService bossBarService;
    private CoreMaintenanceService maintenanceService;
    private CoreMotdService motdService;
    private CoreUpdateService updateService;
    private CoreRewardService rewardService;
    private CoreRewardGui rewardGui;
    private CoreEssentialsPresentationService essentialsPresentation;
    private CoreStarterGuideService starterGuideService;
    private MiraCoreApiImpl api;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageService = new CoreMessageService(this);
        serviceRegistry = new CoreServiceRegistry();
        cooldownService = new CoreCooldownService();
        moduleRegistry = new CoreModuleRegistry();
        profileService = new CorePlayerProfileService(this);
        notificationService = new CoreNotificationService();
        auditService = new CoreAuditService(this);
        paginationService = new CorePaginationService();
        permissionDebugService = new CorePermissionDebugService();
        milestoneService = new CoreMilestoneService(this);
        bossBarService = new CoreBossBarService();
        maintenanceService = new CoreMaintenanceService(this, auditService);
        motdService = new CoreMotdService(this, maintenanceService);
        updateService = new CoreUpdateService(this, moduleRegistry);
        rewardService = new CoreRewardService(this, auditService);
        rewardGui = new CoreRewardGui(rewardService, messageService);
        essentialsPresentation = new CoreEssentialsPresentationService(this);
        starterGuideService = new CoreStarterGuideService(this, messageService);

        api = new MiraCoreApiImpl(getPluginMeta().getVersion(), messageService, serviceRegistry, cooldownService, moduleRegistry,
                profileService, notificationService, auditService, paginationService, permissionDebugService, milestoneService,
                bossBarService, maintenanceService, updateService, rewardService);
        CoreDiagnostics diagnostics = new CoreDiagnostics(this, api, serviceRegistry, cooldownService, moduleRegistry);
        api.diagnostics(diagnostics);

        getServer().getServicesManager().register(MiraCore.class, api, this, ServicePriority.Normal);
        serviceRegistry.register(MiraCore.class, api);
        serviceRegistry.register(PlayerProfileService.class, profileService);
        serviceRegistry.register(NotificationService.class, notificationService);
        serviceRegistry.register(AuditService.class, auditService);
        serviceRegistry.register(PaginationService.class, paginationService);
        serviceRegistry.register(PermissionDebugService.class, permissionDebugService);
        serviceRegistry.register(MilestoneService.class, milestoneService);
        serviceRegistry.register(BossBarService.class, bossBarService);
        serviceRegistry.register(MaintenanceService.class, maintenanceService);
        serviceRegistry.register(UpdateService.class, updateService);
        serviceRegistry.register(RewardService.class, rewardService);
        moduleRegistry.register(this, "MiraCore");

        getServer().getPluginManager().registerEvents(new CoreProfileListener(profileService), this);
        getServer().getPluginManager().registerEvents(new CoreBossBarListener(bossBarService), this);
        getServer().getPluginManager().registerEvents(new CoreMaintenanceListener(maintenanceService), this);
        getServer().getPluginManager().registerEvents(new CoreMotdListener(motdService), this);
        getServer().getPluginManager().registerEvents(new CoreRewardGuiListener(rewardService, messageService, rewardGui), this);
        getServer().getPluginManager().registerEvents(starterGuideService, this);
        getServer().getScheduler().runTaskTimer(this, maintenanceService::tick, 20L, 20L);
        for (Player player : getServer().getOnlinePlayers()) profileService.touch(player.getUniqueId(), player.getName(), true);

        MiraCoreCommand executor = new MiraCoreCommand(this);
        PluginCommand command = getCommand("miracore");
        if (command == null) throw new IllegalStateException("miracore command is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        CorePlayerRewardCommand playerRewards = new CorePlayerRewardCommand(rewardService, messageService, rewardGui);
        PluginCommand rewardsCommand = getCommand("rewards");
        PluginCommand claimCommand = getCommand("claim");
        if (rewardsCommand == null || claimCommand == null) throw new IllegalStateException("reward commands are missing from plugin.yml");
        rewardsCommand.setExecutor(playerRewards);
        claimCommand.setExecutor(playerRewards);

        PluginCommand guidesCommand = getCommand("guides");
        if (guidesCommand == null) throw new IllegalStateException("guides command is missing from plugin.yml");
        guidesCommand.setExecutor(starterGuideService);

        if (getServer().getPluginManager().getPlugin("MOTD") != null) {
            getLogger().warning("A separate plugin named MOTD is installed. MiraCore now owns the server-list MOTD; remove the old MOTD JAR to avoid competing ping listeners.");
        }

        getServer().getScheduler().runTask(this, () -> {
            if (essentialsPresentation.sync(true)) {
                getLogger().info("EssentialsX presentation bridge ready: " + essentialsPresentation.status());
            }
        });

        getLogger().info("MiraCore v" + api.version() + " enabled. Shared suite services registered.");

        if (getConfig().getBoolean("diagnostics.startup-check", true)) {
            getServer().getScheduler().runTask(this, () -> {
                DiagnosticReport report = api.runDiagnostics();
                if (report.passed()) {
                    getLogger().info("Startup diagnostics passed " + report.passedCount() + "/" + report.checks().size() + " checks.");
                } else {
                    getLogger().warning("Startup diagnostics failed: " + report.passedCount() + "/" + report.checks().size() + " checks passed. Run /miracore test for details.");
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (api != null) getServer().getServicesManager().unregister(MiraCore.class, api);
        if (serviceRegistry != null && api != null) serviceRegistry.unregister(MiraCore.class, api);
        if (cooldownService != null) cooldownService.clearAll();
        if (bossBarService != null) bossBarService.shutdown();
        if (rewardService != null) rewardService.saveAll();
        if (moduleRegistry != null) moduleRegistry.unregister(this);
        if (profileService != null) {
            for (Player player : getServer().getOnlinePlayers()) profileService.touch(player.getUniqueId(), player.getName(), false);
        }
    }

    public MiraCoreApiImpl api() { return api; }
    public MessageService messages() { return messageService; }
    public MaintenanceService maintenance() { return maintenanceService; }
    public UpdateService updates() { return updateService; }
    public RewardService rewards() { return rewardService; }
    public CoreEssentialsPresentationService essentialsPresentation() { return essentialsPresentation; }
    public void reloadCoreConfiguration() {
        reloadConfig();
        messageService.reload();
        if (maintenanceService != null) maintenanceService.reload();
        if (motdService != null) motdService.reload();
        if (rewardService != null) rewardService.reloadClaimCodes();
        if (essentialsPresentation != null) essentialsPresentation.sync(true);
    }
}
