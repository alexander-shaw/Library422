package org.cpts422.repositories;

import org.cpts422.models.User;

import java.util.HashMap;
import java.util.Map;

public class InMemoryUserRepository implements UserRepository {
    private final Map<Integer, User> users = new HashMap<>();

    @Override
    public User findUserById(int userId) {
        return users.get(userId);
    }

    @Override
    public void addUser(User user) {
        users.put(user.userId(), user);
    }
}
