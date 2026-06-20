package mock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
}
