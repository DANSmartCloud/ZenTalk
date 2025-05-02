package com.ZenTalk.server.command;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.user.ProfileHandler;
import com.ZenTalk.server.user.UserStatusHandler;
import com.ZenTalk.server.util.MessageBundle;
import org.bson.Document;

public class UserCommand {
    private final ProfileHandler profileHandler;
    private final UserStatusHandler statusHandler;

    public UserCommand(DatabaseService dbService) {
        this.profileHandler = new ProfileHandler(dbService);
        this.statusHandler = new UserStatusHandler(dbService);
    }

    public String execute(String subCommand, String[] args) {
        switch (subCommand.toLowerCase()) {
            case "profile":
                // Usage: user profile <userId>
                if (args.length < 1) return MessageBundle.getMessage("command.user.profile.usage");
                Document profile = profileHandler.viewProfile(args[0]);
                if (profile == null) {
                    return MessageBundle.getMessage("command.user.profile.notfound", args[0]);
                }
                // Format profile information for display
                StringBuilder profileInfo = new StringBuilder(MessageBundle.getMessage("command.user.profile.details", args[0]) + "\n");
                profileInfo.append("  Nickname: ").append(profile.getString("nickname")).append("\n");
                profileInfo.append("  Avatar: ").append(profile.getString("avatar")).append("\n");
                profileInfo.append("  Gender: ").append(profile.getString("gender")).append("\n");
                profileInfo.append("  Birthday: ").append(profile.getString("birthday")).append("\n");
                profileInfo.append("  Tags: ").append(profile.get("tags")); // Assuming tags is a List or similar
                return profileInfo.toString();

            case "editprofile":
                // Usage: user editprofile <userId> <nickname> <avatar> <gender> <birthday>
                // Note: Use "null" for fields you don't want to change.
                if (args.length < 5) return MessageBundle.getMessage("command.user.editprofile.usage");
                String nickname = "null".equalsIgnoreCase(args[1]) ? null : args[1];
                String avatar = "null".equalsIgnoreCase(args[2]) ? null : args[2];
                String gender = "null".equalsIgnoreCase(args[3]) ? null : args[3];
                String birthday = "null".equalsIgnoreCase(args[4]) ? null : args[4];
                boolean editSuccess = profileHandler.editProfile(args[0], nickname, avatar, gender, birthday);
                return editSuccess ? MessageBundle.getMessage("command.user.editprofile.success", args[0])
                                   : MessageBundle.getMessage("command.user.editprofile.failure", args[0]);

            case "setonline":
                // Usage: user setonline <userId> <true|false>
                if (args.length < 2) return MessageBundle.getMessage("command.user.setonline.usage");
                boolean online = Boolean.parseBoolean(args[1]);
                statusHandler.setOnline(args[0], online);
                // Setting status usually doesn't have a direct failure case in this implementation
                return MessageBundle.getMessage("command.user.setonline.success", args[0], online);

            case "isonline":
                 // Usage: user isonline <userId>
                if (args.length < 1) return MessageBundle.getMessage("command.user.isonline.usage");
                boolean isOnline = statusHandler.isOnline(args[0]);
                return MessageBundle.getMessage(isOnline ? "command.user.isonline.online" : "command.user.isonline.offline", args[0]);

            default:
                return MessageBundle.getMessage("command.user.unknown", subCommand);
        }
    }
}