package com.github.hystrixplayground;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class HystrixPlaygroundApplicationTests {

    @Autowired
    private MockMvc mockMvc;

	@Test
	public void contextLoads() {
	}

	@Test
    public void hystrixDashboardIsAccessible() throws Exception {
        mockMvc.perform(get("/hystrix")).andExpect(status().isOk());
    }

}
