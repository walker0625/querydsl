package com.minwoo.querydsl.repository;

import com.minwoo.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
// QuerydslPredicateExecutor<Member> : left join이 불가(실무 사용이 어려움)
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository, QuerydslPredicateExecutor<Member> {

    List<Member> findByUsername(String username);

}
