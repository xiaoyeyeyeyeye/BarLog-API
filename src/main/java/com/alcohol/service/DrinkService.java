package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.Drink;
import com.alcohol.enums.Visibility;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.DrinkMapper;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.drink.DrinkVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrinkService {

    private final DrinkMapper drinkMapper;
    private final CheckInMapper checkInMapper;

    public List<DrinkVO> listClassics() {
        List<Drink> drinks = drinkMapper.selectList(new LambdaQueryWrapper<Drink>()
                .eq(Drink::getIsClassic, 1)
                .orderByAsc(Drink::getName));
        LocalDateTime since = LocalDateTime.now().minusHours(12);
        return drinks.stream().map(d -> toVO(d, since)).toList();
    }

    public DrinkVO getDetail(String drinkId) {
        Drink drink = drinkMapper.selectById(drinkId);
        if (drink == null) {
            throw new BizException("酒款不存在", 404);
        }
        return toVO(drink, LocalDateTime.now().minusHours(12));
    }

    private DrinkVO toVO(Drink d, LocalDateTime since) {
        DrinkVO vo = new DrinkVO();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setCategory(d.getCategory());
        vo.setFlavorTags(JsonUtil.parseStringList(d.getFlavorTags()));
        vo.setDescription(d.getDescription());
        vo.setIconUrl(d.getIconUrl());
        vo.setClassic(d.getIsClassic() != null && d.getIsClassic() == 1);

        List<CheckIn> recent = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getDrinkId, d.getId())
                .in(CheckIn::getVisibility, Visibility.PUBLIC.name(), Visibility.TONIGHT_ONLY.name())
                .gt(CheckIn::getExpiresAt, LocalDateTime.now())
                .ge(CheckIn::getCreatedAt, since));

        vo.setTodayCheckInCount((long) recent.size());
        vo.setTopMood(computeTopMood(recent));
        return vo;
    }

    private String computeTopMood(List<CheckIn> checkIns) {
        Map<String, Long> counts = new HashMap<>();
        checkIns.forEach(c -> JsonUtil.parseStringList(c.getMoodTags())
                .forEach(m -> counts.merge(m, 1L, Long::sum)));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
