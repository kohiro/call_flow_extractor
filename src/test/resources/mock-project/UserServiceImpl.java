package mock;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getUserDetails(String id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null);
    }

    @Override
    @Transactional
    public User updateUser(UserRequest request) {
        return userRepository.updateUser(request);
    }
}
