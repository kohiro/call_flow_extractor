package mock;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public String getUserDetails(String id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get().getName() + " (" + user.get().getEmail() + ")";
        }
        return "Unknown";
    }

    @Override
    @Transactional
    public User updateUser(UserRequest request) {
        return userRepository.updateUser(request);
    }
}
