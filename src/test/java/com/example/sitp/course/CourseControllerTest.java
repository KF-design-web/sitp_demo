package com.example.sitp.course;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    private void cleanTables() {
        courseRepository.deleteAll();
    }

    private Course course(String title, Audience audience) {
        return Course.builder()
                .title(title)
                .description("A course called " + title)
                .targetAudience(audience)
                .build();
    }

    @Test
    void loggedOutVisitor_gets200AndTheList() throws Exception {
        cleanTables();

        courseRepository.save(course("Spring Boot Basics", Audience.INTERN));
        courseRepository.save(course("Advanced Git", Audience.OUTSIDER));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title",
                        hasItem("Spring Boot Basics")))
                .andExpect(jsonPath("$[*].title", hasItem("Advanced Git")))
                .andExpect(jsonPath("$[*].targetAudience",
                        hasItem("INTERN")))
                .andExpect(jsonPath("$[*].targetAudience",
                        hasItem("OUTSIDER")));

    }

    @Test
    void loggedOutVisitor_seesNoPriceLine_ever() throws Exception {
        cleanTables();
        courseRepository.save(Course.builder()
                .title("Advanced Git")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].price").doesNotExist());
    }

    @Test
    void emptyCatalog_answersEmptyList_not404() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }
}
