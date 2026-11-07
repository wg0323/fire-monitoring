package com.fm.module.alarm.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.CommandTask;
import com.fm.module.alarm.vo.CommandTaskVO;
import org.springframework.beans.BeanUtils;

import java.util.stream.Collectors;

/**
 * 下行指令任务对象转换。
 */
public final class CommandConverter {

    private CommandConverter() {
    }

    public static Page<CommandTaskVO> toVOPage(Page<CommandTask> page) {
        Page<CommandTaskVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(CommandConverter::toVO).collect(Collectors.toList()));
        return result;
    }

    public static CommandTaskVO toVO(CommandTask task) {
        if (task == null) {
            return null;
        }
        CommandTaskVO vo = new CommandTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
