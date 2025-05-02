package com.ZenTalk.server.command;

import com.ZenTalk.server.util.MessageBundle;

import com.ZenTalk.server.db.DatabaseService;
import com.ZenTalk.service.UserService;

public class CreateUserCommand {
    private final UserService userService;

    public CreateUserCommand(DatabaseService dbService) {
        this.userService = new UserService(dbService.getUserRepository(), dbService.getJedisPool()); // Pass JedisPool
    }

    public String execute(String username, String password, String email, String phone) {
        try {
            userService.createUser(username, password, email, phone);
            return MessageBundle.getMessage("command.create.success", username);
        } catch (Exception e) {
            return MessageBundle.getMessage("command.create.failure", e.getMessage());
        }
    }
}
