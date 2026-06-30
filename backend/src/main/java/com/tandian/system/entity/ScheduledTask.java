package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体类
 */
@Data
@TableName("scheduled_task")
public class ScheduledTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务唯一标识 */
    private String taskKey;

    /** 任务名称 */
    private String taskName;

    /** 任务描述 */
    private String taskDesc;

    /** 是否启用（0禁用/1启用） */
    private Integer enabled;

    /** 是否通知（0不通知/1通知） */
    private Integer notifyEnabled;

    /** 通知邮箱列表，多个用逗号分隔 */
    private String emails;

    /** Cron表达式 */
    private String cronExpression;

    /** 上次执行时间 */
    private LocalDateTime lastExecuteTime;

    /** 上次执行状态（success/failed） */
    private String lastExecuteStatus;

    /** 上次执行结果信息 */
    private String lastExecuteMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
