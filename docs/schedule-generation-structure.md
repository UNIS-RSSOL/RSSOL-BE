# 스케줄 생성 시스템 구조 변경 요약

## 변경된 시스템 흐름

### 1. 온보딩/가게정보 수정 (시간대만 저장)

- **StoreSetting**: 가게 운영 시간, 세그먼트(파트타임 구간) 시간대만 저장
- **requiredStaff (필요 인원수)**: ❌ 저장하지 않음

```json
// 온보딩/가게정보 수정 요청 예시
{
  "openTime": "09:00",
  "closeTime": "22:00",
  "useSegments": true,
  "segments": [
    { "startTime": "09:00", "endTime": "14:00" },
    { "startTime": "14:00", "endTime": "18:00" },
    { "startTime": "18:00", "endTime": "22:00" }
  ],
  "hasBreakTime": true,
  "breakStartTime": "15:00",
  "breakEndTime": "16:00"
}
```

### 2. 근무표 생성 (시간대별 인원수 받음)

- **시간대**: StoreSetting에서 조회
- **인원수**: 스케줄 요청 시 `StaffRequirementDto`로 별도 전송

#### 2-1. 세그먼트 사용 O (구간화)

```json
// 스케줄 요청 예시 - 세그먼트별 인원수
POST /api/schedules/requests
{
  "startDate": "2026-02-24",
  "endDate": "2026-03-02",
  "staffRequirement": {
    "segmentStaffList": [
      { "segmentIndex": 0, "requiredStaff": 2 },
      { "segmentIndex": 1, "requiredStaff": 3 },
      { "segmentIndex": 2, "requiredStaff": 4 }
    ]
  }
}
```

#### 2-2. 세그먼트 사용 X (전체 시간대)

```json
// 스케줄 요청 예시 - 동시 근무자 수
POST /api/schedules/requests
{
  "startDate": "2026-02-24",
  "endDate": "2026-03-02",
  "staffRequirement": {
    "requiredStaff": 3
  }
}
```

---

## 변경된 파일 목록

### Entity

| 파일                         | 변경사항                      |
|----------------------------|---------------------------|
| `StoreSetting.java`        | `requiredStaff` 필드 제거     |
| `StoreSettingSegment.java` | `requiredStaff` 필드 제거     |
| `ScheduleRequest.java`     | `settingSourceType` 필드 제거 |

### DTO

| 파일                                 | 변경사항                                             |
|------------------------------------|--------------------------------------------------|
| `StoreSettingDto.java`             | `requiredStaff` 필드 제거                            |
| `SegmentDto.java`                  | `requiredStaff` 필드 제거                            |
| `StaffRequirementDto.java`         | ✅ 새로 생성 - 인원수만 받는 DTO                            |
| `ScheduleRequestDto.java`          | `staffRequirement` 필드 추가, `settingSourceType` 제거 |
| `ScheduleRequestResponseDto.java`  | `settingSourceType` 필드 제거                        |
| `TemporaryScheduleSettingDto.java` | ⚠️ Deprecated                                    |
| `SettingSourceType.java`           | ⚠️ Deprecated                                    |

### Service

| 파일                               | 변경사항                                  |
|----------------------------------|---------------------------------------|
| `StoreSettingService.java`       | `requiredStaff` 관련 코드 제거              |
| `ScheduleGenerationService.java` | `fromStoreSettingWithStaff()` 메서드로 변경 |

### Controller

| 파일                                  | 변경사항            |
|-------------------------------------|-----------------|
| `ScheduleGenerationController.java` | 임시 설정 저장 API 제거 |

---

## 핵심 로직: `ScheduleSettingSnapshot.fromStoreSettingWithStaff()`

```java
/**
 * StoreSetting(시간대) + StaffRequirementDto(인원수) 결합
 * 
 * 1. 세그먼트 사용 O: 각 세그먼트별로 필요 인원수 적용
 * 2. 세그먼트 사용 X: 가게 운영 시간 전체에 필요한 동시 근무자 수 적용
 */
public static ScheduleSettingSnapshot fromStoreSettingWithStaff(
        StoreSetting setting, 
        StaffRequirementDto staffRequirement) {
    
    List<SegmentSnapshot> segs = new ArrayList<>();
    
    if (setting.isUseSegments() && setting.getSegments() != null) {
        // 세그먼트 사용 시: 시간대 + 인원수 결합
        Map<Integer, Integer> staffMap = new HashMap<>();
        if (staffRequirement != null && staffRequirement.getSegmentStaffList() != null) {
            for (SegmentStaffDto segStaff : staffRequirement.getSegmentStaffList()) {
                staffMap.put(segStaff.getSegmentIndex(), segStaff.getRequiredStaff());
            }
        }
        
        for (int i = 0; i < storeSegments.size(); i++) {
            int requiredStaff = staffMap.getOrDefault(i, 1); // 기본값 1명
            segs.add(SegmentSnapshot.builder()
                    .startTime(seg.getStartTime())
                    .endTime(seg.getEndTime())
                    .requiredStaff(requiredStaff)
                    .build());
        }
    } else {
        // 세그먼트 미사용 시: 전체 운영시간을 하나의 세그먼트로
        int requiredStaff = staffRequirement != null ? staffRequirement.getRequiredStaff() : 1;
        segs.add(SegmentSnapshot.builder()
                .startTime(setting.getOpenTime())
                .endTime(setting.getCloseTime())
                .requiredStaff(requiredStaff)
                .build());
    }
    
    return ScheduleSettingSnapshot.builder()
            .segments(segs)
            .build();
}
```

---

## API 변경 요약

### 제거된 API

- ~~`POST /api/schedules/settings/temporary`~~ (임시 설정 저장)

### 변경된 API

| API                            | 변경 전                                       | 변경 후               |
|--------------------------------|--------------------------------------------|--------------------|
| `POST /api/schedules/requests` | `settingSourceType`, `temporarySettingKey` | `staffRequirement` |



