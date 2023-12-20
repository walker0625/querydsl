package com.minwoo.querydsl.repository;

import com.minwoo.querydsl.dto.MemberSearchCondition;
import com.minwoo.querydsl.dto.MemberTeamDto;
import com.minwoo.querydsl.entity.Member;
import com.minwoo.querydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.minwoo.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired MemberRepository memberRepository;
    @PersistenceContext EntityManager em;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findedMember = memberRepository.findById(member.getId()).get();
        assertThat(member).isEqualTo(findedMember);

        List<Member> members = memberRepository.findAll();
        assertThat(members).containsExactly(member);

        List<Member> member1 = memberRepository.findByUsername("member1");
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
        List<MemberTeamDto> memberTeamDtos = memberRepository.search(condition);

        for (MemberTeamDto memberTeamDto : memberTeamDtos) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }

        assertThat(memberTeamDtos).extracting("username").containsExactly("member3", "member4");
    }

    @Test
    void searchPageSimpleTest() {
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

        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> results = memberRepository.searchPageSimple(condition, pageRequest);
        Page<MemberTeamDto> results2 = memberRepository.searchPageComplex(condition, pageRequest);

        assertThat(results.getSize()).isEqualTo(3);
        assertThat(results2.getSize()).isEqualTo(3);
    }
    
    @Test
    void queryDslPredicateExecutorTest() {
        Iterable<Member> member1 = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));

        for (Member member2 : member1) {
            System.out.println("member2 = " + member2);
        }
    }

}