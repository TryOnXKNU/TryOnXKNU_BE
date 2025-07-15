package org.example.tryonx.admin.specification;

import org.example.tryonx.member.domain.Member;
import org.springframework.data.jpa.domain.Specification;

public class MemberSpecification {
    public static Specification<Member> search(String key, String value) {
        return (root, query, cb) -> {
            if (key == null || value == null || value.trim().isEmpty()) return null;

            return switch (key) {
                case "name"      -> cb.like(root.get("name"), "%" + value + "%");
                case "memberId"  -> cb.equal(root.get("memberId"), Long.parseLong(value));
                case "email"     -> cb.like(root.get("email"), "%" + value + "%");
                case "phoneNumber"     -> cb.like(root.get("phoneNumber"), "%" + value + "%");
                default          -> throw new IllegalArgumentException("지원되지 않는 검색 항목입니다: " + key);
            };
        };
    }
}
