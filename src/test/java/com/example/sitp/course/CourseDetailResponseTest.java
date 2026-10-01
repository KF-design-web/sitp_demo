package com.example.sitp.course;

import com.example.sitp.course.dto.ChapterCard;
import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Chapter;
import com.example.sitp.course.model.Course;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseDetailResponseTest {

    private Course course() {
        return Course.builder()
                .id(7L)
                .title("Git Foundations")
                .description("Branches without fear")
                .price(new BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build();
    }

    private Chapter chapter(Course c, String title, String videoRef, int position) {
        return Chapter.builder()
                .id((long) position)
                .title(title)
                .videoUrl(videoRef)
                .position(position)
                .course(c)
                .build();
    }

    @Test
    void from_mapsCourseFields_andChapterCardsInTheOrderHanded() {
        Course c = course();
        List<Chapter> chapters = List.of(
                chapter(c, "First steps", "bunny-ref-1", 1),
                chapter(c, "Going further", "bunny-ref-2", 2));

        CourseDetailResponse card = CourseDetailResponse.from(c, chapters, true);

        assertThat(card.getId()).isEqualTo(7L);
        assertThat(card.getTitle()).isEqualTo("Git Foundations");
        assertThat(card.getDescription()).isEqualTo("Branches without fear");
        assertThat(card.getTargetAudience()).isEqualTo(Audience.OUTSIDER);
        assertThat(card.getChapters()).hasSize(2);
        assertThat(card.getChapters()).extracting(ChapterCard::getTitle)
                .containsExactly("First steps", "Going further");
        assertThat(card.getChapters()).extracting(ChapterCard::getPosition)
                .containsExactly(1, 2);
        assertThat(card.getChapters().get(0).getVideoUrl()).isEqualTo("bunny-ref-1");
    }

    @Test
    void from_theVerdictDecidesThePriceLine_anonymousGetsNoLine() {
        Course c = course();

        CourseDetailResponse anonymous = CourseDetailResponse.from(c, List.of(), false);
        assertThat(anonymous.isPriceVisible()).isFalse();
        assertThat(anonymous.getPrice()).isNull();

        CourseDetailResponse member = CourseDetailResponse.from(c, List.of(), true);
        assertThat(member.isPriceVisible()).isTrue();
        assertThat(member.getPrice()).isEqualByComparingTo(new BigDecimal("49.90"));
    }
}
