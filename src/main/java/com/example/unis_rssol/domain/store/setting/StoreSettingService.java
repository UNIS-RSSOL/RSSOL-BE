package com.example.unis_rssol.domain.store.setting;

import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSettingService {

    private final StoreSettingRepository storeSettingRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TEMP_SETTING_PREFIX = "temp_store_setting:store:";

    /**
     * 온보딩 시 기본 매장 설정 저장 (시간대만 저장, 인원수는 스케줄 생성 시 별도)
     */
    @Transactional
    public StoreSetting createStoreSetting(Store store, StoreSettingDto dto) {
        StoreSetting setting = StoreSetting.builder()
                .store(store)
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .useSegments(dto.isUseSegments())
                .hasBreakTime(dto.isHasBreakTime())
                .breakStartTime(dto.getBreakStartTime())
                .breakEndTime(dto.getBreakEndTime())
                .build();

        // 세그먼트 사용 시 세그먼트 추가 (시간대만)
        if (dto.isUseSegments() && dto.getSegments() != null) {
            for (SegmentDto seg : dto.getSegments()) {
                StoreSettingSegment segment = StoreSettingSegment.builder()
                        .startTime(seg.getStartTime())
                        .endTime(seg.getEndTime())
                        .build();
                setting.addSegment(segment);
            }
        }

        return storeSettingRepository.save(setting);
    }

    /**
     * 매장 설정 조회
     */
    @Transactional(readOnly = true)
    public StoreSetting getStoreSetting(Long storeId) {
        return storeSettingRepository.findByStoreId(storeId)
                .orElseThrow(() -> new NotFoundException("매장 설정을 찾을 수 없습니다."));
    }

    /**
     * 매장 설정 조회 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<StoreSetting> findStoreSetting(Long storeId) {
        return storeSettingRepository.findByStoreId(storeId);
    }

    /**
     * 매장 설정 업데이트 (PATCH) - 시간대만 업데이트
     */
    @Transactional
    public StoreSetting updateStoreSetting(Long storeId, StoreSettingDto dto) {
        StoreSetting setting = getStoreSetting(storeId);

        if (dto.getOpenTime() != null) setting.setOpenTime(dto.getOpenTime());
        if (dto.getCloseTime() != null) setting.setCloseTime(dto.getCloseTime());
        setting.setUseSegments(dto.isUseSegments());
        setting.setHasBreakTime(dto.isHasBreakTime());
        if (dto.getBreakStartTime() != null) setting.setBreakStartTime(dto.getBreakStartTime());
        if (dto.getBreakEndTime() != null) setting.setBreakEndTime(dto.getBreakEndTime());

        // 세그먼트 업데이트 (시간대만)
        if (dto.isUseSegments() && dto.getSegments() != null) {
            setting.clearSegments();
            for (SegmentDto seg : dto.getSegments()) {
                StoreSettingSegment segment = StoreSettingSegment.builder()
                        .startTime(seg.getStartTime())
                        .endTime(seg.getEndTime())
                        .build();
                setting.addSegment(segment);
            }
        } else if (!dto.isUseSegments()) {
            setting.clearSegments();
        }

        return storeSettingRepository.save(setting);
    }

    /**
     * 임시 설정 Redis에 저장 (스케줄 생성 시 임시 수정용)
     */
    public String saveTemporarySetting(Long storeId, StoreSettingDto dto) {
        String key = TEMP_SETTING_PREFIX + storeId + ":" + System.currentTimeMillis();
        ObjectMapper mapper = createObjectMapper();

        try {
            String json = mapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24)); // 24시간 유효
            return key;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("임시 설정 저장 실패", e);
        }
    }

    /**
     * 임시 설정 Redis에서 조회
     */
    public StoreSettingDto getTemporarySetting(String redisKey) {
        String json = (String) redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new NotFoundException("임시 설정을 찾을 수 없습니다.");
        }

        ObjectMapper mapper = createObjectMapper();
        try {
            return mapper.readValue(json, StoreSettingDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("임시 설정 읽기 실패", e);
        }
    }

    /**
     * 임시 설정을 기본 설정으로 저장 (사용자가 "기본으로 저장" 선택 시)
     */
    @Transactional
    public StoreSetting applyTemporarySettingAsDefault(Long storeId, String redisKey) {
        StoreSettingDto tempDto = getTemporarySetting(redisKey);
        StoreSetting updated = updateStoreSetting(storeId, tempDto);
        redisTemplate.delete(redisKey); // 임시 설정 삭제
        return updated;
    }

    /**
     * 임시 설정 삭제
     */
    public void deleteTemporarySetting(String redisKey) {
        redisTemplate.delete(redisKey);
    }

    /**
     * Entity -> DTO 변환
     */
    public StoreSettingDto toDto(StoreSetting setting) {
        StoreSettingDto dto = StoreSettingDto.builder()
                .openTime(setting.getOpenTime())
                .closeTime(setting.getCloseTime())
                .useSegments(setting.isUseSegments())
                .hasBreakTime(setting.isHasBreakTime())
                .breakStartTime(setting.getBreakStartTime())
                .breakEndTime(setting.getBreakEndTime())
                .build();

        if (setting.isUseSegments() && setting.getSegments() != null) {
            dto.setSegments(
                    setting.getSegments().stream()
                            .map(seg -> SegmentDto.builder()
                                    .startTime(seg.getStartTime())
                                    .endTime(seg.getEndTime())
                                    .build())
                            .toList()
            );
        }

        return dto;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

