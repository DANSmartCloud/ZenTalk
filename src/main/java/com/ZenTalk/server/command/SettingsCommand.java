package com.ZenTalk.server.command;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.user.SettingsHandler;
import com.ZenTalk.server.util.MessageBundle;

public class SettingsCommand {
    private final SettingsHandler settingsHandler;

    public SettingsCommand(DatabaseService dbService) {
        this.settingsHandler = new SettingsHandler(dbService);
    }

    public String execute(String subCommand, String[] args) {
        switch (subCommand.toLowerCase()) {
            case "avatar":
                // Usage: settings avatar <userId> <avatarUrl>
                if (args.length < 2) return MessageBundle.getMessage("command.settings.avatar.usage");
                boolean avatarSuccess = settingsHandler.updateAvatar(args[0], args[1]);
                return avatarSuccess ? MessageBundle.getMessage("command.settings.avatar.success", args[0])
                                     : MessageBundle.getMessage("command.settings.avatar.failure", args[0]);
            case "invisible":
                // Usage: settings invisible <userId> <true|false>
                if (args.length < 2) return MessageBundle.getMessage("command.settings.invisible.usage");
                boolean invisible = Boolean.parseBoolean(args[1]);
                boolean invisibleSuccess = settingsHandler.setInvisible(args[0], invisible);
                return invisibleSuccess ? MessageBundle.getMessage("command.settings.invisible.success", args[0], invisible)
                                        : MessageBundle.getMessage("command.settings.invisible.failure", args[0]);
            case "tags":
                // Usage: settings tags <userId> <tag1> [tag2] ...
                if (args.length < 2) return MessageBundle.getMessage("command.settings.tags.usage");
                String[] tags = java.util.Arrays.copyOfRange(args, 1, args.length);
                boolean tagsSuccess = settingsHandler.updateTags(args[0], tags);
                return tagsSuccess ? MessageBundle.getMessage("command.settings.tags.success", args[0])
                                   : MessageBundle.getMessage("command.settings.tags.failure", args[0]);
            case "changepwd":
                // Usage: settings changepwd <userId> <oldPassword> <newPassword>
                if (args.length < 3) return MessageBundle.getMessage("command.settings.changepwd.usage");
                boolean changePwdSuccess = settingsHandler.changePassword(args[0], args[1], args[2]);
                return changePwdSuccess ? MessageBundle.getMessage("command.settings.changepwd.success", args[0])
                                        : MessageBundle.getMessage("command.settings.changepwd.failure", args[0]);
            case "nightmode":
                // Usage: settings nightmode <userId> <true|false>
                if (args.length < 2) return MessageBundle.getMessage("command.settings.nightmode.usage");
                boolean nightMode = Boolean.parseBoolean(args[1]);
                boolean nightModeSuccess = settingsHandler.setNightMode(args[0], nightMode);
                return nightModeSuccess ? MessageBundle.getMessage("command.settings.nightmode.success", args[0], nightMode)
                                        : MessageBundle.getMessage("command.settings.nightmode.failure", args[0]);
            default:
                return MessageBundle.getMessage("command.settings.unknown", subCommand);
        }
    }
}