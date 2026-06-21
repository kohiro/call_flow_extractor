package mock;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Optional<User> findById(String id) {
        return userMapper.findById(id);
    }

    @Override
    public User updateUser(UserRequest request) {
        return userMapper.updateUser(request);
    }
}
