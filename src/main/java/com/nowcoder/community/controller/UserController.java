package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import javafx.geometry.Pos;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @RequestMapping(value = "/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "????????????????????????");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "?????????????????????");
            return "/site/setting";
        }

        //???????????????????????????????????????
        filename = CommunityUtil.generateUUID() + suffix;
        //????????????????????????
        File dest = new File(uploadPath + "/" + filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            log.error("??????????????????" + e.getMessage());
            throw new RuntimeException("??????????????????????????????????????????", e);
        }

        //??????????????????
        //localhost:8080/community/user/header/xxx.jpg
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(), headerUrl);
        return "redirect:/index";
    }

    @RequestMapping(value = "/header/{filename}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        // ?????????????????????
        filename = uploadPath + "/" + filename;
        //????????????
        String suffix = filename.substring(filename.lastIndexOf("."));
        //????????????
        response.setContentType("image/" + suffix);
        try(FileInputStream fis = new FileInputStream(filename);
            ServletOutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b=fis.read(buffer))!=-1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("??????????????????" + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oriPassword, String newPassword, Model model){
        User user = hostHolder.getUser();

        oriPassword = CommunityUtil.md5(oriPassword + user.getSalt());

        //???????????????????????????
        if(!user.getPassword().equals(oriPassword)){
            model.addAttribute("PasswordError", "??????????????????");
            return "/site/setting";
        }

        if(newPassword == null) {
            model.addAttribute("newPasswordError", "??????????????????");
            return "/site/setting";
        }


        //???????????????????????????????????????8???
        if(newPassword.length()<8){
            model.addAttribute("newPasswordError", "??????????????????8???");
            return "/site/setting";
        }

        newPassword = CommunityUtil.md5(newPassword + user.getSalt());

        userService.updatePassword(user.getId(), newPassword);
        return "redirect:/index";
    }

    // ????????????
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("??????????????????!");
        }

        // ??????
        model.addAttribute("user", user);
        // ????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //????????????
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        //????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //??????????????????
        boolean hasFollowed = false;
        if(hostHolder.getUser()!= null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }

        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }




}
