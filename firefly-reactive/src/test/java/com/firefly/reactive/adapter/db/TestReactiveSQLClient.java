package com.firefly.reactive.adapter.db;

import com.firefly.db.jdbc.JDBCClient;
import com.firefly.reactive.adapter.Reactor;
import com.firefly.utils.function.Func1;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestReactiveSQLClient {

    private ReactiveSQLClient sqlClient;
    private int size = 10;

    public TestReactiveSQLClient() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        sqlClient = Reactor.db.fromSQLClient(new JDBCClient(ds));
    }

    private <T> Mono<T> exec(Func1<ReactiveSQLConnection, Mono<T>> func1) {
        return sqlClient.newTransaction(func1);
    }

    @Before
    public void before() throws Exception {
        exec(c -> c.update("drop schema if exists test")
                   .then(v -> c.update("create schema test"))
                   .then(v -> c.update("set mode MySQL"))
                   .then(v -> c.update("CREATE TABLE `test`.`user`(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))"))
                   .then(v -> {
                       Object[][] params = new Object[size][2];
                       for (int i = 0; i < size; i++) {
                           params[i][0] = "test transaction " + i;
                           params[i][1] = "pwd transaction " + i;
                       }
                       String sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)";
                       return c.insertBatch(sql, params, (rs) -> rs.stream().map(r -> r.getInt(1)).collect(Collectors.toList()));
                   })).doOnSuccess(System.out::println).block();
    }

    @After
    public void after() throws Exception {
        exec(c -> c.update("DROP TABLE IF EXISTS `test`.`user`")).block();
        System.out.println("drop table user");
    }

    @Test
    public void test() throws Exception {
        exec(c -> testUser(c, 1)).block();
    }

    private Mono<Long> testUser(ReactiveSQLConnection c, long i) {
        return c.queryById(i, User.class).then(user -> {
            Assert.assertThat(user.getId(), is(i));
            Assert.assertThat(user.getName(), is("test transaction " + (i - 1)));
            Assert.assertThat(user.getPassword(), is("pwd transaction " + (i - 1)));
            System.out.println("query user -> " + user);
            if (i < size) {
                return testUser(c, i + 1);
            } else {
                return Mono.create(monoSink -> monoSink.success(i));
            }
        });
    }

    @Test
    public void testRollback() throws Exception {
        Long id = 1L;
        exec(c -> {
            User user = new User();
            user.setId(id);
            user.setName("apple");
            return c.updateObject(user)
                    .doOnSuccess(row -> Assert.assertThat(row, is(1)))
                    .then(v -> c.queryById(id, User.class))
                    .doOnSuccess(user1 -> Assert.assertThat(user1.getName(), is("apple")))
                    .then(v -> c.rollback());
        }).then(ret -> exec(c -> c.queryById(id, User.class)))
          .doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
          .block();
    }

    @Test
    public void testRollback2() throws Exception {
        exec(c -> {
            User user0 = new User();
            user0.setId(2L);
            user0.setName("orange");
            return c.updateObject(user0)
                    .doOnSuccess(row0 -> Assert.assertThat(row0, is(1)))
                    .then(v -> c.queryById(2L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("orange")))
                    .then(v -> c.inTransaction(c1 -> {
                        User user1 = new User();
                        user1.setId(1L);
                        user1.setName("apple");
                        return c1.updateObject(user1)
                                 .doOnSuccess(row1 -> Assert.assertThat(row1, is(1)))
                                 .then(v1 -> c1.queryById(1L, User.class))
                                 .doOnSuccess(user -> Assert.assertThat(user.getName(), is("apple")))
                                 .then(v1 -> c1.rollback());
                    }))
                    .then(v -> c.queryById(1L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
                    .then(v -> c.queryById(2L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 1")));
        }).block();
    }
}
