package mock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public String getUser() {
        return userService.getUserDetails("123");
    }

    @PostMapping("/user/update")
    public User updateUser(UserRequest request) {
        return userService.updateUser(request);
    }
}
