package mock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserApi {
    @GetMapping("/user")
    UserResponse getUser(@RequestParam("id") String id);

    @PostMapping("/user/update")
    UserResponse updateUser(@RequestBody UserRequest request);
}
