package com.ssafy.theme.service.impl;

import com.ssafy.theme.client.FeedClient;
import com.ssafy.theme.client.UserClient;
import com.ssafy.theme.dto.theme.*;
import com.ssafy.theme.entity.Scrap;
import com.ssafy.theme.entity.Theme;
import com.ssafy.theme.entity.UserTheme;
import com.ssafy.theme.repository.ScrapRepository;
import com.ssafy.theme.repository.ThemeRepository;
import com.ssafy.theme.repository.UserThemeRepository;
import com.ssafy.theme.service.ThemeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ThemeServiceImpl implements ThemeService {
    ThemeRepository themeRepository;
    UserThemeRepository userThemeRepository;
    ScrapRepository scrapRepository;
    UserClient userClient;
    FeedClient feedClient;
    @Autowired
    ThemeServiceImpl(ThemeRepository themeRepository,
                     UserThemeRepository userThemeRepository,
                     UserClient userClient,
                     ScrapRepository scrapRepository,
                     FeedClient feedClient) {
        this.themeRepository = themeRepository;
        this.userThemeRepository = userThemeRepository;
        this.userClient = userClient;
        this.scrapRepository = scrapRepository;
        this.feedClient = feedClient;
    }
    @Override
    public int registTheme(ThemeRegistDto themeRegistDto,int userIdx) {
        Optional<Theme> searchTheme = themeRepository.findByName(themeRegistDto.getName());
        if(!searchTheme.isPresent()){ //공용 테마에 이름이 없다면
                Theme theme = Theme.builder()
                        .name(themeRegistDto.getName())
                        .emoticon(themeRegistDto.getEmoticon())
                        .createTime(LocalDateTime.now())
                        .build();
                themeRepository.save(theme);
                return theme.getIdx();
        }
        return -1;
    }
    @Override
    public int createUserTheme(int userIdx, UserThemeRegistDto userThemeRegistDto) {
        Theme theme = themeRepository.findByIdx(userThemeRegistDto.getThemeIdx());
        if(!userThemeRepository.findByThemeAndUserIdx(theme, userIdx).isPresent()){// 유저 테마 보유한지 확인하기

            UserTheme userTheme = UserTheme.builder()
                    .theme(theme)
                    .userIdx(userIdx)
                    .createTime(userThemeRegistDto.getCreateTime())
                    .challenge(userThemeRegistDto.isChallenge())
                    .description(userThemeRegistDto.getDescription())
                    .modifyTime(userThemeRegistDto.getCreateTime())
                    .openType(userThemeRegistDto.getOpenType())
                    .build();
            UserTheme save = userThemeRepository.save(userTheme);
            return save.getIdx();
        }
        return -1;

    }
    @Override
    public List<UserThemeDto> getThemeList(int user_id) {
        List<UserThemeDto> result = new ArrayList<>();

        List<UserTheme> themeList = userThemeRepository.findByUserIdx(user_id);

        for(int i=0;i<themeList.size();i++) {
            UserTheme userTheme = themeList.get(i);

            UserThemeDto target = UserThemeDto.builder()
                    .idx(userTheme.getIdx())
                    .themeIdx(userTheme.getTheme().getIdx())
                    .challenge(userTheme.isChallenge())
                    .description(userTheme.getDescription())
                    .modifyTime(userTheme.getModifyTime())
                    .createTime(userTheme.getCreateTime())
                    .openType(userTheme.getOpenType())
                    .userIdx(userTheme.getUserIdx())
                    .name(userTheme.getTheme().getName())
                    .build();

            result.add(target);
        }

        return result;
    }

    @Override
    public ResponseEntity<?> getUserInfo(String nickname) {
        ResponseEntity<?> userInfo = userClient.getUserInfo(nickname);

        return userInfo;
    }

    @Override
    public String deleteBoardAndComment(int theme_idx) {
        return feedClient.deleteBoardAndComment(theme_idx);
    }

    @Override
    public ResponseEntity<?> getUserIdxInfo(int userIdx) {
        ResponseEntity<?> userInfo = userClient.getUserIdxInfo(userIdx);

        return userInfo;
    }

    @Override
    public List<PublicThemeListDto> getPublicThemeList(int userIdx,int sort, int pageSize, int pageIdx) {
        List<PublicThemeListDto> resultList = new ArrayList<>();
        Slice<PublicThemeDto> themeList;
        PageRequest pageable = PageRequest.of(pageIdx, pageSize);
            if (sort == 0) { // 인기순
                themeList = userThemeRepository.getPopularAllThemeListWithJPA( pageable);
            } else if(sort == 1) {//최신순
                themeList = userThemeRepository.getRecnetAllThemeListWithJPA( pageable);
            }else{
                return null;
            }
        for(PublicThemeDto publicThemeDto : themeList){
            List<Scrap> scraps = scrapRepository.findByUserIdx(userIdx);
            boolean flag = false;
            for(int i=0;i<scraps.size();i++){
                if(scraps.get(i).getTheme().getIdx()==publicThemeDto.getIdx()) {
                    flag = true;
                    break;
                }
            }
            PublicThemeListDto addPublicThemeDto = PublicThemeListDto.builder()
                    .idx(publicThemeDto.getIdx())
                    .emoticon(publicThemeDto.getEmoticon())
                    .name(publicThemeDto.getName())
                    .userCount(publicThemeDto.getUserCount())
                    .createTime(publicThemeDto.getCreateTime())
                    .isBookmarked(flag)
                    .build();
            resultList.add(addPublicThemeDto);
        }
        return resultList;
    }
    public List<SearchThemeDto> searchTheme(String target, int userIdx) {
        List<SearchThemeDto> result = new ArrayList<>();

        //해당 단어를 가진 테마들
        List<Theme> targetThemeList = themeRepository.searchByTarget(target);

        //해당 유저가 가진 유저테마들
        List<UserTheme> userThemeList = userThemeRepository.findByUserIdx(userIdx);
        Map<String, Integer> map = new HashMap<>();

        for(int i=0;i<userThemeList.size();i++)
            map.put(userThemeList.get(i).getTheme().getName(), userThemeList.get(i).getOpenType());

        for(int i=0;i<targetThemeList.size();i++) {
            Theme theme = targetThemeList.get(i);

            //openType 0/1/2에 1씩 더해서 1/2/3 배분 후 없는거는 다시 for문
            if(map.get(theme.getName()) != null) {
                SearchThemeDto searchThemeDto =  SearchThemeDto.builder()
                        .themeIdx(theme.getIdx())
                        .createTime(theme.getCreateTime())
                        .name(theme.getName())
                        .emoticon(theme.getEmoticon())
                        .openType(map.get(theme.getName())+1)
                        .build();

                result.add(searchThemeDto);
            }
        }

        for(int i=0;i<targetThemeList.size();i++) {
            Theme theme = targetThemeList.get(i);

            //openType 0/1/2에 1씩 더해서 1/2/3 배분 후 없는거는 다시 for문
            if(map.get(theme.getName()) == null) {
                SearchThemeDto searchThemeDto =  SearchThemeDto.builder()
                        .themeIdx(theme.getIdx())
                        .createTime(theme.getCreateTime())
                        .name(theme.getName())
                        .emoticon(theme.getEmoticon())
                        .openType(0)
                        .build();

                result.add(searchThemeDto);
            }
        }
        return result;
    }

    @Override
    public void scrapTheme(int userIdx, int themeIdx) {
        Theme theme = themeRepository.findByIdx(themeIdx);
        Scrap scrap = Scrap.builder()
                .theme(theme)
                .userIdx(userIdx)
                .build();

        scrapRepository.save(scrap);
    }

    @Override
    public void deleteScrapTheme(int user_id, int theme_idx) {
        Theme targetTheme = themeRepository.findByIdx(theme_idx);
        if(scrapRepository.existsByThemeAndUserIdx(targetTheme, user_id)) {
            Scrap scrap = scrapRepository.findByThemeAndUserIdx(targetTheme, user_id).orElseThrow(IllegalAccessError::new);

            scrapRepository.delete(scrap);
        }
    }

    @Override
    public List<UserThemeDto> followThemeList(UserThemeIdxDto userThemeIdxDto) {
        List<UserThemeDto> result = new ArrayList<>();

        List<Integer> userThemeList = userThemeIdxDto.getUserThemeList();
        for(int i=0; i<userThemeList.size();i++) {

            int userThemeIdx = userThemeList.get(i);
            // System.out.println("userThemeIdx : " + userThemeIdx);
            UserTheme userTheme = userThemeRepository.findById(userThemeIdx).orElseThrow(IllegalAccessError::new);

            UserThemeDto userThemeDto = UserThemeDto.builder()
                    .themeIdx(userTheme.getTheme().getIdx())
                    .name(userTheme.getTheme().getName())
                    .userIdx(userTheme.getUserIdx())
                    .createTime(userTheme.getCreateTime())
                    .challenge(userTheme.isChallenge())
                    .description(userTheme.getDescription())
                    .modifyTime(userTheme.getModifyTime())
                    .openType(userTheme.getOpenType())
                    .idx(userTheme.getIdx())
                    .emoticon(userTheme.getTheme().getEmoticon())
                    .build();

            result.add(userThemeDto);
        }
        return result;
    }

    @Override
    public List<PublicThemeListDto> getBookmarkThemeList(int userIdx) {
        List<Scrap> scrapList = scrapRepository.findByUserIdx(userIdx);
        List<PublicThemeListDto> publicThemeDtoList = new ArrayList<>();
        for(Scrap scrap : scrapList){
            Long themeCount = userThemeRepository.getThemeCountWithJPA(scrap.getTheme().getIdx());
            PublicThemeListDto publicThemeDto = PublicThemeListDto.builder()
                    .idx(scrap.getIdx())
                    .createTime(scrap.getTheme().getCreateTime())
                    .emoticon(scrap.getTheme().getEmoticon())
                    .name(scrap.getTheme().getName())
                    .userCount(themeCount)
                    .isBookmarked(true)
                    .build();
            publicThemeDtoList.add(publicThemeDto);
        }
        return publicThemeDtoList;
    }
    public List<LiveThemeDto> liveSearchTheme(String value,int userIdx) {
        List<LiveThemeDto> liveThemeDtos = new ArrayList<>();
        List<String> strings = themeRepository.liveSearchByName(value);
        for(int i=0;i<strings.size();i++) {
            boolean isMy = false;
            Optional<Theme> theme = themeRepository.findByName(strings.get(i));
            Optional<UserTheme> userTheme = userThemeRepository.findByThemeAndUserIdx(theme.get(),userIdx);
            if(userTheme.isPresent()) isMy = true;
            LiveThemeDto liveThemeDto = LiveThemeDto.builder()
                    .name(strings.get(i))
                    .isMy(isMy)
                    .build();
            liveThemeDtos.add(liveThemeDto);
        }
        return liveThemeDtos;
    }
    @Override
    public String getThemeName(int theme_idx) {
        Theme theme = themeRepository.findByIdx(theme_idx);
        return theme.getName();
    }

    @Override
    public Map<String, Object> searchThemeInfo(String value,int userIdx) {
        Map<String, Object> answer = new HashMap<>();
        List<ThemeListDto> result = new ArrayList<>();
        boolean same = themeRepository.findByName(value).isPresent();
        int sameThemeIdx = 0;
        if(same) { // 아예 같은 값
            Theme theme = themeRepository.findByName(value).orElseThrow(IllegalAccessError::new);
            sameThemeIdx = theme.getIdx();
            BoardInfoDto boardInfoDto = boardInfoByTheme(theme.getIdx());
            Optional<Scrap> scrap = scrapRepository.findByThemeAndUserIdx(theme,userIdx); // 보는 사람이 그 테마를 스크랩 했는지
            boolean flag = false;
            if(scrap.isPresent()) flag = true;
            List<UserTheme> userThemes = userThemeRepository.findByTheme(theme); // 총 몇명이 테마를 참여하는지
            ThemeListDto themeListDto = ThemeListDto.builder()
                    .boardCount(boardInfoDto.getBoardCount())
                    .isBookMarked(flag)
                    .createTime(theme.getCreateTime())
                    .personCount(userThemes.size())
                    .pictures(boardInfoDto.getPictures())
                    .emoticon(theme.getEmoticon())
                    .name(theme.getName())
                    .themeIdx(theme.getIdx())
                    .build();
            result.add(themeListDto);
        }
        List<Theme> themes = themeRepository.searchByTarget(value);
        for(int i=0;i<themes.size();i++) {
                BoardInfoDto boardInfoDto = boardInfoByTheme(themes.get(i).getIdx());
                if(sameThemeIdx!=themes.get(i).getIdx()) { // 위에서 겹치는 거 제외하기
                    Optional<Scrap> scrap = scrapRepository.findByThemeAndUserIdx(themes.get(i), userIdx); // 보는 사람이 그 테마를 스크랩 했는지
                    boolean flag = false;
                    if (scrap.isPresent()) flag = true;
                    List<UserTheme> userThemes = userThemeRepository.findByTheme(themes.get(i)); // 총 몇명이 테마를 참여하는지
                    ThemeListDto themeListDto = ThemeListDto.builder()
                            .boardCount(boardInfoDto.getBoardCount())
                            .isBookMarked(flag)
                            .createTime(themes.get(i).getCreateTime())
                            .personCount(userThemes.size())
                            .pictures(boardInfoDto.getPictures())
                            .emoticon(themes.get(i).getEmoticon())
                            .name(themes.get(i).getName())
                            .themeIdx(themes.get(i).getIdx())
                            .build();
                    result.add(themeListDto);
                }
        }
        answer.put("result",result);
        answer.put("isSame", same);
        return answer;
    }

    @Override
    public List<UserThemeDtoWithMSA> getThemeUserList(int theme_idx,int user_idx) {
        Theme theme = themeRepository.findByIdx(theme_idx);
        List<UserTheme> userThemeList = userThemeRepository.findByTheme(theme);
        List<UserThemeDtoWithMSA> userThemeDtoList = new ArrayList<>();
        for(UserTheme userTheme : userThemeList){
            if(userTheme.getOpenType()==0){
                UserThemeDtoWithMSA userThemeDto = UserThemeDtoWithMSA.builder()
                        .idx(userTheme.getTheme().getIdx())
                        .userThemeIdx(userTheme.getIdx())
                        .userIdx(userTheme.getUserIdx())
                        .themeEmoticon(userTheme.getTheme().getEmoticon())
                        .themeTitle(userTheme.getTheme().getName())
                        .description(userTheme.getDescription())
                        .openType(userTheme.getOpenType())
                        .createTime(userTheme.getCreateTime())
                        .modifyTime(userTheme.getModifyTime())
                        .build();
                userThemeDtoList.add(userThemeDto);
            }
            else if(userTheme.getOpenType()==1){ // 친구 공개
                boolean flag = userClient.isFollow(userTheme.getUserIdx(),user_idx,userTheme.getIdx());
                if(flag){
                    UserThemeDtoWithMSA userThemeDto = UserThemeDtoWithMSA.builder()
                            .idx(userTheme.getTheme().getIdx())
                            .userThemeIdx(userTheme.getIdx())
                            .userIdx(userTheme.getUserIdx())
                            .themeEmoticon(userTheme.getTheme().getEmoticon())
                            .themeTitle(userTheme.getTheme().getName())
                            .description(userTheme.getDescription())
                            .openType(userTheme.getOpenType())
                            .createTime(userTheme.getCreateTime())
                            .modifyTime(userTheme.getModifyTime())
                            .build();
                    userThemeDtoList.add(userThemeDto);
                }
            }
        }
        return userThemeDtoList;
    }

    @Override
    public List<RecommendDto> getRecommendThemeList() {
        List<RecommendDto> result = new ArrayList<>();

        List<Integer> recommendList = userClient.getRecommendThemeList();
        System.out.println("여기" + recommendList.size());
        for(int i=0;i<recommendList.size();i++) {
            System.out.println(recommendList.get(i));
        }
        for(int i=0;i<recommendList.size();i++) {
            UserTheme userTheme = userThemeRepository.findById(recommendList.get(i)).orElseThrow(IllegalAccessError::new);

            RecommendDto userThemeDto = RecommendDto.builder()
                    .name(userTheme.getTheme().getName())
                    .emoticon(userTheme.getTheme().getEmoticon())
                    .themeIdx(userTheme.getTheme().getIdx())
                    .userIdx(userTheme.getUserIdx())
                    .openType(userTheme.getOpenType())
                    .createTime(userTheme.getCreateTime())
                    .description(userTheme.getDescription())
                    .modifyTime(userTheme.getModifyTime())
                    .challenge(userTheme.isChallenge())
                    .idx(userTheme.getIdx())
                    .build();

            result.add(userThemeDto);

        }

        return result;
    }

    @Override
    public List<UserThemeDto> getUserThemeByUserIdx(int user_idx) {
        List<UserTheme> userThemeList = userThemeRepository.findByUserIdx(user_idx);

        List<UserThemeDto> result = new ArrayList<>();

        for(int i=0;i<userThemeList.size();i++) {
            UserTheme targetUserTheme = userThemeList.get(i);
            UserThemeDto userThemeDto = UserThemeDto.builder()
                    .name(targetUserTheme.getTheme().getName())
                    .emoticon(targetUserTheme.getTheme().getEmoticon())
                    .idx(targetUserTheme.getIdx())
                    .userIdx(targetUserTheme.getUserIdx())
                    .themeIdx(targetUserTheme.getTheme().getIdx())
                    .modifyTime(targetUserTheme.getModifyTime())
                    .createTime(targetUserTheme.getCreateTime())
                    .challenge(targetUserTheme.isChallenge())
                    .openType(targetUserTheme.getOpenType())
                    .description(targetUserTheme.getDescription())
                    .build();

            result.add(userThemeDto);
        }

        return result;
    }

    @Override
    public int getThemeOpenType(int followUserIdx, int followThemeIdx) {
        Optional<UserTheme> userTheme = userThemeRepository.findByIdxAndUserIdx(followThemeIdx,followUserIdx);
        return userTheme.get().getOpenType();
    }
    @Override
    public BoardInfoDto boardInfoByTheme(int themeIdx) {
        BoardInfoDto boardInfoDto = feedClient.boardInfoByTheme(themeIdx);
        return  boardInfoDto;
    }

    @Override
    public ThemeDto getPublicThemeDetail(int theme_idx, int userIdx) {
        Theme theme = themeRepository.findById(theme_idx)
                .orElseThrow(IllegalArgumentException::new);


        ThemeDto themeDto = ThemeDto.builder()
                .createTime(theme.getCreateTime())
                .emoticon(theme.getEmoticon())
                .idx(theme.getIdx())
                .name(theme.getName())
                .bookmarked(scrapRepository.existsByThemeAndUserIdx(theme, userIdx))
                .build();
        return themeDto;
    }

    @Override
    public UserThemeDetailDto getUserThemeDetail(int user_idx, int theme_idx) {

        UserTheme userTheme = userThemeRepository.findById(theme_idx)
                .orElseThrow(IllegalArgumentException::new);

        Theme theme = themeRepository.findById(userTheme.getTheme().getIdx())
                .orElseThrow(IllegalArgumentException::new);

        UserThemeDetailDto userThemeDetailDto = UserThemeDetailDto.builder()
                .emoticon(theme.getEmoticon())
                .themeIdx(theme.getIdx())
                .userThemeIdx(userTheme.getUserIdx())
                .name(theme.getName())
                .build();

        if(userTheme.getUserIdx()==user_idx){
            userThemeDetailDto.setMine(true);
        } else {
            userThemeDetailDto.setFollow(isFollow(user_idx, userTheme.getUserIdx(), theme_idx));
        }

        return userThemeDetailDto;
    }

    @Override
    public int isUserTheme(int userIdx, int themeIdx) {
        Optional<UserTheme> userTheme = userThemeRepository.findByIdx(themeIdx);

        System.out.println(userTheme.toString());
        System.out.println(userTheme.get());
        if(!userTheme.isPresent()){
            UserTheme addUserTheme = UserTheme.builder()
                    .theme(userTheme.get().getTheme())
                    .description(userTheme.get().getTheme().getName())
                    .modifyTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .challenge(false)
                    .userIdx(userIdx)
                    .openType(0) // 공용에서 등록한거니까
                    .build();
            userThemeRepository.save(addUserTheme);
            return addUserTheme.getIdx();
        }
        else return userTheme.get().getIdx(); // userThemeIdx를 넘겨주기
    }

    @Override
    public int whoUserIdx(int userThemeIdx) {
        Optional<UserTheme> userTheme = userThemeRepository.findById(userThemeIdx);
        return userTheme.get().getUserIdx();
    }

    @Override
    public boolean isFollow(int user_idx, int target_user_idx, int theme_idx) {
        return userClient.isFollow(user_idx,target_user_idx,theme_idx);
    }

    @Override
    public String deleteFollowUserTheme(int theme_idx) {
        return userClient.deleteFollowUserTheme(theme_idx);
    }

    @Override
    public boolean isScrap(int userIdx, int themeIdx) {
        Theme theme = themeRepository.findByIdx(themeIdx);
        Optional<Scrap> scrap = scrapRepository.findByThemeAndUserIdx(theme,userIdx);
        return scrap.isPresent();
    }
    @Override
    public List<UserThemeDtoWithMSA> getUserThemeUserList(int userThemeIdx,int user_idx) {
        Optional<UserTheme> userTheme = userThemeRepository.findById(userThemeIdx);
        List<UserThemeDtoWithMSA> userThemeDtoList = new ArrayList<>();
        if ( userTheme.get().getUserIdx() == user_idx) { // 내가 작성한 테마라면
            UserThemeDtoWithMSA userThemeDto = UserThemeDtoWithMSA.builder()
                    .idx(userTheme.get().getTheme().getIdx())
                    .userThemeIdx(userTheme.get().getIdx())
                    .userIdx(userTheme.get().getUserIdx())
                    .themeEmoticon(userTheme.get().getTheme().getEmoticon())
                    .themeTitle(userTheme.get().getTheme().getName())
                    .description(userTheme.get().getDescription())
                    .openType(userTheme.get().getOpenType())
                    .createTime(userTheme.get().getCreateTime())
                    .modifyTime(userTheme.get().getModifyTime())
                    .build();
            userThemeDtoList.add(userThemeDto);
        }
        else if(userTheme.get().getOpenType()==0){
            UserThemeDtoWithMSA userThemeDto = UserThemeDtoWithMSA.builder()
                    .idx(userTheme.get().getTheme().getIdx())
                    .userThemeIdx(userTheme.get().getIdx())
                    .userIdx(userTheme.get().getUserIdx())
                    .themeEmoticon(userTheme.get().getTheme().getEmoticon())
                    .themeTitle(userTheme.get().getTheme().getName())
                    .description(userTheme.get().getDescription())
                    .openType(userTheme.get().getOpenType())
                    .createTime(userTheme.get().getCreateTime())
                    .modifyTime(userTheme.get().getModifyTime())
                    .build();
            userThemeDtoList.add(userThemeDto);
        }
        else if(userTheme.get().getOpenType()==1){ // 친구 공개
            boolean flag = userClient.isFollow(userTheme.get().getUserIdx(),user_idx,userTheme.get().getIdx());
            if(flag){
                UserThemeDtoWithMSA userThemeDto = UserThemeDtoWithMSA.builder()
                        .idx(userTheme.get().getTheme().getIdx())
                        .userThemeIdx(userTheme.get().getIdx())
                        .userIdx(userTheme.get().getUserIdx())
                        .themeEmoticon(userTheme.get().getTheme().getEmoticon())
                        .themeTitle(userTheme.get().getTheme().getName())
                        .description(userTheme.get().getDescription())
                        .openType(userTheme.get().getOpenType())
                        .createTime(userTheme.get().getCreateTime())
                        .modifyTime(userTheme.get().getModifyTime())
                        .build();
                userThemeDtoList.add(userThemeDto);
            }
        }
        return userThemeDtoList;
    }

    @Override
    public String getUserThemeName(int theme_idx) {
        Optional<UserTheme> userTheme = userThemeRepository.findByIdx(theme_idx);
        int publicThemeIdx = userTheme.get().getTheme().getIdx();
        Theme theme = themeRepository.findByIdx(publicThemeIdx);
        return theme.getName();
    }

    @Override
    public int modifyTheme(Integer themeIdx, Integer openType, int userIdx) {
        Optional<UserTheme> userTheme = userThemeRepository.findByIdxAndUserIdx(themeIdx, userIdx);
        if(userTheme.isPresent()){
            userTheme.get().updateOpenType(openType);

            userThemeRepository.save(userTheme.get());
            return 1;
        }
        return -1;
    }

    @Override
    @Transactional
    public int deleteTheme(Integer themeIdx, int userIdx) {
        Optional<UserTheme> userTheme = userThemeRepository.findByIdxAndUserIdx(themeIdx, userIdx);
        if(userTheme.isPresent()){
            feedClient.deleteBoardAndComment(themeIdx); // 게시글, 댓글, 좋아요, 사진 삭제하기
            userClient.deleteFollowUserTheme(themeIdx);  // 팔로우 정보 삭제하기
            userThemeRepository.delete(userTheme.get()); // 유저 테마 삭제하기
            return 1;
        }
        return -1;
    }

}
