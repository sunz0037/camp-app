package com.camp.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OptionItem {
    private Long id;
    private String name;
    private Long parentId;
}
