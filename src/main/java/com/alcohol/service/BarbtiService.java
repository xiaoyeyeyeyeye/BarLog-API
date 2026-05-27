package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.constant.BarbtiConstants;
import com.alcohol.constant.BarbtiScorer;
import com.alcohol.context.UserContext;
import com.alcohol.dto.barbti.BarbtiSubmitRequest;
import com.alcohol.entity.BarbtiResult;
import com.alcohol.mapper.BarbtiResultMapper;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.barbti.BarbtiQuestionVO;
import com.alcohol.vo.barbti.BarbtiVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BarBTI 酒吧人格问卷。
 * <p>题库与计分规则见 {@link BarbtiConstants}、{@link BarbtiScorer}，与 Demo 前端一致。</p>
 */
@Service
@RequiredArgsConstructor
public class BarbtiService {

    private final BarbtiResultMapper barbtiResultMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BarbtiQuestionVO.QuestionList listQuestions() {
        List<BarbtiQuestionVO> questions = new ArrayList<>();
        for (int i = 0; i < BarbtiConstants.QUESTIONS.length; i++) {
            String[] q = BarbtiConstants.QUESTIONS[i];
            BarbtiQuestionVO vo = new BarbtiQuestionVO();
            vo.setIndex(i);
            vo.setCategory(BarbtiConstants.CATEGORIES[i]);
            vo.setText(q[0]);
            vo.setOptions(List.of(q[1], q[2], q[3], q[4]));
            questions.add(vo);
        }
        return new BarbtiQuestionVO.QuestionList(questions);
    }

    public BarbtiVO getMine() {
        BarbtiResult result = barbtiResultMapper.selectById(UserContext.getUserId());
        if (result == null) {
            throw new BizException("尚未完成 BarBTI 测试", 404);
        }
        return toVO(result);
    }

    @Transactional
    public BarbtiVO submit(BarbtiSubmitRequest req) {
        String userId = UserContext.getUserId();
        validateAnswers(req.getAnswers());

        Map<String, Integer> raw = BarbtiScorer.accumulateRawScores(req.getAnswers());
        BarbtiConstants.TypeDefinition type = BarbtiScorer.resolveType(raw);

        BarbtiResult result = barbtiResultMapper.selectById(userId);
        boolean isNew = result == null;
        if (isNew) {
            result = new BarbtiResult();
            result.setUserId(userId);
        }
        result.setTypeCode(type.code());
        result.setSubtitle(type.subtitle());
        result.setDescription(type.description());
        result.setTraitTags(JsonUtil.toJson(type.traits()));
        result.setScores(JsonUtil.toJson(BarbtiScorer.normalize(raw)));
        result.setAnswers(JsonUtil.toJson(req.getAnswers()));
        result.setCompletedAt(LocalDateTime.now());

        if (isNew) {
            barbtiResultMapper.insert(result);
        } else {
            barbtiResultMapper.updateById(result);
        }
        return toVO(result);
    }

    private void validateAnswers(List<Integer> answers) {
        for (int i = 0; i < answers.size(); i++) {
            int opt = answers.get(i);
            if (opt < 0 || opt > 3) {
                throw new BizException("第 " + (i + 1) + " 题答案无效");
            }
        }
    }

    private BarbtiVO toVO(BarbtiResult r) {
        BarbtiVO vo = new BarbtiVO();
        vo.setTypeCode(r.getTypeCode());
        vo.setSubtitle(r.getSubtitle());
        vo.setDescription(r.getDescription());
        vo.setTraitTags(JsonUtil.parseStringList(r.getTraitTags()));
        try {
            vo.setScores(objectMapper.readValue(r.getScores(), new TypeReference<Map<String, Integer>>() {}));
        } catch (Exception e) {
            vo.setScores(Map.of());
        }
        vo.setCompletedAt(r.getCompletedAt());
        return vo;
    }
}
