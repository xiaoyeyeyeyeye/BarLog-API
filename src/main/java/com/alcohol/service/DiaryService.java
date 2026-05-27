package com.alcohol.service;

import com.alcohol.context.UserContext;
import com.alcohol.converter.CheckInConverter;
import com.alcohol.entity.CheckIn;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.util.CheckInStatsUtil;
import com.alcohol.vo.PageVO;
import com.alcohol.vo.checkin.CheckInVO;
import com.alcohol.vo.diary.DiarySummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 日记 Tab 专用聚合查询。
 * <p>不落统计表，按月份从 {@code check_ins} 实时计算。</p>
 */
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final CheckInMapper checkInMapper;
    private final UserService userService;
    private final CheckInConverter checkInConverter;

    public DiarySummaryVO summary(int year, int month) {
        String userId = UserContext.getUserId();
        List<CheckIn> monthList = loadCheckInsInMonth(userId, year, month);

        DiarySummaryVO vo = new DiarySummaryVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setTotalCheckIns(monthList.size());
        vo.setBarsVisited(CheckInStatsUtil.distinctBars(monthList));
        vo.setAvgRating(CheckInStatsUtil.avgRating(monthList));
        vo.setLoggedDays(monthList.stream()
                .map(c -> c.getCreatedAt().getDayOfMonth())
                .distinct()
                .sorted()
                .toList());
        vo.setToday(LocalDate.now().getDayOfMonth());
        return vo;
    }

    public PageVO<CheckInVO> recent(int page, int size) {
        String userId = UserContext.getUserId();
        Page<CheckIn> p = checkInMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .orderByDesc(CheckIn::getCreatedAt));
        var user = userService.requireCurrentUser();
        List<CheckInVO> records = p.getRecords().stream()
                .map(c -> checkInConverter.toRecentVO(c, user))
                .toList();
        return new PageVO<>(records, p.getTotal(), page, size);
    }

    private List<CheckIn> loadCheckInsInMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, start.atStartOfDay())
                .lt(CheckIn::getCreatedAt, end.plusDays(1).atStartOfDay()));
    }
}
