package mock;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    @Select("SELECT name FROM users WHERE id = #{id}")
    String findNameById(String id);

    // This one will be resolved from XML
    String findEmailById(String id);
}
