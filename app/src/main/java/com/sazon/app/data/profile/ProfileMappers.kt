package com.sazon.app.data.profile

import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.profile.UserProfile

fun ProfileEntity.toDomain(): UserProfile =
    UserProfile(
        displayName = displayName,
        avatarColorId = avatarColorId,
        createdAt = createdAt,
        preferences = PreferenceProfile(
            dietPatternId = dietPatternId,
            hardRestrictionIds = hardRestrictionIds,
            softPreferenceIds = softPreferenceIds,
            cuisineIds = cuisineIds,
            preferredIngredientIds = preferredIngredientIds,
            dislikedIngredientIds = dislikedIngredientIds,
            difficultyId = difficultyId,
            timeBucketId = timeBucketId,
            defaultServings = defaultServings,
            updatedAt = updatedAt,
        ),
    )

fun UserProfile.toEntity(): ProfileEntity =
    ProfileEntity(
        id = ProfileEntity.SINGLETON_ID,
        displayName = displayName,
        avatarColorId = avatarColorId,
        createdAt = createdAt,
        dietPatternId = preferences.dietPatternId,
        hardRestrictionIds = preferences.hardRestrictionIds,
        softPreferenceIds = preferences.softPreferenceIds,
        cuisineIds = preferences.cuisineIds,
        preferredIngredientIds = preferences.preferredIngredientIds,
        dislikedIngredientIds = preferences.dislikedIngredientIds,
        difficultyId = preferences.difficultyId,
        timeBucketId = preferences.timeBucketId,
        defaultServings = preferences.defaultServings,
        updatedAt = preferences.updatedAt,
    )
