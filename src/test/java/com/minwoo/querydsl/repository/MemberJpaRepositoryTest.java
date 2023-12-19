package com.minwoo.querydsl.repository;

import com.minwoo.querydsl.dto.MemberSearchCondition;
import com.minwoo.querydsl.dto.MemberTeamDto;
import com.minwoo.querydsl.entity.Member;
import com.minwoo.querydsl.entity.Team;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void jpaTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findedMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findedMember);

        List<Member> members = memberJpaRepository.findAll();
        assertThat(members).containsExactly(member);

        List<Member> member1 = memberJpaRepository.findByUsername("member1");
        assertThat(member1).containsExactly(member);
    }

    @Test
    void dslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        List<Member> members = memberJpaRepository.findAll_dsl();
        assertThat(members).containsExactly(member);

        List<Member> member1 = memberJpaRepository.findByUsername_dsl("member1");
        assertThat(member1).containsExactly(member);
    }

    @Test
    void searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        //condition.setAgeGoe(35);
        //condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        //List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);
        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchWhere(condition);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }

        assertThat(memberTeamDtos).extracting("username").containsExactly("member3", "member4");
    }
}