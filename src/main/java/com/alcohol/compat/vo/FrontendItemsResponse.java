package com.alcohol.compat.vo;

import lombok.Data;

import java.util.List;

@Data
public class FrontendItemsResponse<T> {

    private List<T> items;
    private String nextCursor;

    public static <T> FrontendItemsResponse<T> of(List<T> items) {
        FrontendItemsResponse<T> resp = new FrontendItemsResponse<>();
        resp.setItems(items);
        return resp;
    }
}
