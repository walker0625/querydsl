package com.minwoo.querydsl;

import com.minwoo.querydsl.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.minwoo.querydsl.entity.Member;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class QuerydslApplicationTests {


    @Test
    void contextLoads() {
    }

}
