package com.cheng.meetlink.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private String id;
    private String name;
    private String avatar;
    private String type;
    private List<String> badge;
    private String ipOwnership;
}
