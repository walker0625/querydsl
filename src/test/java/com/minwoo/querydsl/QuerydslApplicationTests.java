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
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Member member = new Member();
        em.persist(member);

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
        QMember qm = new QMember("M");
        Member member1 = jpaQueryFactory.selectFrom(qm).fetchOne();

        Assertions.assertThat(member1).isEqualTo(member);
    }

}
