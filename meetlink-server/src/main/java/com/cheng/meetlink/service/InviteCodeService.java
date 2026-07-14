package com.cheng.meetlink.service;

import com.cheng.meetlink.entity.InviteCode;

import java.util.List;

public interface InviteCodeService {
    /**
     * 生成邀请码
     */
    InviteCode generate(String adminUserId, Integer maxMembers);

    /**
     * 验证并使用邀请码（原子操作）
     */
    InviteCode validateAndUse(String code, String userId, String groupId);

    /**
     * 列出所有邀请码（管理员）
     */
    List<InviteCode> listAll();

    /**
     * 废弃邀请码（管理员）
     */
    void invalidate(String codeId);
}
