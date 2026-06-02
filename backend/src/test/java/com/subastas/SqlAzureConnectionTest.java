package com.subastas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SqlAzureConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testAzureSqlConnection() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            assertNotNull(c);
            System.out.println("DB connection OK, auto-commit=" + c.getAutoCommit());
        }
    }
}
