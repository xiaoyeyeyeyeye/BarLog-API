package com.alcohol.service;

import com.alcohol.context.UserContext;
import com.alcohol.dto.ai.AiCardQuoteRequest;
import com.alcohol.dto.ai.AiDrinkRecommendRequest;
import com.alcohol.entity.AiRecommendLog;
import com.alcohol.entity.Bar;
import com.alcohol.mapper.AiRecommendLogMapper;
import com.alcohol.mapper.BarMapper;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.ai.AiCardQuoteVO;
import com.alcohol.vo.ai.AiDrinkRecommendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 能力（MVP Mock）。
 * <p>接口契约稳定，后续可替换为真实 LLM 而不改 Controller。</p>
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private static final List<MockCocktail> COCKTAILS = List.of(
            new MockCocktail("午夜回声", "Midnight Echo",
                    "「你说狂欢，但眼里藏着一点惆怅——就像 Billie Eilish 的歌，热闹里有凉意。」",
                    "45ml 伏特加\n20ml 荔枝利口酒\n15ml 青柠汁\n苏打水补满\n冰摇，高球杯"),
            new MockCocktail("玻璃心事", "Glass Confessions",
                    "「Jazz 的夜晚总是有话说不完——透明的杯子，装不透明的心情。」",
                    "40ml 金酒\n25ml 白味美思\n10ml 接骨木花利口酒\n柠檬皮扭花"),
            new MockCocktail("霓虹残像", "Neon Afterglow",
                    "「Indie Pop 的余韵，像一场派对散场后街上的灯光。」",
                    "30ml 龙舌兰\n20ml 西柚汁\n15ml 君度\n盐边，玛格丽特杯"),
            new MockCocktail("烈焰清醒", "Lucid Blaze",
                    "「Dark R&B 说：烈的不是酒，是你今晚的清醒。」",
                    "50ml 波本威士忌\n15ml 甜味美思\n2 dash 安格斯特拉苦精\n大冰块 Old Fashioned 杯"),
            new MockCocktail("浮光记", "Floating Memoirs",
                    "「Lo-fi 的底色配这杯——漂浮的，是记忆，不是你。」",
                    "40ml 朗姆\n20ml 椰子利口酒\n15ml 菠萝汁\n薄荷叶")
    );

    private static final List<MockQuote> QUOTES = List.of(
            new MockQuote("「在某个地方，某件事情正在发生——而你刚好在这里。」", "改编自《午夜巴黎》"),
            new MockQuote("「今晚的风很轻，刚好够吹散一点心事。」", "BarLog AI"),
            new MockQuote("「每一杯酒都是一次短暂的旅行。」", "改编自 Hemingway"),
            new MockQuote("「微醺是清醒与梦境之间最好的距离。」", "BarLog AI")
    );

    private final AiRecommendLogMapper aiRecommendLogMapper;
    private final BarMapper barMapper;

    public AiCardQuoteVO cardQuote(AiCardQuoteRequest req) {
        MockQuote q = QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
        String drink = StringUtils.hasText(req.getDrinkName()) ? req.getDrinkName() : "这杯酒";
        String place = StringUtils.hasText(req.getLocationName()) ? req.getLocationName() : "今晚的酒吧";

        AiCardQuoteVO vo = new AiCardQuoteVO();
        vo.setQuote(q.text.replace("这里", place).replace("这杯", drink));
        vo.setSource(q.source);
        return vo;
    }

    public AiDrinkRecommendVO drinkRecommend(AiDrinkRecommendRequest req) {
        String userId = UserContext.getUserId();
        MockCocktail cocktail = COCKTAILS.get(ThreadLocalRandom.current().nextInt(COCKTAILS.size()));

        AiDrinkRecommendVO vo = new AiDrinkRecommendVO();

        AiDrinkRecommendVO.DiyRecommend diy = new AiDrinkRecommendVO.DiyRecommend();
        diy.setNameZh(cocktail.zh);
        diy.setNameEn(cocktail.en);
        diy.setReason(cocktail.reason);
        diy.setRecipe(cocktail.recipe);
        vo.setDiy(diy);

        Bar bar = barMapper.selectOne(new LambdaQueryWrapper<Bar>()
                .eq(Bar::getIsActive, 1)
                .orderByDesc(Bar::getAvgRating)
                .last("LIMIT 1"));

        if (bar != null) {
            AiDrinkRecommendVO.BarRecommend br = new AiDrinkRecommendVO.BarRecommend();
            br.setBarId(bar.getId());
            br.setBarName(bar.getName());
            br.setDistanceM(280);
            br.setDrinkName("Smoke & Mirrors");
            br.setPriceLabel("¥88");
            br.setTag("烟熏系");
            br.setReason("「" + (StringUtils.hasText(req.getMood()) ? req.getMood() : "今晚") + "的心情，配一杯有故事的烟熏鸡尾酒。」");
            vo.setBar(br);
        }

        AiRecommendLog log = new AiRecommendLog();
        log.setUserId(userId);
        log.setMood(req.getMood());
        log.setSpotifyOn(Boolean.TRUE.equals(req.getSpotifyConnected()) ? 1 : 0);
        log.setResultType(StringUtils.hasText(req.getResultTab()) ? req.getResultTab().toUpperCase() : "DIY");
        log.setResultJson(JsonUtil.toJson(vo));
        log.setCreatedAt(LocalDateTime.now());
        aiRecommendLogMapper.insert(log);

        return vo;
    }

    private record MockCocktail(String zh, String en, String reason, String recipe) {}
    private record MockQuote(String text, String source) {}
}
