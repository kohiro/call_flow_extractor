package mock;

public interface UserService {
    User getUserDetails(String id);
    User updateUser(UserRequest request);
}
