package com.lightningdeal.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BloomFilterService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BloomFilterServiceTest {

    @Mock RedissonClient redissonClient;
    @Mock RBloomFilter<Object> bloomFilter;  // 用 Object 避免泛型擦除重载问题

    private BloomFilterService service;

    @BeforeEach
    void setUp() {
        lenient().when(redissonClient.getBloomFilter(anyString())).thenReturn(bloomFilter);
    }

    @Test
    void shouldInitNewBloomFilterWhenNotExists() {
        when(bloomFilter.isExists()).thenReturn(false);
        when(bloomFilter.tryInit(anyLong(), anyDouble())).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        verify(bloomFilter).tryInit(10000L, 0.01);
    }

    @Test
    void shouldUseExistingBloomFilterWhenExists() {
        when(bloomFilter.isExists()).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        verify(bloomFilter, never()).tryInit(anyLong(), anyDouble());
    }

    @Test
    void shouldReturnFalseForNonExistentElement() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains(99999L)).thenReturn(false);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.mightContain(99999L)).isFalse();
    }

    @Test
    void shouldReturnTrueForExistingElement() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains(1L)).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.mightContain(1L)).isTrue();
    }

    @Test
    void shouldAddSingleElement() {
        when(bloomFilter.isExists()).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        // 只要不抛异常就算通过
        service.add(42L);
    }

    @Test
    void shouldAddMultipleElements() {
        when(bloomFilter.isExists()).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        // 只要不抛异常就算通过
        service.addAll(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void shouldReturnCount() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.count()).thenReturn(5L);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.count()).isEqualTo(5L);
    }

    @Test
    void shouldHandleNullActivityId() {
        when(bloomFilter.isExists()).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.mightContain(null)).isFalse();
        service.add(null);  // 不应抛异常
        verify(bloomFilter, never()).add(any());
    }

    @Test
    void shouldHandleNullCollection() {
        when(bloomFilter.isExists()).thenReturn(true);

        service = new BloomFilterService(redissonClient);
        service.init();
        service.addAll(null);  // 不应抛异常

        verify(bloomFilter, never()).add(any());
    }

    @Test
    void shouldGetExpectedInsertions() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.getExpectedInsertions()).thenReturn(10000L);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.getExpectedInsertions()).isEqualTo(10000L);
    }

    @Test
    void shouldGetFalseProbability() {
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.getFalseProbability()).thenReturn(0.01);

        service = new BloomFilterService(redissonClient);
        service.init();

        assertThat(service.getFalseProbability()).isEqualTo(0.01);
    }
}
