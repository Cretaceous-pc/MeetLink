package com.cheng.meetlink.vo.file;

import lombok.Data;

@Data
public class InviteVo {
    private String userId;
    private FileInfo fileInfo;

    @Data
    public static class FileInfo {
        private String name;
        private long size;
    }
}
