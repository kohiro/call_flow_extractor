package mock;

import org.springframework.beans.factory.annotation.Autowired;

public class UserController implements UserApi {
    
    @Autowired
    private UserService userService;

    @Override
    public String getUser() {
        return userService.getUserDetails("123");
    }

    @Override
    public User updateUser(UserRequest request) {
        return userService.updateUser(request);
    }
}
