package com.cheng.meetlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cheng.meetlink.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * " +
            "FROM message " +
            "WHERE (from_id = #{userId} AND to_id = #{targetId}) " +
            "   OR (from_id = #{targetId} AND to_id = #{userId}) " +
            "ORDER BY create_time DESC LIMIT 1")
    Message getPreviousShowTimeMsg(String userId, String targetId);


    @Select("<script>" +
            "SELECT * FROM message " +
            "WHERE ( " +
            "  (from_id = #{userId} AND to_id = #{targetId}) " +
            "  OR (from_id = #{targetId} AND to_id = #{userId}) " +
            "  OR (source = 'group' AND to_id = #{targetId}) " +
            ") " +
            "<if test='cursorTime != null and cursorId != null'>" +
            "  AND (create_time &lt; #{cursorTime} OR (create_time = #{cursorTime} AND id &lt; #{cursorId})) " +
            "</if>" +
            "ORDER BY create_time DESC, id DESC " +
            "LIMIT #{num}" +
            "</script>")
    @ResultMap("mybatis-plus_Message")
    List<Message> record(@Param("userId") String userId,
                         @Param("targetId") String targetId,
                         @Param("cursorTime") Date cursorTime,
                         @Param("cursorId") String cursorId,
                         @Param("num") int num);
}
