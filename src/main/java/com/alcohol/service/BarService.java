package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.context.UserContext;
import com.alcohol.entity.Bar;
import com.alcohol.entity.BarFavorite;
import com.alcohol.mapper.BarFavoriteMapper;
import com.alcohol.mapper.BarMapper;
import com.alcohol.util.GeoUtil;
import com.alcohol.vo.bar.BarVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地图 Tab — 酒吧 POI 查询、距离排序、收藏。
 */
@Service
@RequiredArgsConstructor
public class BarService {

    private final BarMapper barMapper;
    private final BarFavoriteMapper barFavoriteMapper;

    public List<BarVO> nearby(Double lat, Double lng, Integer radiusM, String view) {
        String userId = UserContext.getUserId();
        Set<String> favIds = favoriteBarIds(userId);

        List<Bar> bars = barMapper.selectList(new LambdaQueryWrapper<Bar>()
                .eq(Bar::getIsActive, 1));

        int radius = radiusM != null ? radiusM : 3000;

        return bars.stream()
                .map(b -> toVO(b, lat, lng, favIds.contains(b.getId())))
                .filter(vo -> lat == null || lng == null || vo.getDistanceM() == null || vo.getDistanceM() <= radius)
                .sorted(Comparator.comparingInt(b -> b.getDistanceM() != null ? b.getDistanceM() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    public List<BarVO> ranking(String city, String period, String tag) {
        String userId = UserContext.getUserId();
        Set<String> favIds = favoriteBarIds(userId);

        LambdaQueryWrapper<Bar> qw = new LambdaQueryWrapper<Bar>()
                .eq(Bar::getIsActive, 1)
                .orderByDesc(Bar::getAvgRating)
                .orderByDesc(Bar::getReviewCount);
        if (StringUtils.hasText(city)) {
            qw.eq(Bar::getCity, city);
        }
        if (StringUtils.hasText(tag)) {
            qw.like(Bar::getTypeLabel, tag);
        }

        return barMapper.selectList(qw).stream()
                .map(b -> toVO(b, null, null, favIds.contains(b.getId())))
                .collect(Collectors.toList());
    }

    public BarVO detail(String id) {
        Bar bar = barMapper.selectById(id);
        if (bar == null || bar.getIsActive() == null || bar.getIsActive() != 1) {
            throw new BizException("酒吧不存在", 404);
        }
        String userId = UserContext.getUserId();
        boolean fav = favoriteBarIds(userId).contains(id);
        return toVO(bar, null, null, fav);
    }

    @Transactional
    public void favorite(String barId) {
        if (barMapper.selectById(barId) == null) {
            throw new BizException("酒吧不存在", 404);
        }
        String userId = UserContext.getUserId();
        Long exists = barFavoriteMapper.selectCount(new LambdaQueryWrapper<BarFavorite>()
                .eq(BarFavorite::getUserId, userId)
                .eq(BarFavorite::getBarId, barId));
        if (exists > 0) return;

        BarFavorite fav = new BarFavorite();
        fav.setUserId(userId);
        fav.setBarId(barId);
        fav.setCreatedAt(LocalDateTime.now());
        barFavoriteMapper.insert(fav);
    }

    @Transactional
    public void unfavorite(String barId) {
        String userId = UserContext.getUserId();
        barFavoriteMapper.delete(new LambdaQueryWrapper<BarFavorite>()
                .eq(BarFavorite::getUserId, userId)
                .eq(BarFavorite::getBarId, barId));
    }

    private Set<String> favoriteBarIds(String userId) {
        return barFavoriteMapper.selectList(new LambdaQueryWrapper<BarFavorite>()
                        .eq(BarFavorite::getUserId, userId))
                .stream()
                .map(BarFavorite::getBarId)
                .collect(Collectors.toSet());
    }

    private BarVO toVO(Bar b, Double lat, Double lng, boolean favorited) {
        BarVO vo = new BarVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setTypeLabel(b.getTypeLabel());
        vo.setCity(b.getCity());
        vo.setArea(b.getArea());
        vo.setAddress(b.getAddress());
        vo.setLatitude(b.getLatitude());
        vo.setLongitude(b.getLongitude());
        vo.setOpenHours(b.getOpenHours());
        vo.setAvgRating(b.getAvgRating());
        vo.setReviewCount(b.getReviewCount());
        vo.setCoverUrl(b.getCoverUrl());
        vo.setFavorited(favorited);
        if (lat != null && lng != null && b.getLatitude() != null && b.getLongitude() != null) {
            vo.setDistanceM(GeoUtil.distanceMeters(lat, lng, b.getLatitude(), b.getLongitude()));
        }
        return vo;
    }
}
