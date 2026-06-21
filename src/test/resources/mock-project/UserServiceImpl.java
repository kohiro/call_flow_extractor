package mock;

import org.springframework.transaction.annotation.Transactional;

public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserDetails(String id) {
        String name = userMapper.findNameById(id);
        String email = userMapper.findEmailById(id); // To test multiple calls
        return name + " (" + email + ")";
    }

    @Override
    @Transactional
    public User updateUser(UserRequest request) {
        return userMapper.updateUser(request);
    }
}
