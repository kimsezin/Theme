package com.ssafy.user.repository;

import com.ssafy.user.entity.Follow;
import com.ssafy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Integer> {

    @Query("Select F.themeIdx From Follow F where F.followUser =:user")
    List<Integer> findThemeIdByFollowingUser(@Param("user") User user);

    @Query("Select distinct(F.followingUser.idx) From Follow F where F.followingUser =:user")
    List<Integer> findFollowerByUser(@Param("user") User user);

    @Query("Select distinct(F.followUser.idx) From Follow F where F.followingUser =:user")
    List<Integer> findFollower(@Param("user") User user);

    @Query("Select distinct (F.followingUser.idx) From Follow F where F.followUser=:user")
    List<Integer> findFollowingByUser(@Param("user") User user);

    List<Follow> findByFollowUserAndFollowingUser(User FollowerUser, User FollowingUser);

    List<Follow> findByFollowUserOrFollowingUser(User FollowerUser, User FollowingUser);

    Optional<Follow> findByFollowUserAndFollowingUserAndThemeIdx(User FollowUser, User FollowingUser, int themeIdx);


    @Query("Select F.followingUser From Follow F group by F.followingUser order by (count(F.followingUser)) desc")
    List<User> searchRecommned();

    List<Follow> findByFollowingUser(User user);

    @Query("select F.themeIdx from Follow F group by F.themeIdx order by count(F.themeIdx) desc")
    List<Integer> countByThemeIdx();

    Optional<Follow> findByFollowingUserAndFollowUserAndThemeIdx(User FollowingUser, User FollowerUser,int themeIdx);

    Optional<Follow> findByThemeIdxAndFollowUser(int theme_idx, User FollowUser);
    List<Follow> findByFollowUser(User followUser);

    List<Follow> findByThemeIdx(int theme_idx);
}
