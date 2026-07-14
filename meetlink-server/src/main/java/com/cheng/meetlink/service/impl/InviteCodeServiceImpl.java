package com.cheng.meetlink.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cheng.meetlink.entity.InviteCode;
import com.cheng.meetlink.exception.MeetLinkException;
import com.cheng.meetlink.mapper.InviteCodeMapper;
import com.cheng.meetlink.service.InviteCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InviteCodeServiceImpl extends ServiceImpl<InviteCodeMapper, InviteCode> implements InviteCodeService {

    @Override
    public InviteCode generate(String adminUserId, Integer maxMembers) {
        if (maxMembers == null || maxMembers <= 0) {
            maxMembers = 100;
        }
        InviteCode code = new InviteCode();
        code.setId(IdUtil.simpleUUID());
        code.setCode(generateCode());
        code.setMaxMembers(maxMembers);
        code.setIsUsed(false);
        code.setCreatedBy(adminUserId);
        save(code);
        return code;
    }

    @Override
    @Transactional
    public InviteCode validateAndUse(String codeStr, String userId, String groupId) {
        InviteCode code = lambdaQuery()
                .eq(InviteCode::getCode, codeStr)
                .eq(InviteCode::getIsUsed, false)
                .one();
        if (code == null) {
            throw new MeetLinkException("邀请码无效或已被使用~");
        }
        code.setIsUsed(true);
        code.setUsedBy(userId);
        code.setUsedForGroup(groupId);
        updateById(code);
        return code;
    }

    @Override
    public List<InviteCode> listAll() {
        return lambdaQuery().orderByDesc(InviteCode::getCreateTime).list();
    }

    @Override
    public void invalidate(String codeId) {
        InviteCode code = getById(codeId);
        if (code == null) {
            throw new MeetLinkException("邀请码不存在~");
        }
        removeById(codeId);
    }

    private String generateCode() {
        return RandomUtil.randomStringUpper(8);
    }
}
