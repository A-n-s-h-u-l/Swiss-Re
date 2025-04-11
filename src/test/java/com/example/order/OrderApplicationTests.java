package com.example.order;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.example.order.domain.Product;
import com.example.order.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApplicationTests {
	@Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepo;

	@BeforeEach
    void setup() {
        productRepo.save(new Product(1L, "Test Product", 10));
    }
	// @Test
	// void contextLoads() {
	// }
	// @Test./mvnw test

    // void testOrderCreation() throws Exception {
    //     String body = "{\"userId\":1, \"productIds\":[1]}";
    //     mockMvc.perform(post("/order")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(body))
    //             .andExpect(status().isOk());
    // }
}
