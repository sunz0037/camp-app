package com.camp.app.model;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String name;
    private Long parentId;
}
