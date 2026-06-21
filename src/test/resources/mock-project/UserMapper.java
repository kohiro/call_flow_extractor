package mock;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {

    @Select("SELECT name FROM users WHERE id = #{id}")
    String findNameById(String id);

    String findEmailById(String id);

    @Update("UPDATE users SET name = 'foo' WHERE id = #{requestedId}")
    User updateUser(UserRequest request);
}
