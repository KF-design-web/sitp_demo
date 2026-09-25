package com.example.sitp.course;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    private Course course(String title, Audience audience) {
        return Course.builder()
                .title(title)
                .description("A course called " + title)
                .targetAudience(audience)
                .build();
    }

    @Test
    void findAll_returnsEverySavedCourse() {
        courseRepository.save(course("Spring Boot Basics", Audience.INTERN));
        courseRepository.save(course("Advanced Git", Audience.OUTSIDER));
        courseRepository.save(course("Clean Code Habits", Audience.INTERN));

        var all = courseRepository.findAll();

        assertThat(all).hasSize(3);
        assertThat(all)
                .extracting(Course::getTitle)
                .containsExactlyInAnyOrder("Spring Boot Basics", "Advanced Git", "Clean Code Habits");
    }

    @Test
    void findAll_returnsAnEmptyList_notNull_whenTheCabinetIsEmpty() {
        var all = courseRepository.findAll();

        assertThat(all).isNotNull();
        assertThat(all).isEmpty();
    }

    @Test
    void aCourse_roundTrips_throughTheCabinet_fieldByField() {
        courseRepository.save(Course.builder()
                .title("Advanced Git")
                .description("Branches without fear")
                .price(new BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());

        var all = courseRepository.findAll();

        assertThat(all).hasSize(1);
        Course row = all.get(0);
        assertThat(row.getId()).isNotNull();
        assertThat(row.getTitle()).isEqualTo("Advanced Git");
        assertThat(row.getDescription()).isEqualTo("Branches without fear");
        assertThat(row.getPrice()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(row.getTargetAudience()).isEqualTo(Audience.OUTSIDER);
    }

    @Test
    void title_isRequiredByTheSteelDoor() {
        Course noTitle = Course.builder()
                .description("A course that forgot its own name")
                .targetAudience(Audience.INTERN)
                .build();

        assertThatThrownBy(() -> courseRepository.saveAndFlush(noTitle))
                .isInstanceOf(Exception.class);
    }
}
