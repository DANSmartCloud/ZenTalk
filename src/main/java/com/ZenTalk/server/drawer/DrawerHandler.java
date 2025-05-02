package com.ZenTalk.server.drawer;

import com.ZenTalk.server.util.MessageBundle;

public class DrawerHandler {
    // 搜一搜
    public void search(String keyword) {
        System.out.println(MessageBundle.getMessage("search.keyword", keyword));
    }
    // 日历
    public void showCalendar(String userId) {
        System.out.println(MessageBundle.getMessage("calendar.show", userId));
    }
}