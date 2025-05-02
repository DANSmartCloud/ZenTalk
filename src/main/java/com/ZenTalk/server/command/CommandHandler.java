package com.ZenTalk.server.command;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.server.http.AppHttpServer;
import com.ZenTalk.server.user.PasswordResetHandler;
import com.ZenTalk.server.command.FriendCommand; // Import FriendCommand
import com.ZenTalk.server.command.UserCommand;   // Import UserCommand
import com.ZenTalk.server.command.SettingsCommand; // Import SettingsCommand
import com.ZenTalk.server.util.LogUtil;
import com.ZenTalk.server.util.MessageBundle;
import com.ZenTalk.model.User; // Added import for User
import java.util.Optional; // Added import for Optional
import java.util.Scanner;

public class CommandHandler {
    private final DatabaseService dbService;
    private final AppHttpServer httpServer;
    private final BanUserCommand banCommand;
    private final FreezeUserCommand freezeCommand;
    private final CreateUserCommand createCommand;
    private final FriendCommand friendCommand;     // Add FriendCommand instance
    private final UserCommand userCommand;       // Add UserCommand instance
    private final SettingsCommand settingsCommand; // Add SettingsCommand instance
    private final PasswordResetHandler passwordResetHandler;

    public CommandHandler(DatabaseService dbService, AppHttpServer httpServer) {
        this.dbService = dbService;
        this.httpServer = httpServer;
        this.banCommand = new BanUserCommand(dbService);
        this.freezeCommand = new FreezeUserCommand(dbService);
        this.createCommand = new CreateUserCommand(dbService);
        this.passwordResetHandler = new PasswordResetHandler(dbService);
        this.friendCommand = new FriendCommand(dbService);         // Initialize FriendCommand
        this.userCommand = new UserCommand(dbService);           // Initialize UserCommand
        this.settingsCommand = new SettingsCommand(dbService);     // Initialize SettingsCommand
    }

    public void listen() {
        log(MessageBundle.getMessage("command.console.start"));
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("> ");
                String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
                if (line.isEmpty()) {
                    continue;
                }

                // Split into command, potential subcommand, and the rest
                // parts[0] = command, parts[1] = subcommand or first arg, parts[2] = remaining args
                String[] parts = line.split("\\s+", 3);
                String command = parts[0].toLowerCase();
                String subCommand = parts.length > 1 ? parts[1] : ""; // Potential subcommand
                // Arguments for commands with subcommands (like 'friend request ...')
                String[] commandArgs = parts.length > 2 ? parts[2].split("\\s+") : new String[0];
                // Full argument string for original commands (like 'ban username')
                String fullArgs = parts.length > 1 ? line.substring(parts[0].length()).trim() : "";

                switch (command) {
                    case "stop":
                        log(MessageBundle.getMessage("command.console.stop"));
                        scanner.close(); // Close scanner before exiting
                        System.exit(0);
                        break; // Exit case
                    case "help":
                        log("\n" + MessageBundle.getMessage("command.console.help.title"));
                        log(MessageBundle.getMessage("command.console.help.stop"));
                        log(MessageBundle.getMessage("command.console.help.help"));
                        log(MessageBundle.getMessage("command.console.help.ban"));
                        log(MessageBundle.getMessage("command.console.help.unban"));
                        log(MessageBundle.getMessage("command.console.help.freeze"));
                        log(MessageBundle.getMessage("command.console.help.unfreeze"));
                        log(MessageBundle.getMessage("command.console.help.deleteuser")); // Corrected help message key
                        log(MessageBundle.getMessage("command.console.help.create"));
                        log(MessageBundle.getMessage("command.console.help.sendcode"));
                        log(MessageBundle.getMessage("command.console.help.resetpwd"));
                        log(MessageBundle.getMessage("command.console.help.friend")); // Add help for friend command
                        log(MessageBundle.getMessage("command.console.help.user"));   // Add help for user command
                        log(MessageBundle.getMessage("command.console.help.settings"));// Add help for settings command
                        // log("");  // Removed extra empty line
                        break;
                    case "ban":
                        if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.ban.usage")); // Add usage message
                            continue;
                        }
                        boolean banSuccess = banCommand.execute(fullArgs);
                        log(banSuccess ?
                            MessageBundle.getMessage("command.ban.success", fullArgs) :
                            MessageBundle.getMessage("command.ban.failure", fullArgs));
                        break;
                    case "unban":
                         if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.unban.usage")); // Add usage message
                            continue;
                        }
                        boolean unbanSuccess = banCommand.unban(fullArgs);
                        log(unbanSuccess ?
                            MessageBundle.getMessage("command.unban.success", fullArgs) :
                            MessageBundle.getMessage("command.unban.failure", fullArgs));
                        break;
                    case "freeze":
                        if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.freeze.usage")); // Add usage message
                            continue;
                        }
                        boolean freezeSuccess = freezeCommand.execute(fullArgs);
                        log(freezeSuccess ?
                            MessageBundle.getMessage("command.freeze.success", fullArgs) :
                            MessageBundle.getMessage("command.freeze.failure", fullArgs));
                        break;
                    case "unfreeze":
                        if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.unfreeze.usage")); // Add usage message
                            continue;
                        }
                        boolean unfreezeSuccess = freezeCommand.unfreeze(fullArgs);
                        log(unfreezeSuccess ?
                            MessageBundle.getMessage("command.unfreeze.success", fullArgs) :
                            MessageBundle.getMessage("command.unfreeze.failure", fullArgs));
                        break;
                    case "create":
                        String[] createParts = fullArgs.split("\\s+");
                        if (createParts.length < 4) {
                            log(MessageBundle.getMessage("command.create.usage"));
                            continue;
                        }
                        String createResult = createCommand.execute(createParts[0], createParts[1], createParts[2], createParts[3]);
                        log(createResult);
                        break;
                    // Removed 'delete' case as it seemed incorrect, using 'deleteuser' instead
                    case "sendcode":
                         if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.sendcode.usage")); // Add usage message
                            continue;
                        }
                        boolean sendCodeSuccess = passwordResetHandler.sendVerificationCode(fullArgs);
                        log(sendCodeSuccess ?
                            MessageBundle.getMessage("command.sendcode.success", fullArgs) :
                            MessageBundle.getMessage("command.sendcode.failure", fullArgs));
                        break;
                    case "resetpwd":
                        String[] resetParts = fullArgs.split("\\s+");
                        if (resetParts.length < 3) {
                            log(MessageBundle.getMessage("command.resetpwd.usage"));
                            continue;
                        }
                        boolean resetSuccess = passwordResetHandler.resetPassword(resetParts[0], resetParts[1], resetParts[2]);
                        log(resetSuccess ?
                            MessageBundle.getMessage("command.resetpwd.success", resetParts[0]) :
                            MessageBundle.getMessage("command.resetpwd.failure", resetParts[0]));
                        break;
                    case "deleteuser":
                        if (fullArgs.isEmpty()) {
                            log(MessageBundle.getMessage("command.deleteuser.usage"));
                            continue;
                        }
                        Optional<User> userToDelete = dbService.getUserRepository().findByUsername(fullArgs);
                        if (!userToDelete.isPresent()) {
                            // Try finding by ID if not found by username
                            userToDelete = dbService.getUserRepository().findById(fullArgs);
                        }

                        if (userToDelete.isPresent()) {
                            dbService.getUserRepository().deleteById(userToDelete.get().getId());
                            log(MessageBundle.getMessage("command.deleteuser.success", fullArgs));
                        } else {
                            log(MessageBundle.getMessage("command.deleteuser.failure", fullArgs));
                        }
                        break;
                    case "friend":
                        String friendResult = friendCommand.execute(subCommand, commandArgs);
                        log(friendResult);
                        break;
                    case "user":
                        String userResult = userCommand.execute(subCommand, commandArgs);
                        log(userResult);
                        break;
                    case "settings":
                        String settingsResult = settingsCommand.execute(subCommand, commandArgs);
                        log(settingsResult);
                        break;
                    default:
                        log(MessageBundle.getMessage("command.console.unknown", line));
                        break;
                }
            } catch (Exception e) {
                LogUtil.error(CommandHandler.class, MessageBundle.getMessage("command.console.error", e.getMessage()), e); // Log the exception stack trace
                // Consider adding more specific error handling or logging
            }
        }
        // Scanner is closed in the 'stop' case before exiting
    }

    private void log(String message) {
        LogUtil.info(CommandHandler.class, message);
    }
}
