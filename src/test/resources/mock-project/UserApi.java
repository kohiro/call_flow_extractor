package mock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Optional;

public interface UserApi {
    @GetMapping("/user")
    String getUser();

    @PostMapping("/user/update")
    User updateUser(UserRequest request);
}
