package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.VisitLog;
import com.ican.mapper.VisitLogMapper;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.PageResult;
import com.ican.service.RedisService;
import com.ican.service.VisitLogService;
import com.ican.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static com.ican.constant.RedisConstant.VISIT_LOG;

/**
 * 访问业务接口实现类
 *
 * @author ican
 */
@Service
public class VisitLogServiceImpl extends ServiceImpl<VisitLogMapper, VisitLog> implements VisitLogService {

    @Autowired
    private VisitLogMapper visitLogMapper;
    @Resource
    private RedisService redisService;

    @Override
    public void saveVisitLog(VisitLog visitLog) {

        redisService.setHash(VISIT_LOG, LocalDateTime.now().toString(),visitLog);
        // 保存访问日志
//        visitLogMapper.insert(visitLog);
    }

    @Override
    public PageResult<VisitLog> listVisitLog(ConditionDTO condition) {
        // 查询访问日志数量
        Long count = visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .like(StringUtils.hasText(condition.getKeyword()), VisitLog::getPage, condition.getKeyword())
        );
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询访问日志列表
        List<VisitLog> visitLogVOList = visitLogMapper.selectVisitLogList(PageUtils.getLimit(),
                PageUtils.getSize(), condition.getKeyword());
        return new PageResult<>(visitLogVOList, count);
    }
}




