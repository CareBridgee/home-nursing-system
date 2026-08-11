package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Profile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProfileImageUtilTest {

    @Test
    void nullProfileResolvesToNull() {
        assertThat(ProfileImageUtil.resolveProfileImageUrl(null)).isNull();
    }

    @Test
    void nullImageUrlResolvesToNull() {
        Profile profile = Profile.builder().profileImageUrl(null).build();
        assertThat(ProfileImageUtil.resolveProfileImageUrl(profile)).isNull();
    }

    @Test
    void blankImageUrlResolvesToNull() {
        Profile profile = Profile.builder().profileImageUrl("   ").build();
        assertThat(ProfileImageUtil.resolveProfileImageUrl(profile)).isNull();
    }

    @Test
    void populatedImageUrlIsReturned() {
        Profile profile = Profile.builder().profileImageUrl("https://cdn.example.com/a.png").build();
        assertThat(ProfileImageUtil.resolveProfileImageUrl(profile))
                .isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void imageUrlWithSurroundingWhitespaceIsReturnedAsIs() {
        Profile profile = Profile.builder().profileImageUrl("  https://cdn.example.com/a.png  ").build();
        assertThat(ProfileImageUtil.resolveProfileImageUrl(profile))
                .isEqualTo("  https://cdn.example.com/a.png  ");
    }
}
