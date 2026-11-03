package com.camp.app.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"size", "records"})
public class PageResult<T> {
    private long page;
    private long size;
    private long total;
    private List<T> records;

    @JsonProperty("pageSize")
    public long getPageSize() {
        return size;
    }

    @JsonProperty("pageSize")
    public void setPageSize(long pageSize) {
        this.size = pageSize;
    }

    @JsonProperty("items")
    public List<T> getItems() {
        return records;
    }

    @JsonProperty("items")
    public void setItems(List<T> items) {
        this.records = items;
    }
}

