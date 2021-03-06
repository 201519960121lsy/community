package com.springboot.community.controller;

import com.springboot.community.dto.AccessTokenDTO;
import com.springboot.community.dto.GithubUser;
import com.springboot.community.mapper.UserMapper;
import com.springboot.community.model.User;
import com.springboot.community.provider.GithubProvider;
import com.springboot.community.service.UserService;
import jdk.nashorn.internal.parser.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @Classname authorizeController
 * @Description TODO
 * @Date 2019/10/12 11:33
 * @Created by 猪刚鬣·李
 */
//用户登录控制层
@Controller
@Slf4j
public class AuthorizeController {
    /*自动识别spring容器*/
    @Autowired
    private GithubProvider githubProvider;
    /*@value 读取配置文件的信息*/
    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Autowired
    private UserService userService;

    /*登录*/
    @GetMapping("/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientSecret);
        accessTokenDTO.setRedirect_uri(redirectUri);
        accessTokenDTO.setCode(code);
        accessTokenDTO.setState(state);
        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        GithubUser githubUser = githubProvider.getUser(accessToken);
        /*判断githubUser是否为空*/
        if ((githubUser != null) && (githubUser.getId() != null)) {
            User user = new User();
            //获取用户信息，随机生成一个token
            //放到user对象存进数据库
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            user.setName(githubUser.getName());
            user.setAccountId(String.valueOf(githubUser.getId()));
            user.setAvatarUrl(githubUser.getAvatarUrl());
            /*user.setGmtCreate(System.currentTimeMillis());
            user.setGmtModified(user.getGmtCreate());*/
            userService.createOrUpdate(user);
            /*userMapper.insert(user);*/
//            System.out.println(user);
            //user不为空，登录成功,写cookie和session
            //自动写入cookie
            response.addCookie(new Cookie("token", token));
            //手动写入cookie
//            request.getSession().setAttribute("githubUser",githubUser);
//            System.out.println(githubUser);
            return "redirect:/";
        } else {
            //登陆失败，重新登录
            log.error("callback get github error {}", githubUser);
            return "redirect:/";
        }
    }

    /*退出登录*/
    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        /*清除session*/
        request.getSession().removeAttribute("user");
        /*清除cookie*/
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}
