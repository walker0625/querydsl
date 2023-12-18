package com.minwoo.querydsl;

import com.minwoo.querydsl.dto.MemberDto;
import com.minwoo.querydsl.dto.QMemberDto;
import com.minwoo.querydsl.dto.UserDto;
import com.minwoo.querydsl.entity.Member;
import com.minwoo.querydsl.entity.QMember;
import com.minwoo.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.minwoo.querydsl.entity.QMember.member;
import static com.minwoo.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
public class QueryDSLTest {

    @Autowired
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

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
        // QMember.member; // static import하여 사용 권장 > member
        // import static com.minwoo.querydsl.entity.QMember.member;

        Member member1 = queryFactory.select(member)
                                     .from(member)
                                     .where(member.username.eq("member1"))
                                     .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        Member findedMember = queryFactory.selectFrom(member)
                                          //.where(member.username.eq("member1")
                                          //     .and(member.age.between(10,30))
                                          .where(member.username.eq("member1"), // , => and의 의미
                                                  member.age.between(10,30)
                                          )
                                          .fetchOne();

        assertThat(findedMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
        Member findedMember = queryFactory.selectFrom(member).fetchOne();
        Member finded1Member = queryFactory.selectFrom(member).fetchFirst(); // limit 1

        // paging 용이
        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member).fetchResults();
        memberQueryResults.getTotal();
        List<Member> results = memberQueryResults.getResults();

        long count = queryFactory.selectFrom(member).fetchCount();
    }

    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> memberList = queryFactory.selectFrom(member).where(member.age.eq(100)).orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        for (Member member1 : memberList) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void paging() {
        List<Member> memberList = queryFactory.selectFrom(member)
                                              .orderBy(member.username.desc())
                                              .offset(1)
                                              .limit(2)
                                              .fetch();

        assertThat(memberList.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getOffset()).isEqualTo(0);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(member.count(), member.age.sum(), member.age.avg())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }

    @Test
    void group() {

        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    void join() {
        List<Member> memberList = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(memberList)
                .extracting("username")
                .containsExactly("member1", "member2");
    }
    
    @Test
    void join_on_filtering() {
        List<Member> memberListInnerJoin = queryFactory.select(member)
                .from(member)
                .join(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();

        for (Member member1 : memberListInnerJoin) {
            System.out.println("member1 = " + member1);
        }

        List<Member> memberListLeftJoin = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team) // .leftJoin(team) 으로 하면 카테시안 조인됨 주의
                .on(team.name.eq("teamA")) // outer join의 경우에 사용
                .fetch();

        for (Member member2 : memberListLeftJoin) {
            System.out.println("member2 = " + member2);
        }

        List<Member> memberListLeftJoinOn = queryFactory
                .select(member)
                .from(member)
                .leftJoin(team) // .leftJoin(team) 으로 하면 카테시안 조인됨 주의
                .on(member.username.eq(team.name)) // outer join의 경우에 사용
                .fetch();

        for (Member member3 : memberListLeftJoinOn) {
            System.out.println("member3 = " + member3);
        }
    }

    @Test
    void noFetchJoin() {
        em.flush();
        em.clear();

        Member findedMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //Lazy 로딩
        boolean loadedFlag = emf.getPersistenceUnitUtil().isLoaded(findedMember.getTeam());

        assertThat(loadedFlag).isFalse();
    }

    @Test
    void fetchJoin() {
        em.flush();
        em.clear();

        Member findedMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin() // 이 부분만 추가됨
                .where(member.username.eq("member1")).fetchOne();
        boolean loadedFlag = emf.getPersistenceUnitUtil().isLoaded(findedMember.getTeam());

        assertThat(loadedFlag).isTrue();
    }

    @Test
    void subQuery() {

        // sub query용 member
        QMember memberSub = new QMember("memberSub");

        Member fmember = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        //JPAExpressions.select(subMember.age.max())
                        select(memberSub.age.max())
                        .from(memberSub) // sub query
                )).fetchOne();

        assertThat(fmember.getAge()).isEqualTo(40);
    }

    @Test
    void subQueryGOE() {
        QMember subMember = new QMember("subMember");

        List<Member> memberList = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(subMember.age.avg())
                        .from(subMember)))
                .fetch();

        assertThat(memberList).extracting("age").containsExactly(30, 40);
    }

    @Test
    void subQueryIN() {
        QMember subMember = new QMember("subMember");

        List<Member> memberList = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(subMember.age)
                        .from(subMember)
                        .where(subMember.age.gt(10))))
                .fetch();

        assertThat(memberList).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    void scalarSubQuery() {

        QMember subMember = new QMember("subMember");

        List<Tuple> fetch = queryFactory.select(member.username,
                //JPAExpressions.select(subMember.age.avg()).from(subMember)
                select(subMember.age.avg()).from(subMember)
        ).from(member).fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    // subquery
    // from 절(인라인뷰)에 subQuery는 불가(jpql 자체가 지원하지 않음)
    // 1. join으로 해결 2. 쿼리를 2번 실행 3. native 쿼리로 실행

    @Test
    void basicCase() {
        List<String> ages = queryFactory
                .select(member.age
                    .when(10).then("초등학생")
                    .when(20).then("대학생")
                    .otherwise("기타"))
                .from(member)
                .fetch();

        for (String age : ages) {
            System.out.println("age = " + age);
        }
    }
    
    @Test
    void complexCase() {
        List<String> ages = queryFactory.select(new CaseBuilder()
                                            .when(member.age.between(0, 20)).then("미성년자")
                                            .otherwise("성인"))
                                        .from(member)
                                        .fetch();

        for (String age : ages) {
            System.out.println("age = " + age);
        }
    }

    // case when 절을 db에서 하는 것은 비권장(service나 화면에서 처리 권장)
    
    @Test
    void constant() {
        List<Tuple> results = queryFactory.select(member.username, Expressions.constant("A"))
                                          .from(member).fetch();

        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
    }
    
    @Test
    void concat() {
        List<String> results = queryFactory.select(
                    member.username
                    .concat("_")
                    .concat(member.age.stringValue())) // ENUM 처리 때도 유용
                .from(member)
                .fetch();

        for (String result : results) {
            System.out.println("result = " + result);
        }
    }

    // advance queryDSL

    @Test
    void simpleProjection() {

        List<String> results = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void tupleProjection() {

        List<Tuple> results = queryFactory
                                        .select(member.username, member.age)
                                        .from(member)
                                        .fetch();

        // Tuple을 repository를 넘어서 사용하는 것은 지양(dto 등으로 변환)
        for (Tuple result : results) {
            System.out.println("result.get(member.username) = " + result.get(member.username));
            System.out.println("result.get(member.age) = " + result.get(member.age));
        }

    }

    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new com.minwoo.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                                       .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByQueryDSL() {

        List<MemberDto> memberDtoList = queryFactory
                //.select(Projections.bean(MemberDto.class, member.username, member.age)) // by setter
                //.select(Projections.fields(MemberDto.class, member.username, member.age)) // direct field(private도 가능)
                .select(Projections.constructor(MemberDto.class, member.username, member.age)) // constructor(runtime에만 에러 파악 가능)
                .from(member)
                .fetch();

        for (MemberDto memberDto : memberDtoList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByQueryDSL2() {

        QMember subMember = new QMember("memberSub");

        List<UserDto> userDtoList = queryFactory
            .select(Projections.fields(UserDto.class, member.username.as("name"),
                    ExpressionUtils.as(
                        JPAExpressions.select(subMember.age.max()).from(subMember), "age"
                    ))) // constructor
            .from(member)
            .fetch();

        for (UserDto userDto : userDtoList) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryDSL3() {

        List<MemberDto> memberDtoList = queryFactory
                // @QueryProjection 으로 생성된 QMemberDto를 바로 사용
                // 문제점 1. build/run 등을 통해 생성 2. dto에 queryDSL에 의존성이 생김
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : memberDtoList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();

        if(usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory.selectFrom(member)
                           .where(builder)
                           .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory.selectFrom(member)
                           //.where(usernameEq(usernameParam), ageEq(ageParam))
                           .where(getWantMember(usernameParam, ageParam))
                           .fetch();
    }
    
    // Predicate를 interface로 하여 유연하게 변경 가능
    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    // 1. 재사용이 가능 2. 조건 자체에 이름을 붙혀서 의미 부여 가능
    private BooleanExpression getWantMember(String usernameParam, Integer ageParam) {
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }
    
    @Test
    //@Commit
    void bulkUpdate() {

        // bulk성 쿼리는 실제 db와 1차 캐시간의 data sync가 맞지 않을 수 있음
        // jpa는 db data보다 1차 캐시가 우선권을 가짐
        queryFactory
            .update(member)
            .set(member.username, "not member")
            .where(member.age.lt(25))
            .execute();
        
        // 1차 캐시 초기화!
        em.flush();
        em.clear();

        List<Member> memberList = queryFactory.selectFrom(member).fetch();

        for (Member member1 : memberList) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void bulkAdd() {
        queryFactory.update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() {
        queryFactory.delete(member)
                .where(member.age.gt(20))
                .execute();
    }

    @Test
    @Commit
    void sqlFunction() {
        List<String> results = queryFactory.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                                           .from(member)
                                           .fetch();

        for (String result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void sqlFunction2() {
        List<String> lowerMemberList = queryFactory.select(member.username)
                                                   .from(member)
                                                   .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                                                   .from(member)
                                                   .fetch();

        for (String member : lowerMemberList) {
            System.out.println("member = " + member);
        }
    }

    @Test
    void sqlFunction3() {
        List<String> lowerMemberList = queryFactory.select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower())) // 기본적인 function은 제공
                .from(member)
                .fetch();

        for (String member : lowerMemberList) {
            System.out.println("member = " + member);
        }
    }

}