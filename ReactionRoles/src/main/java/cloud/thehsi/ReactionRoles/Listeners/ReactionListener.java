package cloud.thehsi.ReactionRoles.Listeners;

import cloud.thehsi.ComitasBotJ.API.Bot.Comitas;
import cloud.thehsi.ComitasBotJ.API.Discord.Role.Role;
import cloud.thehsi.ComitasBotJ.API.Event.EventHandler;
import cloud.thehsi.ComitasBotJ.API.Event.EventPriority;
import cloud.thehsi.ComitasBotJ.API.Event.Events.ReactionUpdatedEvent;
import cloud.thehsi.ComitasBotJ.API.Event.Listener;
import cloud.thehsi.ComitasBotJ.API.Plugin.PersistentData.PersistentDataStorage;
import cloud.thehsi.ComitasBotJ.API.Plugin.PersistentData.PersistentDataTypes;
import cloud.thehsi.ComitasBotJ.API.Plugin.Plugin;
import cloud.thehsi.ReactionRoles.Main;

public record ReactionListener(Main plugin) implements Listener {
   @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onReaction(ReactionUpdatedEvent event) {
        // Do not react to our own reaction events.
        if (Comitas.getBot().isMeOrNull(event.member())) {
            return;
        }

        PersistentDataStorage storage = Plugin.getPersistentDataStorage();
        if (!storage.has(Main.MESSAGE_ID_KEY, PersistentDataTypes.LONG)) {
            return;
        }

        Long configuredMessageId = storage.get(
                Main.MESSAGE_ID_KEY,
                PersistentDataTypes.LONG
        );

        if (configuredMessageId == null || event.message().getId() != configuredMessageId) {
            return;
        }

        String emoji = event.reaction().getEmoji().getName();
        Long roleId = plugin.getRoleIds().get(emoji);
        if (roleId == null) {
            return;
        }

        Role role = event.member().getGuild().getRoleById(roleId);
        if (role == null) {
            plugin.getLogger().warn("Configured role {} does not exist in guild {}.",
                    roleId, event.member().getGuild().getId());
            return;
        }

        if (event.reactionAction().isIncrease()) {
            event.member().addRole(role);
        } else if (event.reactionAction().isDecrease()) {
            event.member().removeRole(role);
        }
    }
}