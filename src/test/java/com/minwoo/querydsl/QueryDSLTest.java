package com.minwoo.querydsl;

import com.minwoo.querydsl.entity.Member;
import com.minwoo.querydsl.entity.QMember;
import com.minwoo.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.minwoo.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QueryDSLTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        // multi thread 문제가 없으므로 신경 쓰지 않아도 됨
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        Member member = em.createQuery("select m from Member m where m.username = :username", Member.class).setParameter("username", "member1").getSingleResult();
        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    void startDSL() {
        //JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        // QMember m = new QMember("m"); : 같은 테이블 join 경우는 사용
        // import static com.minwoo.querydsl.entity.QMember.member;

        Member member1 = queryFactory.select(member)
                                     .from(member)
                                     .where(member.username.eq("member1"))
                                     .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

}
