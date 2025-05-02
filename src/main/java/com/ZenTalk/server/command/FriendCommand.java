package com.ZenTalk.server.command;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.friend.FriendRequestHandler;
import com.ZenTalk.server.friend.FriendSearchHandler;
import com.ZenTalk.server.util.MessageBundle;
import org.bson.Document;

import java.util.List;

public class FriendCommand {
    private final FriendRequestHandler requestHandler;
    private final FriendSearchHandler searchHandler;
    // TODO: Add FriendListHandler if needed

    public FriendCommand(DatabaseService dbService) {
        this.requestHandler = new FriendRequestHandler(dbService);
        this.searchHandler = new FriendSearchHandler(dbService);
    }

    public String execute(String subCommand, String[] args) {
        switch (subCommand.toLowerCase()) {
            case "request":
                // Usage: friend request <fromUser> <toUser> [message]
                if (args.length < 2) return MessageBundle.getMessage("command.friend.request.usage");
                String message = args.length > 2 ? args[2] : "";
                boolean reqSuccess = requestHandler.requestFriend(args[0], args[1], message);
                return reqSuccess ? MessageBundle.getMessage("command.friend.request.success", args[0], args[1])
                                  : MessageBundle.getMessage("command.friend.request.failure");
            case "accept":
                // Usage: friend accept <requestId>
                if (args.length < 1) return MessageBundle.getMessage("command.friend.accept.usage");
                boolean acceptSuccess = requestHandler.verifyFriend(args[0], true);
                return acceptSuccess ? MessageBundle.getMessage("command.friend.accept.success", args[0])
                                     : MessageBundle.getMessage("command.friend.accept.failure", args[0]);
            case "reject":
                // Usage: friend reject <requestId>
                if (args.length < 1) return MessageBundle.getMessage("command.friend.reject.usage");
                boolean rejectSuccess = requestHandler.verifyFriend(args[0], false);
                return rejectSuccess ? MessageBundle.getMessage("command.friend.reject.success", args[0])
                                     : MessageBundle.getMessage("command.friend.reject.failure", args[0]);
            case "delete":
                // Usage: friend delete <user> <friendId>
                if (args.length < 2) return MessageBundle.getMessage("command.friend.delete.usage");
                boolean delSuccess = requestHandler.deleteFriend(args[0], args[1]);
                return delSuccess ? MessageBundle.getMessage("command.friend.delete.success", args[1], args[0])
                                  : MessageBundle.getMessage("command.friend.delete.failure", args[1], args[0]);
            case "remark":
                // Usage: friend remark <user> <friendId> <remark>
                if (args.length < 3) return MessageBundle.getMessage("command.friend.remark.usage");
                boolean remarkSuccess = requestHandler.updateRemark(args[0], args[1], args[2]);
                return remarkSuccess ? MessageBundle.getMessage("command.friend.remark.success", args[1], args[0])
                                     : MessageBundle.getMessage("command.friend.remark.failure", args[1], args[0]);
            case "search":
                // Usage: friend search <keyword>
                if (args.length < 1) return MessageBundle.getMessage("command.friend.search.usage");
                List<Document> users = searchHandler.searchUser(args[0]);
                if (users.isEmpty()) {
                    return MessageBundle.getMessage("command.friend.search.notfound", args[0]);
                }
                StringBuilder result = new StringBuilder(MessageBundle.getMessage("command.friend.search.results", args[0]) + "\n");
                for (Document user : users) {
                    result.append(" - ID: ").append(user.getString("userId")) // Assuming userId exists
                          .append(", Nickname: ").append(user.getString("nickname"))
                          .append(", Email: ").append(user.getString("email"))
                          .append(", Phone: ").append(user.getString("phone"))
                          .append("\n");
                }
                return result.toString();
            // case "list":
                // TODO: Implement list friends functionality
                // return MessageBundle.getMessage("command.friend.list.usage");
            default:
                return MessageBundle.getMessage("command.friend.unknown", subCommand);
        }
    }
}