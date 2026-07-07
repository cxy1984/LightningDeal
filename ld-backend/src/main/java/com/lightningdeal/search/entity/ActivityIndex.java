package com.lightningdeal.search.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ES 商品活动索引
 */
@Data
@Document(indexName = "seckill_activity")
@Schema(description = "ES 商品活动索引")
public class ActivityIndex {

    @Id
    @Schema(description = "活动ID")
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    @Schema(description = "活动名称")
    private String name;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    @Schema(description = "商品名称")
    private String goodsName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    @Schema(description = "商品描述")
    private String goodsDescription;

    @Field(type = FieldType.Keyword)
    @Schema(description = "商品图片")
    private String goodsImage;

    @Field(type = FieldType.Double)
    @Schema(description = "秒杀价")
    private BigDecimal flashPrice;

    @Field(type = FieldType.Double)
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Field(type = FieldType.Integer)
    @Schema(description = "总库存")
    private Integer totalStock;

    @Field(type = FieldType.Integer)
    @Schema(description = "已售数量")
    private Integer soldStock;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Field(type = FieldType.Integer)
    @Schema(description = "状态")
    private Integer status;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
