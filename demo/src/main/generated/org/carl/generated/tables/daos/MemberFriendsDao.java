/*
 * This file is generated by jOOQ.
 */
package org.carl.generated.tables.daos;

import java.util.List;
import java.util.Optional;
import org.carl.generated.tables.MemberFriends;
import org.carl.generated.tables.records.MemberFriendsRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class MemberFriendsDao
        extends DAOImpl<MemberFriendsRecord, org.carl.generated.tables.pojos.MemberFriends, Long> {

    /**
     * Create a new MemberFriendsDao without any configuration
     */
    public MemberFriendsDao() {
        super(MemberFriends.MEMBER_FRIENDS, org.carl.generated.tables.pojos.MemberFriends.class);
    }

    /**
     * Create a new MemberFriendsDao with an attached configuration
     */
    public MemberFriendsDao(Configuration configuration) {
        super(
                MemberFriends.MEMBER_FRIENDS,
                org.carl.generated.tables.pojos.MemberFriends.class,
                configuration);
    }

    @Override
    public Long getId(org.carl.generated.tables.pojos.MemberFriends object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    public List<org.carl.generated.tables.pojos.MemberFriends> fetchRangeOfId(
            Long lowerInclusive, Long upperInclusive) {
        return fetchRange(MemberFriends.MEMBER_FRIENDS.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.carl.generated.tables.pojos.MemberFriends> fetchById(Long... values) {
        return fetch(MemberFriends.MEMBER_FRIENDS.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.carl.generated.tables.pojos.MemberFriends fetchOneById(Long value) {
        return fetchOne(MemberFriends.MEMBER_FRIENDS.ID, value);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public Optional<org.carl.generated.tables.pojos.MemberFriends> fetchOptionalById(Long value) {
        return fetchOptional(MemberFriends.MEMBER_FRIENDS.ID, value);
    }
}
