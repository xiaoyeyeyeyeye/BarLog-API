package com.alcohol.service;

import com.alcohol.context.UserContext;
import com.alcohol.entity.Drink;
import com.alcohol.entity.UserDrink;
import com.alcohol.mapper.DrinkMapper;
import com.alcohol.mapper.UserDrinkMapper;
import com.alcohol.vo.drink.UserDrinkVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final UserDrinkMapper userDrinkMapper;
    private final DrinkMapper drinkMapper;

    public List<UserDrinkVO> listMyCollection() {
        String userId = UserContext.getUserId();
        List<UserDrink> records = userDrinkMapper.selectList(
                new LambdaQueryWrapper<UserDrink>()
                        .eq(UserDrink::getUserId, userId)
                        .orderByDesc(UserDrink::getLastCheckInAt));
        if (records.isEmpty()) {
            return List.of();
        }

        List<String> drinkIds = records.stream().map(UserDrink::getDrinkId).toList();
        Map<String, Drink> drinkMap = drinkMapper.selectBatchIds(drinkIds).stream()
                .collect(Collectors.toMap(Drink::getId, d -> d));

        List<UserDrinkVO> result = new ArrayList<>();
        for (UserDrink ud : records) {
            Drink drink = drinkMap.get(ud.getDrinkId());
            UserDrinkVO vo = new UserDrinkVO();
            vo.setDrinkId(ud.getDrinkId());
            vo.setCheckInCount(ud.getCheckInCount());
            vo.setFirstUnlockedAt(ud.getFirstUnlockedAt());
            vo.setLastCheckInAt(ud.getLastCheckInAt());
            if (drink != null) {
                vo.setDrinkName(drink.getName());
                vo.setCategory(drink.getCategory());
                vo.setIconUrl(drink.getIconUrl());
            }
            result.add(vo);
        }
        return result;
    }
}
