package org.cpts422.repositories;

import org.cpts422.models.User;

public interface UserRepository {
    User findUserById(int userId);
    void addUser(User user);
}
