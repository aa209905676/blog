package com.ican.quartz.task;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ican.entity.VisitLog;
import com.ican.mapper.VisitLogMapper;
import com.ican.service.RedisService;
import com.ican.service.VisitLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ican.constant.RedisConstant.UNIQUE_VISITOR;
import static com.ican.constant.RedisConstant.VISIT_LOG;

/**
 * 执行定时任务
 *
 * @author ican
 */
@SuppressWarnings(value = "all")
@Component("timedTask")
public class TimedTask {
    @Autowired
    private RedisService redisService;

    @Autowired
    private VisitLogMapper visitLogMapper;
    @Autowired
    private VisitLogService visitLogService;

    /**
     * 清除博客访问记录
     */
    public void clear() {
        redisService.deleteObject(UNIQUE_VISITOR);
    }

    /**
     * 测试任务
     */
    public void test() {
        System.out.println("测试任务");
    }

    /**
     * 清除一周前的访问日志
     */
    public void clearVistiLog() {
        DateTime endTime = DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), -7));
        visitLogMapper.deleteVisitLog(endTime);
    }

    /**
     * 每5分钟更新访客
     */
    @Scheduled(fixedRate = 600000) // 每6分钟执行一次
    public void batchInsertFromRedisToMySQL() {
        System.out.println("更新访客任务开始");
        Map<String, Object> entries = redisService.getHashAll(VISIT_LOG);
        if (MapUtil.isEmpty(entries)) {
            return;
        }
        List<VisitLog> entityList = entries.values().stream()
                .map(object -> (VisitLog) object)
                .collect(Collectors.toList());

        visitLogService.saveBatch(entityList);

        // 删除已经取出的数据
        Object[] keys = entries.keySet().toArray();
        redisService.deleteHash(VISIT_LOG, keys);

    }
}