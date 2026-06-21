package mock;

public interface UserService {
    String getUserDetails(String id);
    User updateUser(UserRequest request);
}
