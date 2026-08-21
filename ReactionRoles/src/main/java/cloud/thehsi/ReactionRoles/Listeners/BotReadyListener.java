package cloud.thehsi.ReactionRoles.Listeners;

import cloud.thehsi.ComitasBotJ.API.Event.EventHandler;
import cloud.thehsi.ComitasBotJ.API.Event.EventPriority;
import cloud.thehsi.ComitasBotJ.API.Event.Events.BotReadyEvent;
import cloud.thehsi.ComitasBotJ.API.Event.Listener;
import cloud.thehsi.ReactionRoles.Main;

public record BotReadyListener(Main plugin) implements Listener {
    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOW)
    public void onBotReady(BotReadyEvent event) {
        try {
            plugin.ensureReactionRoleMessage();
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize the reaction-role message.", e);
        }
    }
}