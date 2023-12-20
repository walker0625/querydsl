package com.minwoo.querydsl.repository;

import com.minwoo.querydsl.dto.MemberSearchCondition;
import com.minwoo.querydsl.dto.MemberTeamDto;
import com.minwoo.querydsl.dto.QMemberTeamDto;
import com.minwoo.querydsl.entity.Member;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.minwoo.querydsl.entity.QMember.member;
import static com.minwoo.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryImpl implements MemberCustomRepository{

    /*
    extends QuerydslRepositorySupport

    public MemberRepositoryImpl() {
        super(Member.class);
    }

    // QueryDSL 관련 Util(Paging)을 지원하기는 하나 단점들이 장점에 비해 많아보임
    // 1. from 부터 시작하는 방식(3.0 방식) +
    // 2. 의존성이 늘어남(불필요한 복잡성)
    // 3. Sort는 버그 존재(QSort는 가능)
    // 4. JPAQueryFactory queryFactory 주입 방식 변경
    */

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where( // 조건들을 조합/재사용이 가능함
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                        //ageLoe(condition.getAgeLoe()),
                        //ageGoe(condition.getAgeGoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where( // 조건들을 조합/재사용이 가능함
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                        //ageLoe(condition.getAgeLoe()),
                        //ageGoe(condition.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> contents = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(contents, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        List<MemberTeamDto> contents = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where( // 조건들을 조합/재사용이 가능함
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                        //ageLoe(condition.getAgeLoe()),
                        //ageGoe(condition.getAgeGoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // join 같은 쿼리가 필요없는 경우는 간단한 쿼리로 변경(최적화)
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where( // 조건들을 조합/재사용이 가능함
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
                        //ageLoe(condition.getAgeLoe()),
                        //ageGoe(condition.getAgeGoe())
                );

        // count(total)가 필요하지 않은 경우
        // 1. 첫페이지인데 contents보다 size가 더 클 경우 -> 가져온 contents 수가 count가 됨
        // 2. 마지막 페이지일 경우 -> offset + 가져온 contents 수가 count가 됨
        return PageableExecutionUtils.getPage(contents, pageable, countQuery::fetchCount);
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        // 둘다 null인 경우 true(1) 응답(그냥 null로 응답하면 500 에러 발생)
        if(ageLoe == null && ageGoe == null) {
            return Expressions.TRUE;
        }

        return ageGoe(ageLoe).and(ageLoe(ageGoe));
    }

}