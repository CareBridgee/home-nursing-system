package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Profile;

public final class ProfileImageUtil {

    private ProfileImageUtil() {
    }

    public static String resolveProfileImageUrl(Profile profile) {
        if (profile == null) {
            return null;
        }
        String profileImageUrl = profile.getProfileImageUrl();
        return hasText(profileImageUrl) ? profileImageUrl : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
