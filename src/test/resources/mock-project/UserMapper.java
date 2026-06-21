package mock;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.Optional;

public interface UserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(String id);

    @Update("UPDATE users SET name = 'foo' WHERE id = #{requestedId}")
    User updateUser(UserRequest request);
}
