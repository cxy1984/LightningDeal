package com.lightningdeal.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ES 同步 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型：sync（同步或更新）、delete（删除）
     */
    private String action;

    /**
     * 活动ID
     */
    private Long activityId;
}
