package mock;

import org.apache.ibatis.annotations.Select;
import java.util.Optional;

public interface UserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(String id);

    // SQL is defined in UserMapper.xml
    User updateUser(UserRequest request);
}
