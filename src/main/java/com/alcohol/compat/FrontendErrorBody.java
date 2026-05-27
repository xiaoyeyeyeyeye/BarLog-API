package com.alcohol.compat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrontendErrorBody {

    private String message;
    private String code;
}
