package com.compass.domain.user.application;

import com.compass.domain.user.dao.UserPreferenceRepository;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.UserPreference;
import com.compass.domain.user.dto.UserPreferenceRequest;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자 취향 서비스
@Service
@RequiredArgsConstructor
@Transactional
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;

    // 취향 저장 및 수정
    public void saveOrUpdate(Long userId, UserPreferenceRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        var pref = user.getPreference();

        if (pref == null) {
            var newPref = UserPreference.builder()
                    .user(user)
                    .preferredCategory(request.preferredCategory())
                    .dietRestriction(request.dietRestriction())
                    .travelPace(request.travelPace())
                    .build();
            preferenceRepository.save(newPref);
            return;
        }

        pref.update(request.preferredCategory(), request.dietRestriction(), request.travelPace());
    }
}