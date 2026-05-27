package com.alcohol.vo.persona;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonaVO {

    private String mainDrinkType;
    private String secondaryDrinkType;
    private List<String> flavorProfile;
    private List<String> nightKeywords;
    private String socialTendency;
    private String generatedText;
    private LocalDateTime updatedAt;
}
