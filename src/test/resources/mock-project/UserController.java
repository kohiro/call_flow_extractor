package mock;

import org.springframework.beans.factory.annotation.Autowired;

public class UserController implements UserApi {
    
    @Autowired
    private UserService userService;

    @Override
    public UserResponse getUser(String id) {
        User user = userService.getUserDetails(id);
        UserResponse response = new UserResponse();
        response.setUser(user);
        response.setMessage("Success");
        return response;
    }

    @Override
    public UserResponse updateUser(UserRequest request) {
        User user = userService.updateUser(request);
        UserResponse response = new UserResponse();
        response.setUser(user);
        response.setMessage("Updated");
        return response;
    }
}
