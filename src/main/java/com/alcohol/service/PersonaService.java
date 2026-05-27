package com.alcohol.service;

import com.alcohol.context.UserContext;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.Persona;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.PersonaMapper;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.persona.PersonaVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private static final Map<String, String> PERSONA_COPY = Map.ofEntries(
            Map.entry("Negroni", "你更像 Negroni Type。苦甜平衡，不想解释太多，但仍然想让夜晚有个漂亮结尾。"),
            Map.entry("Black Russian", "你更像 Black Russian Type。冷、直接、深色、不解释。你不太需要热闹，但你需要一个足够暗的角落。"),
            Map.entry("Mojito", "你更像 Mojito Type。清爽、轻盈，适合把白天留在门外。"),
            Map.entry("Martini", "你更像 Martini Type。干净利落，偏爱克制与距离感。"),
            Map.entry("Highball", "你更像 Highball Type。简单、耐喝，像城市夜晚里一条稳定的呼吸线。"),
            Map.entry("Old Fashioned", "你更像 Old Fashioned Type。复古、缓慢，相信好东西值得等。")
    );

    private final PersonaMapper personaMapper;
    private final CheckInMapper checkInMapper;

    public PersonaVO getMyPersona() {
        String userId = UserContext.getUserId();
        Persona persona = personaMapper.selectById(userId);
        if (persona == null) {
            refreshPersona(userId);
            persona = personaMapper.selectById(userId);
        }
        return persona == null ? emptyPersona() : toVO(persona);
    }

    @Transactional
    public PersonaVO refreshPersona(String userId) {
        List<CheckIn> checkIns = checkInMapper.selectList(
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .orderByDesc(CheckIn::getCreatedAt)
                        .last("LIMIT 100"));

        if (checkIns.isEmpty()) {
            return emptyPersona();
        }

        Map<String, Long> drinkCounts = checkIns.stream()
                .collect(Collectors.groupingBy(CheckIn::getDrinkName, Collectors.counting()));
        List<Map.Entry<String, Long>> sorted = drinkCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        String main = sorted.get(0).getKey();
        String secondary = sorted.size() > 1 ? sorted.get(1).getKey() : main;

        Set<String> moods = new LinkedHashSet<>();
        checkIns.forEach(c -> moods.addAll(JsonUtil.parseStringList(c.getMoodTags())));

        Persona persona = personaMapper.selectById(userId);
        if (persona == null) {
            persona = new Persona();
            persona.setUserId(userId);
        }
        persona.setMainDrinkType(main + " Type");
        persona.setSecondaryDrinkType(secondary + " Type");
        persona.setFlavorProfile(JsonUtil.toJson(List.of("bitter", "citrus", "night")));
        persona.setNightKeywords(JsonUtil.toJson(moods.stream().limit(5).toList()));
        persona.setSocialTendency("低压力 · 可聊天");
        persona.setGeneratedText(PERSONA_COPY.getOrDefault(main,
                "你更像 " + main + " Type。你的夜晚有自己的节奏，不需要向任何人证明。"));
        persona.setUpdatedAt(LocalDateTime.now());

        if (personaMapper.selectById(userId) == null) {
            personaMapper.insert(persona);
        } else {
            personaMapper.updateById(persona);
        }
        return toVO(persona);
    }

    private PersonaVO emptyPersona() {
        PersonaVO vo = new PersonaVO();
        vo.setGeneratedText("完成更多打卡后，系统将为你生成酒精人格。");
        vo.setFlavorProfile(List.of());
        vo.setNightKeywords(List.of());
        return vo;
    }

    private PersonaVO toVO(Persona p) {
        PersonaVO vo = new PersonaVO();
        vo.setMainDrinkType(p.getMainDrinkType());
        vo.setSecondaryDrinkType(p.getSecondaryDrinkType());
        vo.setFlavorProfile(JsonUtil.parseStringList(p.getFlavorProfile()));
        vo.setNightKeywords(JsonUtil.parseStringList(p.getNightKeywords()));
        vo.setSocialTendency(p.getSocialTendency());
        vo.setGeneratedText(p.getGeneratedText());
        vo.setUpdatedAt(p.getUpdatedAt());
        return vo;
    }
}
