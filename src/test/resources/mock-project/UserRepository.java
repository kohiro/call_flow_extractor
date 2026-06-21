package mock;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    User updateUser(UserRequest request);
}
