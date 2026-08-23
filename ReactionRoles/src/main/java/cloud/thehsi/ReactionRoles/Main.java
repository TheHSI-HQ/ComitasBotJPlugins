package cloud.thehsi.ReactionRoles;

import cloud.thehsi.ComitasBotJ.API.Bot.Comitas;
import cloud.thehsi.ComitasBotJ.API.Discord.Channel.Channel;
import cloud.thehsi.ComitasBotJ.API.Discord.Channel.MessageChannel;
import cloud.thehsi.ComitasBotJ.API.Discord.Emoji.Emoji;
import cloud.thehsi.ComitasBotJ.API.Discord.Guild.Guild;
import cloud.thehsi.ComitasBotJ.API.Discord.Message.Components.Component;
import cloud.thehsi.ComitasBotJ.API.Discord.Message.Message;
import cloud.thehsi.ComitasBotJ.API.Discord.Message.MyMessage;
import cloud.thehsi.ComitasBotJ.API.Plugin.PersistentData.PersistentDataStorage;
import cloud.thehsi.ComitasBotJ.API.Plugin.PersistentData.PersistentDataTypes;
import cloud.thehsi.ComitasBotJ.API.Plugin.Plugin;
import cloud.thehsi.ReactionRoles.Listeners.BotReadyListener;
import cloud.thehsi.ReactionRoles.Listeners.ReactionListener;
import cloud.thehsi.ReactionRoles.Utils.ConfigUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class Main extends Plugin {

    public static final String MESSAGE_ID_KEY = "reaction-role-message-id";

    private final Map<String, Long> roleIds = new LinkedHashMap<>();
    private Properties config;

    @Override
    public void onEnable() {
        Path configPath = Path.of("plugins", "ReactionRoles", "reaction-roles.properties");
        this.config = ConfigUtils.loadOrCreateConfig(configPath, getLogger());

        loadRoleMappings();

        // Register listeners
        Comitas.getPluginManager().registerEvents(this, new BotReadyListener(this));
        Comitas.getPluginManager().registerEvents(this, new ReactionListener(this));

        getLogger().info("ReactionRoles enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReactionRoles disabled.");
    }

    public Map<String, Long> getRoleIds() {
        return roleIds;
    }

    public void ensureReactionRoleMessage() {
        long guildId = ConfigUtils.getRequiredLong(config, "guild-id");
        long channelId = ConfigUtils.getRequiredLong(config, "channel-id");

        Guild guild = Comitas.getBot().getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException("Guild " + guildId + " was not found.");
        }

        Channel channel = guild.getChannelById(channelId);
        if (!(channel instanceof MessageChannel messageChannel)) {
            throw new IllegalStateException("Channel " + channelId + " is not a message channel: " + channel);
        }

        PersistentDataStorage storage = getPersistentDataStorage();
        Message existing = null;

        if (storage.has(MESSAGE_ID_KEY, PersistentDataTypes.LONG)) {
            Long savedId = storage.get(MESSAGE_ID_KEY, PersistentDataTypes.LONG);
            assert savedId != null;
            try {
                existing = messageChannel.getMessageById(savedId);
            } catch (RuntimeException ex) {
                getLogger().warn("Could not retrieve saved reaction-role message {}; recreating it.", savedId);
                getLogger().error("", ex);
            }
        }

        if (existing != null && !existing.isDeleted()) {
            getLogger().info("Reusing reaction-role message {}.", existing.getId());
            ensureConfiguredReactions(existing);
            return;
        }

        MyMessage created = messageChannel.sendMessage(
                Component.raw(config.getProperty("message-content", "React below!")
                        .replace("\\n", "\n"))
        );

        storage.set(
                MESSAGE_ID_KEY,
                PersistentDataTypes.LONG,
                created.getId()
        );

        getLogger().info("Created reaction-role message {}.", created.getId());
        ensureConfiguredReactions(created);
    }

    private void ensureConfiguredReactions(Message message) {
        for (String emojiText : roleIds.keySet()) {
            Emoji emoji = Emoji.fromUnicode(emojiText);
            if (emoji == null) {
                getLogger().warn("Could not resolve configured Unicode emoji '{}'.", emojiText);
                continue;
            }
            message.react(emoji);
        }
    }

    private void loadRoleMappings() {
        roleIds.clear();

        for (String key : config.stringPropertyNames()) {
            if (!key.startsWith("role.")) {
                continue;
            }

            String emoji = key.substring("role.".length());
            if (emoji.isBlank()) {
                continue;
            }

            try {
                roleIds.put(emoji, Long.parseLong(config.getProperty(key).trim()));
            } catch (NumberFormatException ex) {
                getLogger().warn(
                        "Ignoring invalid role ID for emoji '{}': {}",
                        emoji,
                        config.getProperty(key)
                );
            }
        }

        if (roleIds.isEmpty()) {
            throw new IllegalStateException("No role.* mappings are configured.");
        }
    }
}